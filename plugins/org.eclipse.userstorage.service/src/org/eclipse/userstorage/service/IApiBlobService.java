/**
 * Copyright (C) 2016, 1C
 */
package org.eclipse.userstorage.service;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response;

public interface IApiBlobService {

    public Response delete(String urlToken, String urlFilename, String headerIfMatch, String headerxCsrfToken,
        String cookieSESSION);

    public Response put(String urlToken, String urlFilename, InputStream blob, String headerIfMatch,
        String headerxCsrfToken, String cookieSESSION)
        throws IOException;

    public Response get(String urlToken, String urlFilename, String headerIfNoneMatch, String queryPageSize,
        String queryPage, String headerxCsrfToken, String cookieSESSION) throws IOException;

}

