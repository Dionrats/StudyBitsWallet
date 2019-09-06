package nl.quintor.studybits.studybitswallet.university;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import java.util.ArrayList;

import nl.quintor.studybits.indy.wrapper.message.IndyMessageTypes;
import nl.quintor.studybits.studybitswallet.IndyClient;
import nl.quintor.studybits.studybitswallet.IndyConnection;
import nl.quintor.studybits.studybitswallet.R;
import nl.quintor.studybits.studybitswallet.messages.StudyBitsMessageTypes;
import nl.quintor.studybits.studybitswallet.room.AppDatabase;
import nl.quintor.studybits.studybitswallet.room.entity.University;

public class UniversityActivity extends AppCompatActivity {
    private UniversityRecyclerViewAdapter universityAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        IndyMessageTypes.init();
        StudyBitsMessageTypes.init();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_university);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final UniversityActivity activity = this;

        Intent intent = getIntent();


        // login modal
        if (intent != null && intent.getData() != null) {
            Uri data = intent.getData();

            String name = data.getQueryParameter("university");
            String did = data.getQueryParameter("did");
            String endpoint = data.getQueryParameter("endpoint");

            ConnectUniversityDialogFragment dialogFragment = new ConnectUniversityDialogFragment();
            Bundle arguments = new Bundle();
            arguments.putString("name", name);
            dialogFragment.setArguments(arguments);


            dialogFragment.setConnectDialogListener(() -> {
                String username = dialogFragment.getUsernameText();
                String password = dialogFragment.getPasswordText();
                Log.d("STUDYBITS", "Logging in with endpoint " + endpoint + " and username " + username);

                IndyClient indyClient = new IndyClient(IndyConnection.getInstance(), AppDatabase.getInstance(getApplicationContext()));

                try {
                    University university = indyClient.connect(endpoint, name, username, password, did);

                    Snackbar.make(activity.getWindow().getDecorView(), "Connected to " + university.getName() + "!", Snackbar.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e("STUDYBITS", "Exception on accepting connection request" + e.getMessage());

                }
            });
            dialogFragment.show(getSupportFragmentManager(), "connect");
        }



        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        // display connected universities?
        RecyclerView universityRecyclerView = findViewById(R.id.university_recycler_view);

        RecyclerView.LayoutManager universityLayoutManager = new LinearLayoutManager(this);
        universityRecyclerView.setLayoutManager(universityLayoutManager);

        universityAdapter = new UniversityRecyclerViewAdapter(this, new ArrayList<>());

        universityRecyclerView.setAdapter(universityAdapter);

        UniversityListViewModel universityListViewModel = ViewModelProviders.of(this).get(UniversityListViewModel.class);

        universityListViewModel.getUniversityList().observe(UniversityActivity.this, universities -> universityAdapter.setData(universities));


    }

}
