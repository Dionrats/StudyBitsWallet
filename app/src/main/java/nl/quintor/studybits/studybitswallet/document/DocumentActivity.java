package nl.quintor.studybits.studybitswallet.document;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import nl.quintor.studybits.studybitswallet.R;
import nl.quintor.studybits.studybitswallet.credential.CredentialOrOffer;

public class DocumentActivity extends AppCompatActivity  implements DocumentFragment.OnListFragmentInteractionListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onListFragmentInteraction(CredentialOrOffer credentialOrOffer) {}
}
