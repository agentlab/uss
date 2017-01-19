/**
 * Copyright (C) 2016, 1C
 */
package org.eclipse.userstorage.service;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response;

//@Path("/api/blob")
public interface IApiBlobService {

//    @DELETE
//    @Path("/{token}/{filename}")
    public Response delete(/*@PathParam("token")*/ String urlToken, /*@PathParam("filename")*/ String urlFilename,
        /*@Context HttpHeaders*/ String headerIfMatch);

//    public Response postLogin(InputStream creditianals);
//
//    public OutputStream retrieveBlob(BlobServiceModel blob) throws IOException;
//
//    public void getListBlobs();
//

//    @PUT
//    @Path("/{token}/{filename}")
    public Response put(/*@PathParam("token")*/ String urlToken, /*@PathParam("filename")*/ String urlFilename,
        InputStream blob, /*@Context*/ String headerIfMatch)
        throws IOException;

//    @GET
//    @Produces("application/json")
//    @Path("/{token}/{filename}")
    public Response get(/*@PathParam("token")*/ String urlToken, /*@PathParam("filename")*/ String urlFilename,
        /*@Context*/ String headerIfMatch, String queryPageSize, String queryPage) throws IOException;

}

