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
import nl.quintor.studybits.indy.wrapper.dto.CredentialInfo;
import nl.quintor.studybits.indy.wrapper.dto.CredentialOffer;
import nl.quintor.studybits.studybitswallet.AgentClient;
import nl.quintor.studybits.studybitswallet.IndyClient;
import nl.quintor.studybits.studybitswallet.IndyConnection;
import nl.quintor.studybits.studybitswallet.credential.CredentialOrOffer;
import nl.quintor.studybits.studybitswallet.room.AppDatabase;
import nl.quintor.studybits.studybitswallet.room.entity.University;


public class DocumentViewModel extends AndroidViewModel {
    public static final String DOCUMENT_SCHEMA = "Document";

    private final MutableLiveData<List<CredentialOrOffer>> documentOffers = new MutableLiveData<>();
    private final MutableLiveData<List<CredentialInfo>> documents = new MutableLiveData<>();

    public DocumentViewModel(@NonNull Application application) {
        super(application);
    }

    public void initDocuments() {
        IndyClient indyClient = new IndyClient(IndyConnection.getInstance(), AppDatabase.getInstance(getApplication().getApplicationContext()));
        try {
            documents.setValue(indyClient.findCredentials(credentialInfo -> credentialInfo.getSchemaId().contains(DOCUMENT_SCHEMA)));
        } catch (InterruptedException | ExecutionException | IndyException e) {
            Log.e("STUDYBITS", "Exception while refreshing documents: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void initDocumentOffers(List<University> universities) {
        if (universities == null) {
            return;
        }
        Log.d("STUDYBITS", "Initializing document offers");
        try {
            List<CredentialOrOffer> documents = new ArrayList<>();
            for (University university : universities) {
                Log.d("STUDYBITS", "Initializing document offers for university " + university);
                List<CredentialOrOffer> documentsForUni = getDocumentOffers(university);
                documents.addAll(documentsForUni);
            }

            documentOffers.setValue(documents);
        }
        catch (Exception e) {
            Log.e("STUDYBITS", "Exception while getting documentoffers " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private List<CredentialOrOffer> getDocumentOffers(University university) throws IOException, InterruptedException, ExecutionException, IndyException {

        List<CredentialOffer> documentsForUni = new AgentClient(university, IndyConnection.getInstance()).getDocumentOffers();

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

