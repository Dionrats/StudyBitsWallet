package nl.quintor.studybits.studybitswallet.document;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import nl.quintor.studybits.indy.wrapper.util.AsyncUtil;
import nl.quintor.studybits.studybitswallet.IPFSClient;
import nl.quintor.studybits.studybitswallet.IndyClient;
import nl.quintor.studybits.studybitswallet.IndyConnection;
import nl.quintor.studybits.studybitswallet.R;
import nl.quintor.studybits.studybitswallet.room.AppDatabase;

public class DocumentOptionListDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_ITEM_COUNT = "item_count";
    private static final String ARG_DOCUMENT = "document";
    public static final int ITEM_COUNT = 3;
    private Listener mListener;
    private ProgressBar mProgressBar;

    public static DocumentOptionListDialogFragment newInstance(Document document) {
        final DocumentOptionListDialogFragment fragment = new DocumentOptionListDialogFragment();
        final Bundle args = new Bundle();
        args.putInt(ARG_ITEM_COUNT, ITEM_COUNT);
        args.putSerializable(ARG_DOCUMENT, document);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_documentoption_list_dialog, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new DocumentOptionAdapter(ITEM_COUNT));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final Fragment parent = getParentFragment();
        if (parent != null) {
            mListener = (Listener) parent;
        } else {
            mListener = (Listener) context;
        }
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    private CompletableFuture<File> getFile(Document document) {
        mProgressBar.setVisibility(View.VISIBLE);
        IPFSClient ipfsClient = new IPFSClient(IndyConnection.getInstance(), progress -> {
            if(progress <= 100) {
                mProgressBar.setProgress(progress);
            }else if(progress == 101) {
                mProgressBar.setIndeterminate(true);
            }else if(progress == 102) {
                mProgressBar.setIndeterminate(false);
                mProgressBar.setVisibility(View.INVISIBLE);
            }
        });
        return ipfsClient.retrieveFile(document);
    }

    private CompletableFuture<File> getValidation(Document document, File file) throws ExecutionException, InterruptedException {
        File target = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), document.getName() + ".sbv");
        CompletableFuture<File> future = new CompletableFuture<>();
        try(FileOutputStream fileOutputStream = new FileOutputStream(target))  {
            IndyClient indyClient = new IndyClient(IndyConnection.getInstance(), AppDatabase.getInstance(getContext()));

            JSONObject jsonObject = indyClient.composeDocumentVerification(document, file);
            fileOutputStream.write(jsonObject.toString().getBytes());

            future.complete(target);
        } catch (IOException e) {
            Log.e("StudyBits", e.getMessage());
        }

        return future;
    }

    private void downloadFile(View view, Document document) {
        try{
            getFile(document).thenAccept(file -> promptFileOpen(view, file));
        } catch (Exception e) {
            Log.e("StudyBits", e.getMessage());
        }
    }

    private void downloadValidation(View view, Document document) {
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.setIndeterminate(true);

        getFile(document).thenAccept(file -> {
            try {
                getValidation(document, file).thenAccept(val -> promptFileOpen(view, val));
            } catch (ExecutionException | InterruptedException e) {
                Log.e("StudyBits", "Exception during file-validation download: " + e.getMessage());
                e.printStackTrace();
            }
        });


        mProgressBar.setIndeterminate(false);
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    private void sendFile(View view, Document document) {
        try {
            File file = getFile(document).get();
            CompletableFuture<File> validation = getValidation(document, file);

            promptFileShare(view, file, validation.get());

        } catch (ExecutionException | InterruptedException e) {
            Log.e("StudyBits", e.getMessage());
        }
    }

    private void promptFileOpen(View view, File file) {
        dismiss();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setData(FileProvider.getUriForFile(view.getContext(), "nl.quintor.studybits.fileProvider", file));
        intent = Intent.createChooser(intent, "Choose an application to open with:");
        view.getContext().startActivity(intent);
    }

    private void promptFileShare(View view, File file, File validation) {
        dismiss();

        ArrayList<Uri> files = new ArrayList<>();
        files.add(FileProvider.getUriForFile(view.getContext(), "nl.quintor.studybits.fileProvider", file));
        files.add(FileProvider.getUriForFile(view.getContext(), "nl.quintor.studybits.fileProvider", validation));

        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.putExtra(Intent.EXTRA_SUBJECT, "StudyBits: Document (" + file.getName() + ") and Validation");
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
        intent.setType("*/*");
        view.getContext().startActivity(Intent.createChooser(intent, "Choose an application to share with:"));
    }

    public interface Listener {
        void onDocumentOptionClicked(int position);
    }

    private class ViewHolder extends RecyclerView.ViewHolder {

        final TextView text;

        ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.fragment_documentoption_list_dialog_item, parent, false));
            text = itemView.findViewById(R.id.document_option);
        }
    }

    private class DocumentOptionAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final int mItemCount;

        DocumentOptionAdapter(int itemCount) {
            mItemCount = itemCount;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Document document = (Document) getArguments().getSerializable(ARG_DOCUMENT);

            ((TextView)getView().findViewById(R.id.modal_title)).setText(document.getName());
            ((TextView)getView().findViewById(R.id.modal_type)).setText(String.format("%s%s", getString(R.string.filetype), document.getType()));
            ((TextView)getView().findViewById(R.id.modal_size)).setText(String.format("%s%s", getString(R.string.size), document.readableFileSize()));
            ((TextView)getView().findViewById(R.id.modal_source)).setText(String.format("%s%s", getString(R.string.source), document.getIssuer().getName()));

            mProgressBar = getView().findViewById(R.id.modal_progressBar);

            switch (position) {
                case 0:
                    holder.text.setText(R.string.savefile);
                    holder.text.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_save, 0,0,0);
                    holder.text.setOnClickListener(view -> downloadFile(holder.itemView, document));
                    break;
                case 1:
                    holder.text.setText(R.string.getvalidation);
                    holder.text.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_fingerprint, 0,0,0);
                    holder.text.setOnClickListener(view -> downloadValidation(holder.itemView, document));
                    break;
                case 2:
                    holder.text.setText(R.string.sharefile);
                    holder.text.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_send, 0,0,0);
                    holder.text.setOnClickListener(view -> sendFile(holder.itemView, document));
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return mItemCount;
        }

    }

}
