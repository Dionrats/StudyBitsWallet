package nl.quintor.studybits.studybitswallet.credential;

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
import nl.quintor.studybits.studybitswallet.room.AppDatabase;
import nl.quintor.studybits.studybitswallet.room.entity.University;


public class CredentialOfferViewModel extends AndroidViewModel {
    private static final String TRANSCRIPT_SCHEMA = "Transcript";

    private final MutableLiveData<List<CredentialOrOffer>> credentialOffers = new MutableLiveData<>();
    private final MutableLiveData<List<CredentialInfo>> credentials = new MutableLiveData<>();

    public CredentialOfferViewModel(@NonNull Application application) {
        super(application);
    }

    public void initCredentials() {
        IndyClient indyClient = new IndyClient(IndyConnection.getInstance(), AppDatabase.getInstance(getApplication().getApplicationContext()));

        try {
            credentials.setValue(indyClient.findCredentials(credentialInfo -> credentialInfo.getSchemaId().contains(TRANSCRIPT_SCHEMA)));
        } catch (InterruptedException | ExecutionException | IndyException e) {
            Log.e("STUDYBITS", "Error while refreshing credentials");
            e.printStackTrace();
        }
    }

    public void initCredentialOffers(List<University> universities) {
        if (universities == null) {
            return;
        }
        Log.d("STUDYBITS", "Initializing credential offers");
        try {
            List<CredentialOrOffer> credentialOrOffers = new ArrayList<>();
            for (University university : universities) {
                Log.d("STUDYBITS", "Initializing credential offers for university " + university);
                List<CredentialOrOffer> credentialOrOffersForUni = getCredentialOrOffers(university);

                credentialOrOffers.addAll(credentialOrOffersForUni);
            }

            credentialOffers.setValue(credentialOrOffers);
        }
        catch (Exception e) {
            Log.e("STUDYBITS", "Exception while getting credential offers" + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private List<CredentialOrOffer> getCredentialOrOffers(University university) throws IOException, InterruptedException, ExecutionException, IndyException {

        List<CredentialOffer> offersForUni = new AgentClient(university, IndyConnection.getInstance()).getCredentialOffers();

        Log.d("STUDYBITS", "Got " + offersForUni.size() + " message envelopes with offers");

        return offersForUni.stream()
                .map(credentialOffer -> CredentialOrOffer.fromCredentialOffer(university, credentialOffer))
                .collect(Collectors.toList());
    }

    public LiveData<List<CredentialOrOffer>> getCredentialOffers() {
        return credentialOffers;
    }

    public LiveData<List<CredentialInfo>> getCredentials() {
        return credentials;
    }
}
