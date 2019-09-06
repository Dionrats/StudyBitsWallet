package nl.quintor.studybits.studybitswallet.credential;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.hyperledger.indy.sdk.IndyException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import nl.quintor.studybits.indy.wrapper.IndyPool;
import nl.quintor.studybits.indy.wrapper.IndyWallet;
import nl.quintor.studybits.indy.wrapper.dto.CredentialInfo;
import nl.quintor.studybits.indy.wrapper.dto.CredentialOffer;
import nl.quintor.studybits.indy.wrapper.message.MessageEnvelopeCodec;
import nl.quintor.studybits.studybitswallet.IndyClient;
import nl.quintor.studybits.studybitswallet.IndyConnection;
import nl.quintor.studybits.studybitswallet.R;
import nl.quintor.studybits.studybitswallet.TestConfiguration;
import nl.quintor.studybits.studybitswallet.room.AppDatabase;
import nl.quintor.studybits.studybitswallet.room.entity.University;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class CredentialFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CredentialFragment() {
    }

    @SuppressWarnings("unused")
    public static CredentialFragment newInstance(int columnCount) {
        CredentialFragment fragment = new CredentialFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_credential_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }


            final CredentialOfferViewModel credentialOfferViewModel = ViewModelProviders.of(this)
                    .get(CredentialOfferViewModel.class);


            LiveData<List<University>> universities = AppDatabase.getInstance(context).universityDao().get();

            LiveData<List<CredentialInfo>> credentials =  credentialOfferViewModel.getCredentials();

            LiveData<List<CredentialOrOffer>> credentialOffers = credentialOfferViewModel.getCredentialOffers();

            credentialOfferViewModel.initCredentials();

            Runnable renewAdapter = () -> {
                List<University> endpoints = universities.getValue();
                if (endpoints == null) {
                    return;
                }

                List<CredentialOrOffer> credentialOrOffers = new ArrayList<>();

                if (credentials.getValue() != null) {
                    credentialOrOffers.addAll(getCredentialOrOffersFromCredentials(endpoints, credentials.getValue()));
                }

                if (credentialOffers.getValue() != null) {
                    credentialOrOffers.addAll(credentialOffers.getValue());
                }

                Log.d("STUDYBITS", "Setting credential offers adapter");
                recyclerView.setAdapter(createAdapter(view, universities.getValue(), credentialOfferViewModel, credentialOrOffers));
            };

            universities.observe(this, endpoints -> {
                credentialOfferViewModel.initCredentialOffers(endpoints);
                renewAdapter.run();
            });

            credentials.observe(this, _var -> renewAdapter.run());
            credentialOffers.observe(this, _var -> renewAdapter.run());

        }



        return view;

    }

    private List<CredentialOrOffer> getCredentialOrOffersFromCredentials(List<University> endpoints, List<CredentialInfo> credentials) {
        return credentials.stream()
                .map(credential -> {
                    Log.d("STUDYBITS", "Credential Referent" + credential.getReferent());
                    return endpoints.stream()
                            .filter(u -> credential.getCredDefId().equals(u.getCredDefId()))
                            .map(u -> CredentialOrOffer.fromCredential(u, credential))
                            .limit(1);
                })
                .flatMap(s -> s).collect(Collectors.toList());
    }

    @NonNull
    private CredentialRecyclerViewAdapter createAdapter(View view, List<University> endpoints, CredentialOfferViewModel credentialOfferViewModel, List<CredentialOrOffer> credentialOrOffers) {
        return new CredentialRecyclerViewAdapter(credentialOrOffers, credentialOrOffer -> {
            if (credentialOrOffer.getCredentialOffer() != null) {
                IndyClient indyClient = new IndyClient(IndyConnection.getInstance(), AppDatabase.getInstance(getContext()));
                CompletableFuture<Void> future = new CompletableFuture<>();
                indyClient.acceptCredentialOffer(this, credentialOrOffer, future);
                future.thenAccept(_void -> {
                    credentialOfferViewModel.initCredentials();
                    Snackbar.make(view, "Obtained credential!", Snackbar.LENGTH_SHORT).show();
                });

            }
            mListener.onListFragmentInteraction(credentialOrOffer);

            credentialOfferViewModel.initCredentialOffers(endpoints);
        });
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(CredentialOrOffer credentialOffer);
    }
}
