package nl.quintor.studybits.studybitswallet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.security.NetworkSecurityPolicy;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.sun.jna.Native;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.hyperledger.indy.sdk.LibIndy;
import org.hyperledger.indy.sdk.pool.Pool;

import java.io.File;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import nl.quintor.studybits.indy.wrapper.IndyPool;
import nl.quintor.studybits.indy.wrapper.IndyWallet;
import nl.quintor.studybits.indy.wrapper.Prover;
import nl.quintor.studybits.indy.wrapper.message.IndyMessageTypes;
import nl.quintor.studybits.indy.wrapper.util.PoolUtils;
import nl.quintor.studybits.studybitswallet.credential.CredentialActivity;
import nl.quintor.studybits.studybitswallet.document.DocumentActivity;
import nl.quintor.studybits.studybitswallet.exchangeposition.ExchangePositionActivity;
import nl.quintor.studybits.studybitswallet.messages.StudyBitsMessageTypes;
import nl.quintor.studybits.studybitswallet.room.AppDatabase;
import nl.quintor.studybits.studybitswallet.university.UniversityActivity;


public class MainActivity extends AppCompatActivity {

    static {
        Log.d("STUDYBITS", "ENDPOINT IP: " +  new TestConfiguration().getEndpointIP());
        Log.d("STUDYBITS", "Attempting to load indy");
        System.loadLibrary("indy");
        Log.d("STUDYBITS", "Loaded indy");
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
        Log.d("STUDYBITS", "Network allowed" + NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted());

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);

        Log.d("STUDYBITS", "Creating indyConnection.");
        IndyConnection.setConfiguration(new TestConfiguration());
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 112;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ImageButton universityButton = (ImageButton) findViewById(R.id.button_university);

        universityButton.setOnClickListener((view) -> {
            Intent intent = new Intent(this, UniversityActivity.class);
            startActivity(intent);
        });

        ImageButton credentialButton = (ImageButton) findViewById(R.id.button_credential);

        credentialButton.setOnClickListener((view) -> {
            Intent intent = new Intent(this, CredentialActivity.class);
            startActivity(intent);
        });

        ImageButton exchangePositionButton = (ImageButton) findViewById(R.id.button_exchange_position);

        exchangePositionButton.setOnClickListener((view) -> {
            Intent intent = new Intent(this, ExchangePositionActivity.class);
            startActivity(intent);
        });

        ImageButton documentsButton = (ImageButton) findViewById(R.id.button_document);

        documentsButton.setOnClickListener((view) -> {
            Intent intent = new Intent(this, DocumentActivity.class);
            startActivity(intent);
        });


        try {
            Pool.setProtocolVersion(PoolUtils.PROTOCOL_VERSION).get();
        } catch (Exception e) {
            Log.e("STUDYBITS", "Exception during create" + e.getMessage());
            e.printStackTrace();
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this::resetWallet);

    }

    private void resetWallet(View view) {
        try {
            int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                        this,
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                );
            }

            File indyClientDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/.indy_client/");
            boolean indyFolderExists = true;
            if (!indyClientDir.exists()) {
                indyFolderExists = indyClientDir.mkdir();
            }
            if (indyFolderExists) {
                Log.d("STUDYBITS", indyClientDir.toString());
                for (File file : FileUtils.listFilesAndDirs(indyClientDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)) {
                    Log.d("STUDYBITS", "File " + file);
                }
                try {
                    FileUtils.deleteDirectory(indyClientDir);
                } catch (IOException e) {
                    throw new IOException(e);
                }
            }
            for (String abi : Build.SUPPORTED_ABIS) {
                Log.d("STUDYBITS", "Supported ABI: " + abi);
            }

            Log.d("STUDYBITS", "Loading other indy");
            LibIndy.API api = (LibIndy.API) Native.loadLibrary("indy", LibIndy.API.class);
            Log.d("STUDYBITS", "Indy api object: " + api);

            String poolName = PoolUtils.createPoolLedgerConfig(new TestConfiguration().getEndpointIP(), new TestConfiguration().getPoolName());

            IndyPool indyPool = new IndyPool(poolName);
            IndyWallet tempWallet = IndyWallet.create(indyPool, new TestConfiguration().getWalletName(), new TestConfiguration().getStudentSeed());
            Log.d("STUDYBITS", "DiD: " + tempWallet.getMainDid());
            Prover prover = new Prover(tempWallet, new TestConfiguration().getStudentSecretName());
            prover.init();
            tempWallet.close();
            Log.d("STUDYBITS", "Closing tempWallet");
            indyPool.close();

            URL url = new URL(new TestConfiguration().getGentEndpoint() + "/bootstrap/reset");

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(false);
            urlConnection.setDoInput(true);
            Log.d("STUDYBITS", "Response code: " + urlConnection.getResponseCode());

            url = new URL(new TestConfiguration().getRuGEndpoint() + "/bootstrap/reset");

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(false);
            urlConnection.setDoInput(true);

            Log.d("STUDYBITS", "Response code: " + urlConnection.getResponseCode());

            AtomicInteger countDownLatch = new AtomicInteger(1);

            AppDatabase.AsyncDatabaseTask databaseClean = new AppDatabase.AsyncDatabaseTask(
                    () -> AppDatabase.getInstance(this).universityDao().delete(),
                    countDownLatch,
                    () -> Snackbar.make(view, "Successfully reset", Snackbar.LENGTH_SHORT).show());
            databaseClean.execute();

        } catch (Exception e) {
            Log.e("STUDYBITS", "Exception during reset" + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
