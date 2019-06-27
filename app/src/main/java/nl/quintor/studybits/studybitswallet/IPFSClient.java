package nl.quintor.studybits.studybitswallet;

import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.util.Log;

import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.crypto.Crypto;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import io.ipfs.api.IPFS;
import io.ipfs.multihash.Multihash;
import nl.quintor.studybits.indy.wrapper.IndyWallet;

import static nl.quintor.studybits.studybitswallet.TestConfiguration.ENDPOINT_IP;

public class IPFSClient {
    private IPFS ipfs;
    private IndyWallet studentWallet;

    public IPFSClient(IndyWallet studentWallet) {
        this.ipfs = new IPFS(ENDPOINT_IP, 5001);
        this.studentWallet = studentWallet;
    }

    public File retrieveFile(String fileName, String hash, String issuerDid) throws IOException, IndyException, ExecutionException, InterruptedException, JSONException {
        Multihash filePointer = Multihash.fromBase58(hash);
        File target = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
        byte[] encryptedData = ipfs.cat(filePointer);
        Holder h = studentWallet.authDecrypt(encryptedData, issuerDid, Holder.class).get();
        byte[] decryptedData = h.getData();
        try(FileOutputStream fileOutputStream = new FileOutputStream(target)) {
            fileOutputStream.write(decryptedData);
        } catch (IOException e) {
            Log.e("STUDYBITS", e.getMessage());
        }

        return target;
    }


}

class Holder {
    private byte[] data;

    public Holder() { }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
