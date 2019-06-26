package nl.quintor.studybits.studybitswallet.messages;

import java.io.Serializable;
import java.util.List;

import nl.quintor.studybits.studybitswallet.document.Document;

public class AuthcryptableDocuments implements Serializable {
    private List<Document> documents;

    public AuthcryptableDocuments(){}

    public AuthcryptableDocuments(List<Document> documents) {
        this.documents = documents;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }
}
