package org.eclipse.userstorage.service.model;

import java.io.InputStream;

public class BlobServiceModel {

    private String token;
    private String blobName;
    private InputStream value;
    private String parentFolder;


    public BlobServiceModel() {
	}

    public BlobServiceModel(String token, String blob) {
        this.token = token;
        this.blobName = blob;
	}

    //Put
    public BlobServiceModel(InputStream in, String blobName, String parentFolder) {
        this.value = in;
        this.blobName = blobName;
        this.parentFolder = parentFolder;
    }

    public InputStream getValue() {
        return this.value;
    }

    public String getToken() {
        return token;
	}

    public String getBlobName() {
        return blobName;
	}
}
