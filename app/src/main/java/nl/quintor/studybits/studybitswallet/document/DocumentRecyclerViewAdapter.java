package nl.quintor.studybits.studybitswallet.document;

import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.hyperledger.indy.sdk.IndyException;

import nl.quintor.studybits.indy.wrapper.IndyPool;
import nl.quintor.studybits.indy.wrapper.IndyWallet;
import nl.quintor.studybits.studybitswallet.IPFSClient;
import nl.quintor.studybits.studybitswallet.R;
import nl.quintor.studybits.studybitswallet.TestConfiguration;
import nl.quintor.studybits.studybitswallet.credential.CredentialOrOffer;
import nl.quintor.studybits.studybitswallet.document.DocumentFragment.OnListFragmentInteractionListener;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Document} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
public class DocumentRecyclerViewAdapter extends RecyclerView.Adapter<DocumentRecyclerViewAdapter.ViewHolder> {

    private final List<CredentialOrOffer> mDocuments;
    private final IndyWallet mWallet;
    private final OnListFragmentInteractionListener mListener;

    public DocumentRecyclerViewAdapter(List<CredentialOrOffer> documents, IndyWallet indyWallet, OnListFragmentInteractionListener listener) {
        mDocuments = documents;
        mWallet = indyWallet;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_document, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mDocument = new Document(mDocuments.get(position));

        if(holder.mDocument.isFulfilled()) {
            holder.mTypeView.setText(holder.mDocument.getType());
            holder.mNameView.setText(holder.mDocument.getName());
            holder.mSizeView.setText(holder.mDocument.getSize());

            holder.mDownloadButton.setOnClickListener(v -> {
                Snackbar.make(v, "downloading " + holder.mDocument.getName(), Snackbar.LENGTH_SHORT).show();

                //TODO replace button with spinner

                try{
                    IPFSClient ipfsClient = new IPFSClient(mWallet);

                    File file = ipfsClient.retrieveFile(holder.mDocument.getName(), holder.mDocument.getHash(), holder.mDocument.getIssuer().getTheirDid());

                    Snackbar snackbar = Snackbar.make(v, holder.mDocument.getName() + " is gedownload", Snackbar.LENGTH_LONG);
                    snackbar.setAction(R.string.open_file_action, _void -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setData(FileProvider.getUriForFile(holder.mView.getContext(), "nl.quintor.studybits.fileProvider", file));
                        intent = Intent.createChooser(intent, "Choose an application to open with:");
                        v.getContext().startActivity(intent);
                    });
                    snackbar.show();
                } catch (Exception e) {
                    Log.e("StudyBits", e.getMessage());
                }


            });

        }else {
            holder.mSizeView.setVisibility(View.GONE);

            holder.mTypeView.setText(holder.mDocument.getIssuer().getName());
            holder.mNameView.setText(holder.mDocument.getOffer().getSchemaId());

            holder.mDownloadButton.setImageResource(R.drawable.ic_new);
            holder.mView.setBackgroundColor(ContextCompat.getColor(holder.mView.getContext(), R.color.colorCredentialOffer));
        }

        holder.mView.setOnClickListener(v -> {
            if(mListener != null) {
                mListener.onListFragmentInteraction(mDocuments.get(position));
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDocuments.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mTypeView;
        public final TextView mNameView;
        public final TextView mSizeView;
        public final ImageButton mDownloadButton;
        public Document mDocument;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mTypeView = view.findViewById(R.id.file_type);
            mNameView = view.findViewById(R.id.file_name);
            mSizeView = view.findViewById(R.id.file_size);
            mDownloadButton = view.findViewById(R.id.file_download);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mNameView.getText() + "'";
        }
    }
}
