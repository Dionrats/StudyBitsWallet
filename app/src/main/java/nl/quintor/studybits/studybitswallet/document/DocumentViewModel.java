package nl.quintor.studybits.studybitswallet.document;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.util.Log;

import org.hyperledger.indy.sdk.IndyException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import nl.quintor.studybits.indy.wrapper.IndyWallet;
import nl.quintor.studybits.indy.wrapper.Prover;
import nl.quintor.studybits.indy.wrapper.dto.CredentialInfo;
import nl.quintor.studybits.indy.wrapper.dto.CredentialOffer;
import nl.quintor.studybits.indy.wrapper.message.MessageEnvelopeCodec;
import nl.quintor.studybits.studybitswallet.AgentClient;
import nl.quintor.studybits.studybitswallet.credential.CredentialOrOffer;
import nl.quintor.studybits.studybitswallet.room.entity.University;

import static nl.quintor.studybits.studybitswallet.TestConfiguration.STUDENT_SECRET_NAME;

public class DocumentViewModel extends AndroidViewModel {

    public static final String DOCUMENT_SCHEMA = "Document";
    private final MutableLiveData<List<CredentialOrOffer>> documentOffers = new MutableLiveData<>();
    private final MutableLiveData<List<CredentialInfo>> documents = new MutableLiveData<>();

    public DocumentViewModel(@NonNull Application application) {
        super(application);
    }

    public void initDocuments(IndyWallet indyWallet) {
        Prover prover = new Prover(indyWallet, STUDENT_SECRET_NAME);

        try {
            documents.setValue(prover.findAllCredentials().get().stream().filter(cred -> cred.getSchemaId().contains(DOCUMENT_SCHEMA)).collect(Collectors.toList()));
        } catch (InterruptedException | ExecutionException | IndyException e) {
            Log.e("STUDYBITS", "Error while refreshing credentials");
            e.printStackTrace();
        }
    }

    public void initDocumentOffers(List<University> universities, MessageEnvelopeCodec codec) {
        if (universities == null) {
            return;
        }
        Log.d("STUDYBITS", "Initializing document offers");
        try {
            List<CredentialOrOffer> documents = new ArrayList<>();
            for (University university : universities) {
                Log.d("STUDYBITS", "Initializing document offers for university " + university);
                List<CredentialOrOffer> documentsForUni = getDocumentOffers(codec, university);
                documents.addAll(documentsForUni);
            }

            documentOffers.setValue(documents);
        }
        catch (Exception e) {
            Log.e("STUDYBITS", "Exception while getting document offers" + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private List<CredentialOrOffer> getDocumentOffers(MessageEnvelopeCodec codec, University university) throws IOException, InterruptedException, ExecutionException, IndyException {

        List<CredentialOffer> documentsForUni = new AgentClient(university, codec).getDocumentOffers();

        Log.d("STUDYBITS", "Got " + documentsForUni.size() + " message envelopes with offers");

        return documentsForUni.stream()
                .map(offer -> CredentialOrOffer.fromCredentialOffer(university, offer))
                .collect(Collectors.toList());
    }

    public LiveData<List<CredentialOrOffer>> getDocumentOffers() {
        return documentOffers;
    }

    public LiveData<List<CredentialInfo>> getDocuments() {
        return documents;
    }
}

