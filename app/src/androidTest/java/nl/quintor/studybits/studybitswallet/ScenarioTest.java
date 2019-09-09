package nl.quintor.studybits.studybitswallet;

import android.Manifest;
import android.content.Intent;
import android.os.Environment;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import nl.quintor.studybits.studybitswallet.document.DocumentActivity;
import nl.quintor.studybits.studybitswallet.university.UniversityActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */

@LargeTest
public class ScenarioTest {

    @Rule
    public ActivityTestRule<MainActivity> mainActivityRule = new ActivityTestRule<>(MainActivity.class);

    @Rule
    public ActivityTestRule<UniversityActivity> universityActivityRule = new ActivityTestRule<>(UniversityActivity.class, true, false);

    @Rule
    public ActivityTestRule<DocumentActivity> documentActivityRule = new ActivityTestRule<>(DocumentActivity.class, true, false);

    @Rule
    public GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.INTERNET);

    @Before
    public void setTimeouts() {
        Log.d("STUDYBITS", "Setting timeouts to 3 minutes");

        IdlingPolicies.setMasterPolicyTimeout(180, TimeUnit.SECONDS);
        IdlingPolicies.setIdlingResourceTimeout(180, TimeUnit.SECONDS);
    }

    @Test
    public void fullScenarioTest() {
        Log.d("STUDYBITS", "Starting test");
        // Reset
        onView(withId(R.id.fab))
                .perform(click());
        Log.d("STUDYBITS", "Clicked reset");

        onView(allOf(withId(android.support.design.R.id.snackbar_text), withText("Successfully reset")))
                .check(matches(isDisplayed()));

        Log.d("STUDYBITS", "Successfully reset");




        // Launch connection dialog
        Intent intent = new Intent();

        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(new TestConfiguration().getRuGConnectionURI());

        universityActivityRule.launchActivity(intent);

        // Enter studentID
        onView(withId(R.id.student_id_text))
                .check(matches(isDisplayed()))
                .perform(typeText("12345678"));

        // Enter password
        onView(withId(R.id.password_text))
                .check(matches(isDisplayed()))
                .perform(typeText("test1234"));

        // Click connect
        onView(withText(R.string.connect))
                .perform(click());

        // Check connection to university
        onView(withText("Rijksuniversiteit Groningen"))
                .check(matches(isDisplayed()));

        // Navigate to credentials via pressing back on toolbar
        onView(withContentDescription(R.string.abc_action_bar_up_description)).perform(click());

        // Simulate file publish
        File file = null;
        try {
            file = File.createTempFile("temp", "tmp");
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write("Hello World");
            fileWriter.close();

            HttpClient httpclient = new DefaultHttpClient();
            // build multipart upload request
            HttpEntity data = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .addBinaryBody("file", file, ContentType.MULTIPART_FORM_DATA, file.getName())
                    .addTextBody("name", file.getName(), ContentType.MULTIPART_FORM_DATA)
                    .addTextBody("type", "tst", ContentType.MULTIPART_FORM_DATA)
                    .addTextBody("student", "12345678", ContentType.MULTIPART_FORM_DATA)
                    .build();

            // build http request and assign multipart upload data
            HttpUriRequest request = RequestBuilder
                    .post(new TestConfiguration().getRuGEndpoint() + "/documents/upload")
                    .setEntity(data)
                    .build();

            httpclient.execute(request);

        } catch (IOException e) {
            e.printStackTrace();
        }

        //navigate to document activity
        onView(withId(R.id.button_document))
                .perform(click());

        // Check presence of CredentialOffer
        onView(withText("Rijksuniversiteit Groningen"))
                .check(matches(isDisplayed()));

        // Accept CredentialOffer
        onView(withText("Rijksuniversiteit Groningen"))
                .perform(click());

        // Open document menu
        onView(withId(R.id.file_download))
                .perform(click());

        // Download document
        onView(withText(R.string.savefile))
                .perform(click());



        // Check Document
        File doc = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file.getName() + ".tst");
        assertTrue(doc.exists() && doc.isFile());

        // Open document menu
        onView(withId(R.id.file_download))
                .perform(click());

        // Download Verification-document
        onView(withText(R.string.getvalidation))
                .perform(click());

        // check Verification-document
        File sbv = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file.getName() + ".tst.sbv");
        assertTrue(sbv.exists() && sbv.isFile());

        // Verfify document integrity
        try {
            HttpClient httpclient = new DefaultHttpClient();
            // build multipart upload request
            HttpEntity data = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .addBinaryBody("file", doc, ContentType.MULTIPART_FORM_DATA, doc.getName() + ".tst")
                    .addBinaryBody("validation", sbv, ContentType.MULTIPART_FORM_DATA, sbv.getName() + ".tst.sbv")
                    .build();

            // build http request and assign multipart upload data
            HttpUriRequest request = RequestBuilder
                    .post(new TestConfiguration().getGentEndpoint() + "/documents/verify")
                    .setEntity(data)
                    .build();

            String result = EntityUtils.toString(httpclient.execute(request).getEntity());
            assertTrue(new JSONObject(result).getBoolean("response"));

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        // Delete Document & Verification-document
        doc.delete();
        sbv.delete();
    }
}
