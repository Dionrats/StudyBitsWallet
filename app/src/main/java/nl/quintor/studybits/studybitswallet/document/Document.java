package nl.quintor.studybits.studybitswallet.document;

import android.icu.text.DecimalFormat;

import java.io.Serializable;
import java.util.Map;

import nl.quintor.studybits.indy.wrapper.dto.CredentialOffer;
import nl.quintor.studybits.studybitswallet.credential.CredentialOrOffer;
import nl.quintor.studybits.studybitswallet.room.entity.University;

public class Document implements Serializable {
    private String name;
    private String type;
    private String size;
    private String hash;
    private University issuer;
    private boolean fulfilled;
    private CredentialOffer offer;


    public Document(String name, String type, String size, String hash) {
        this.name = name;
        this.type = type;
        this.size = size;
        this.hash = hash;
        this.fulfilled = true;
    }

    public Document(CredentialOffer offer, University issuer) {
        this.offer = offer;
        this.issuer = issuer;
        this.fulfilled = false;
    }

    public Document(CredentialOrOffer credentialOrOffer) {
        if(credentialOrOffer.getCredential() != null) {
            Map<String, String> values = credentialOrOffer.getCredential().getAttrs();

            this.name = values.get("name");
            String[] parts = this.name.split("[.]");
            this.type = parts[parts.length-1];
            this.size = values.get("size");
            this.hash = values.get("hash");

            this.issuer = credentialOrOffer.getUniversity();

            this.fulfilled = true;
        } else {
            this.issuer = credentialOrOffer.getUniversity();
            this.offer = credentialOrOffer.getCredentialOffer();
            this.fulfilled = false;
        }
    }

    public String readableFileSize() {
        int s = Integer.valueOf(size);
        if(s <= 0) return "0";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(s)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(s/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public boolean isFulfilled() {
        return fulfilled;
    }

    public void setFulfilled(boolean fulfilled) {
        this.fulfilled = fulfilled;
    }

    public CredentialOffer getOffer() {
        return offer;
    }

    public void setOffer(CredentialOffer offer) {
        this.offer = offer;
    }

    public University getIssuer() {
        return issuer;
    }

    public void setIssuer(University issuer) {
        this.issuer = issuer;
    }

}
