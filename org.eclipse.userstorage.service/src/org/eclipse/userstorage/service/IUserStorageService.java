/**
 * Copyright (C) 2016, 1C
 */
package org.eclipse.userstorage.service;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

@Path("/api/blob")
public interface IUserStorageService {

    @DELETE
    @Path("/{token}/{filename}")
    public Response deleteBlob(@PathParam("token") String token, @PathParam("filename") String filename,
        @Context HttpHeaders headers);
//
//    public OutputStream retrieveBlob(BlobServiceModel blob) throws IOException;
//
//    public void getListBlobs();
//

    @PUT
    @Path("/{token}/{filename}")
    public Response updateBlob(@PathParam("token") String token, @PathParam("filename") String filename,
        InputStream blob, @Context HttpHeaders headers)
        throws IOException;

    @GET
    @Produces("application/json")
    @Path("/{token}/{filename}")
    public Response getBlob(@PathParam("token") String token, @PathParam("filename") String filename,
        @Context HttpHeaders headers)
        throws IOException;

}

