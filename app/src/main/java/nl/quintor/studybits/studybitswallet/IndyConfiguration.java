package nl.quintor.studybits.studybitswallet;

import android.net.Uri;

public interface IndyConfiguration {

    String getStudentDiD();
    String getStudentSecretName();
    String getStudentSeed();
    String getPoolName();
    String getWalletName();
    String getEndpointIP();
    String getGentEndpoint();
    String getRuGEndpoint();
    Uri getRuGConnectionURI();
    Uri getGentConnectionURI();

}
