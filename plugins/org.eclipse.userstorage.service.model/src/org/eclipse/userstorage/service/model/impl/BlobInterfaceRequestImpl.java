package org.eclipse.userstorage.service.model.impl;

import org.eclipse.userstorage.service.model.IBlobInterfaceRequest;;

public class BlobInterfaceRequestImpl implements IBlobInterfaceRequest {

    private String token;
    private String blob;

    public BlobInterfaceRequestImpl() {
	}

    public BlobInterfaceRequestImpl(String token, String blob) {
        this.token = token;
        this.blob = blob;
	}

    @Override
    public String getBlob() {
        // TODO Auto-generated method stub
        return blob;
    }

    @Override
    public String getToken() {
        // TODO Auto-generated method stub
        return token;
    }
}
