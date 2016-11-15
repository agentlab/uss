package org.eclipse.userstorage.service.model;


public class BlobRequest {

    private String token;
    private String blob;

    public BlobRequest() {
	}

    public BlobRequest(String token, String blob) {
        this.token = token;
        this.blob = blob;
	}

    public String getToken() {
        return token;
	}

    public String getBlob() {
        return blob;
	}
}
