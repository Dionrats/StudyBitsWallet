package nl.quintor.studybits.studybitswallet;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import org.hyperledger.indy.sdk.IndyException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import io.ipfs.api.IPFS;
import io.ipfs.multihash.Multihash;
import nl.quintor.studybits.indy.wrapper.util.AsyncUtil;
import nl.quintor.studybits.studybitswallet.document.Document;


public class IPFSClient extends AsyncTask<Document, Integer, File> {
    private IPFS ipfs;
    private IndyConnection connection;
    private Consumer<Integer> progressCallback;

    public IPFSClient(IndyConnection connection, Consumer<Integer> progressCallback) {
        this.ipfs = new IPFS(connection.getConfiguration().getEndpointIP(), 5001);
        this.connection = connection;
        this.progressCallback = progressCallback;
    }

    public CompletableFuture<File> retrieveFile(Document document) {
        return CompletableFuture.supplyAsync(AsyncUtil.wrapException(() -> execute(document).get()));
    }

    @Override
    protected File doInBackground(Document... documents) {
        Document document = documents[0];
        File target = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), document.getName());

        try(
            FileOutputStream fileOutputStream = new FileOutputStream(target);
            InputStream input = ipfs.catStream(Multihash.fromBase58(document.getHash()))
        ) {
            int fileSize = Integer.valueOf(document.getSize());
            int total = 0;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];

            int r;
            while((r = input.read(buf)) >= 0) {
                if (isCancelled()) {
                    input.close();
                    return null;
                }
                total += r;
                output.write(buf, 0, r);
                publishProgress(((total / fileSize) * 100));
            }
            byte[] encryptedData = output.toByteArray();
            publishProgress(101);
            Holder h = connection.getWallet().authDecrypt(encryptedData, document.getIssuer().getTheirDid(), Holder.class).get();
            byte[] decryptedData = h.getData();
            fileOutputStream.write(decryptedData);

        } catch (IOException | ExecutionException | IndyException e) {
            Log.e("STUDYBITS", e.getMessage());
        } catch (InterruptedException e) {
            Log.e("STUDYBITS", e.getMessage());
            Thread.currentThread().interrupt();
        }
        publishProgress(102);
        return target;

    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        progressCallback.accept(values[0]);
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
