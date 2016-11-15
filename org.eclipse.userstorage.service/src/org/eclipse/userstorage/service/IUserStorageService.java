/**
 * Copyright (C) 2016, 1C
 */
package org.eclipse.userstorage.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.userstorage.service.model.BlobRequest;

public interface IUserStorageService {

    public void deleteBlob(BlobRequest blob);

    public OutputStream retrieveBlob(BlobRequest blob) throws IOException;

    public void getBlobs();

    public void updateBlob(InputStream blob) throws IOException;

}

