package nl.quintor.studybits.studybitswallet;

import android.net.Uri;

public class TestConfiguration implements IndyConfiguration{

    @Override
    public String getStudentDiD() {
        return "Xepuw1Y1k9DpvoSvZaoVJr";
    }

    @Override
    public String getStudentSecretName() {
        return "student_secret_name";
    }

    @Override
    public String getStudentSeed() {
        return "000000000000000000000000Student1";
    }

    @Override
    public String getPoolName() {
        return "testPool4";
    }

    @Override
    public String getWalletName() {
        return "student_wallet4";
    }

    @Override
    public String getEndpointIP() {
        return BuildConfig.ENDPOINT_IP;
    }

    @Override
    public String getGentEndpoint() {
        return "http://" + this.getEndpointIP() + ":8081";
    }

    @Override
    public String getRuGEndpoint() {
        return "http://" + this.getEndpointIP() + ":8080";
    }

    @Override
    public Uri getRuGConnectionURI() {
        return Uri.parse("ssi://studybits?university=Rijksuniversiteit%20Groningen&did=SYqJSzcfsJMhSt7qjcQ8CC&endpoint=" + Uri.encode(this.getRuGEndpoint()));
    }

    @Override
    public Uri getGentConnectionURI() {
        return Uri.parse("ssi://studybits?university=Universiteit%20Gent&did=Vumgc4B8hFq7n5VNAnfDAL&endpoint=" + Uri.encode(this.getGentEndpoint()));
    }

}