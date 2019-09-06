package nl.quintor.studybits.studybitswallet.document;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import nl.quintor.studybits.indy.wrapper.IndyWallet;
import nl.quintor.studybits.studybitswallet.R;
import nl.quintor.studybits.studybitswallet.credential.CredentialOrOffer;
import nl.quintor.studybits.studybitswallet.document.DocumentFragment.OnListFragmentInteractionListener;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Document} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
public class DocumentRecyclerViewAdapter extends RecyclerView.Adapter<DocumentRecyclerViewAdapter.ViewHolder> {

    private final List<CredentialOrOffer> mDocuments;
    private final OnListFragmentInteractionListener mListener;

    public DocumentRecyclerViewAdapter(List<CredentialOrOffer> documents, OnListFragmentInteractionListener listener) {
        mDocuments = documents;
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
            holder.mSizeView.setText(holder.mDocument.readableFileSize());

            holder.mDownloadButton.setOnClickListener(v -> mListener.onListFragmentInteraction( null, holder.mDocument));
        }else {
            holder.mSizeView.setVisibility(View.GONE);

            holder.mTypeView.setText(holder.mDocument.getIssuer().getName());
            holder.mNameView.setText(holder.mDocument.getOffer().getSchemaId());

            holder.mDownloadButton.setImageResource(R.drawable.ic_new);
            holder.mView.setBackgroundColor(ContextCompat.getColor(holder.mView.getContext(), R.color.colorCredentialOffer));
            holder.mView.setOnClickListener(v -> mListener.onListFragmentInteraction(mDocuments.get(position), holder.mDocument));
        }
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
