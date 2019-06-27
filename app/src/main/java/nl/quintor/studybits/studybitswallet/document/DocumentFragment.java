package nl.quintor.studybits.studybitswallet.document;

import android.arch.lifecycle.LiveData;
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
import java.util.stream.Collectors;

import nl.quintor.studybits.indy.wrapper.IndyPool;
import nl.quintor.studybits.indy.wrapper.IndyWallet;
import nl.quintor.studybits.indy.wrapper.dto.CredentialInfo;
import nl.quintor.studybits.indy.wrapper.message.MessageEnvelopeCodec;
import nl.quintor.studybits.studybitswallet.IndyClient;
import nl.quintor.studybits.studybitswallet.R;
import nl.quintor.studybits.studybitswallet.TestConfiguration;
import nl.quintor.studybits.studybitswallet.credential.CredentialOrOffer;
import nl.quintor.studybits.studybitswallet.room.AppDatabase;
import nl.quintor.studybits.studybitswallet.room.entity.University;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class DocumentFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;

    protected IndyPool indyPool;
    protected IndyWallet studentWallet;
    protected MessageEnvelopeCodec studentCodec;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public DocumentFragment() {
    }

    @SuppressWarnings("unused")
    public static DocumentFragment newInstance(int columnCount) {
        DocumentFragment fragment = new DocumentFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    private void initWallet() {
        try {
            if (indyPool == null || studentWallet == null) {
                indyPool = new IndyPool("testPool");
                studentWallet = IndyWallet.open(indyPool, "student_wallet", TestConfiguration.STUDENT_SEED, TestConfiguration.STUDENT_DID);
                studentCodec = new MessageEnvelopeCodec(studentWallet);
            }
        } catch (IndyException | ExecutionException | InterruptedException | JsonProcessingException e) {
            Log.e("STUDYBITS", "Exception on resume " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            studentWallet.close();
            indyPool.close();
        } catch (Exception e) {
            Log.e("STUDYBITS", "Exception on pause" + e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initWallet();
        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_document_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            final DocumentViewModel documentViewModel = ViewModelProviders.of(this)
                    .get(DocumentViewModel.class);


            LiveData<List<University>> universities = AppDatabase.getInstance(context).universityDao().get();

            LiveData<List<CredentialInfo>> documentCredentials = documentViewModel.getDocuments();
            LiveData<List<CredentialOrOffer>> documentOffers =  documentViewModel.getDocumentOffers();

            documentViewModel.initDocuments(studentWallet);

            Runnable renewAdapter = () -> {
                List<University> endpoints = universities.getValue();
                if (endpoints == null) {
                    return;
                }

                List<CredentialOrOffer> documents = new ArrayList<>();

                if (documentCredentials.getValue() != null) {
                    documents.addAll(getCredentialOrOffersFromCredentials(endpoints, documentCredentials.getValue()));
                }

                if (documentOffers.getValue() != null) {
                    documents.addAll(documentOffers.getValue());
                }

                Log.d("STUDYBITS", "Setting credential offers adapter");
                recyclerView.setAdapter(createAdapter(view, universities.getValue(), documentViewModel, documents));
            };

            universities.observe(this, endpoints -> {
                documentViewModel.initDocumentOffers(endpoints, studentCodec);
                renewAdapter.run();
            });

            documentCredentials.observe(this, _var -> renewAdapter.run());
            documentOffers.observe(this, _var -> renewAdapter.run());

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
    private RecyclerView.Adapter createAdapter(View view, List<University> endpoints, DocumentViewModel documentViewModel, List<CredentialOrOffer> documents) {
        return new DocumentRecyclerViewAdapter(documents, studentWallet, document -> {
            if (document.getCredentialOffer() != null) {
                initWallet();
                IndyClient indyClient = new IndyClient(studentWallet, AppDatabase.getInstance(getContext()));
                CompletableFuture<Void> future = new CompletableFuture<>();

                indyClient.acceptCredentialOffer(this, document, future);
                future.thenAccept(_void -> {
                    documentViewModel.initDocuments(studentWallet);
                    Snackbar.make(view, "Obtained Document!", Snackbar.LENGTH_SHORT).show();
                });
            }
            mListener.onListFragmentInteraction(document);

            documentViewModel.initDocumentOffers(endpoints, studentCodec);
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
        void onListFragmentInteraction(CredentialOrOffer document);
    }
}
