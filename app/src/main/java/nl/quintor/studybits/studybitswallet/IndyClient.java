package nl.quintor.studybits.studybitswallet;

import android.annotation.SuppressLint;
import android.arch.lifecycle.LifecycleOwner;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.crypto.Crypto;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.ipfs.multihash.Multihash;
import nl.quintor.studybits.indy.wrapper.Prover;
import nl.quintor.studybits.indy.wrapper.dto.AttributeInfo;
import nl.quintor.studybits.indy.wrapper.dto.ConnectionRequest;
import nl.quintor.studybits.indy.wrapper.dto.ConnectionResponse;
import nl.quintor.studybits.indy.wrapper.dto.Credential;
import nl.quintor.studybits.indy.wrapper.dto.CredentialInfo;
import nl.quintor.studybits.indy.wrapper.dto.CredentialRequest;
import nl.quintor.studybits.indy.wrapper.dto.CredentialWithRequest;
import nl.quintor.studybits.indy.wrapper.dto.Filter;
import nl.quintor.studybits.indy.wrapper.dto.Proof;
import nl.quintor.studybits.indy.wrapper.dto.ProofRequest;
import nl.quintor.studybits.indy.wrapper.message.IndyMessageTypes;
import nl.quintor.studybits.indy.wrapper.message.MessageEnvelope;
import nl.quintor.studybits.studybitswallet.credential.CredentialOrOffer;
import nl.quintor.studybits.studybitswallet.document.Document;
import nl.quintor.studybits.studybitswallet.exchangeposition.ExchangePosition;
import nl.quintor.studybits.studybitswallet.room.AppDatabase;
import nl.quintor.studybits.studybitswallet.room.entity.University;

public class IndyClient {


    private final IndyConnection connection;
    private final AppDatabase appDatabase;

    public IndyClient(IndyConnection connection, AppDatabase appDatabase) {
        this.connection = connection;
        this.appDatabase = appDatabase;
    }

    public void acceptCredentialOffer(LifecycleOwner lifecycleOwner, CredentialOrOffer credentialOrOffer, CompletableFuture<Void> returnValue) {
        try {
            Log.d("STUDYBITS", "Accepting credential offer");
            Prover studentProver = new Prover(connection.getWallet(), connection.getConfiguration().getStudentSecretName());

            CredentialRequest credentialRequest = studentProver.createCredentialRequest(credentialOrOffer.getTheirDid(), credentialOrOffer.getCredentialOffer()).get();
            MessageEnvelope credentialRequestEnvelope = connection.getCodec().encryptMessage(credentialRequest, IndyMessageTypes.CREDENTIAL_REQUEST, credentialOrOffer.getTheirDid()).get();

            University university = credentialOrOffer.getUniversity();

            if (returnValue.isDone()) {
                return;
            }
            try {
                MessageEnvelope<CredentialWithRequest> credentialEnvelope = new AgentClient(university, connection).postAndReturnMessage(credentialRequestEnvelope, IndyMessageTypes.CREDENTIAL);

                CredentialWithRequest credentialWithRequest = connection.getCodec().decryptMessage(credentialEnvelope).get();

                studentProver.storeCredential(credentialWithRequest).get();

                Credential credential = credentialWithRequest.getCredential();

                university.setCredDefId(credential.getCredDefId());

                new AppDatabase.AsyncDatabaseTask(() -> appDatabase.universityDao().insertUniversities(university),
                        new AtomicInteger(1), () -> {
                    Log.d("STUDYBITS", "Accepted credential offer");
                    returnValue.complete(null);
                }).execute();


            }
            catch (Exception e) {
                Log.e("STUDYBITS", "Error while accepting credential offer " + e.getMessage());
                returnValue.completeExceptionally(e);
            }
        }
        catch (Exception e) {
            Log.e("STUDYBITS", "Exception when accepting credential offer" + e.getMessage());
            e.printStackTrace();
            returnValue.completeExceptionally(e);
        }
    }

    public MessageEnvelope fulfillExchangePosition(ExchangePosition exchangePosition) throws IndyException, IOException, ExecutionException, InterruptedException {
        ProofRequest proofRequest = exchangePosition.getProofRequest();

        Prover prover = new Prover(connection.getWallet(), connection.getConfiguration().getStudentSecretName());
        Map<String, String> values = new HashMap<>();

        Proof proof = prover.fulfillProofRequest(proofRequest, values).get();

        return connection.getCodec().encryptMessage(proof, IndyMessageTypes.PROOF, exchangePosition.getTheirDid()).get();
    }

    private ProofRequest composeDocumentProofRequest(Document document) {
        List<Filter> documentFilter = Collections.singletonList(new Filter(document.getIssuer().getCredDefId()));
        return ProofRequest.builder()
                .name("Document")
                .nonce("45780293854785932345")
                .version("1.0")
                .requestedAttribute("attr1_referent", new AttributeInfo("hash", Optional.of(documentFilter)))
               // .requestedPredicate("predicate1_referent", new PredicateInfo("hash", "=", 0, Optional.of(documentFilter)))
                .build();
    }

    public JSONObject composeDocumentVerification(Document document, File file) throws ExecutionException, InterruptedException {
        @SuppressLint("StaticFieldLeak") AsyncTask<Document, Integer, JSONObject> task = new AsyncTask<Document, Integer, JSONObject>() {
            @Override
            protected JSONObject doInBackground(Document... documents) {
            JSONObject jsonObject = new JSONObject();
            try {
                ProofRequest proofRequest = composeDocumentProofRequest(documents[0]);
                Proof proof = proofDocument(proofRequest);

                jsonObject.put("p", proof.toJSON());
                jsonObject.put("r", proofRequest.toJSON());
                jsonObject.put("h", new Multihash(Multihash.Type.sha2_256, DigestUtils.sha256(IOUtils.toByteArray(file.toURI()))));
                jsonObject.put("k", connection.getWallet().getMainKey());

                byte[] signature = signMessage(jsonObject.toString().getBytes());

                jsonObject.put("s", new JSONArray(signature));
            } catch (IndyException | ExecutionException | JSONException | IOException e) {
                Log.e("StudyBits", e.getMessage());
            } catch (InterruptedException e) {
                Log.e("StudyBits", e.getMessage());
                Thread.currentThread().interrupt();
            }

                return jsonObject;
            }
        };

        return task.execute(document).get();
    }

    private Proof proofDocument(ProofRequest proofRequest) throws JsonProcessingException, IndyException, ExecutionException, InterruptedException {
        Prover prover = new Prover(connection.getWallet(), connection.getConfiguration().getStudentSecretName());
        Map<String, String> values = new HashMap<>();

        return prover.fulfillProofRequest(proofRequest, values).get();
    }

    public byte[] signMessage(byte[] message) throws IndyException, ExecutionException, InterruptedException {
        return Crypto.cryptoSign(connection.getWallet().getWallet(), connection.getWallet().getMainKey(), message).get();
    }

    @NonNull
    public University connect(String endpoint, String uniName, String username, String password, String uniVerinymDid) throws Exception {
        ConnectionRequest connectionRequest = connection.getWallet().createConnectionRequest().get();

        MessageEnvelope<ConnectionRequest> connectionResponseEnvelope = connection.getCodec().encryptMessage(connectionRequest, IndyMessageTypes.CONNECTION_REQUEST, uniVerinymDid).get();

        MessageEnvelope<ConnectionResponse> connectionResponseMessageEnvelope = AgentClient.login(endpoint, username, password, connectionResponseEnvelope);

        ConnectionResponse connectionResponse = connection.getCodec().decryptMessage(connectionResponseMessageEnvelope).get();

        connection.getWallet().acceptConnectionResponse(connectionResponse, connectionRequest.getDid());

        University university = new University(uniName, endpoint, connectionResponse.getDid());

        Log.d("STUDYBITS", "Inserting university: " + university);
        new AppDatabase.AsyncDatabaseTask(() -> appDatabase.universityDao().insertUniversities(university), null, null).execute();
        return university;
    }

    public List<CredentialInfo> findCredentials(Predicate<? super CredentialInfo> filter) throws IndyException, ExecutionException, InterruptedException {
        Prover prover = new Prover(connection.getWallet(), connection.getConfiguration().getStudentSecretName());

        return prover.findAllCredentials().get().stream().filter(filter).collect(Collectors.toList());

    }

}
