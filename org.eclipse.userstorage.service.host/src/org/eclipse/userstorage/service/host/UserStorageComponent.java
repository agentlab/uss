/**
 * Copyright (C) 2016, 1C
 */
package org.eclipse.userstorage.service.host;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.internal.util.JSONUtil;
import org.eclipse.userstorage.internal.util.StringUtil;
import org.eclipse.userstorage.service.IUserStorageService;
import org.eclipse.userstorage.tests.util.USSServer;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

/**
 * @author admin
 *
 */

@Component(enabled = true, immediate = true,
    property = { "service.exported.interfaces=*", "service.exported.configs=ecf.jaxrs.jersey.server",
        "ecf.jaxrs.jersey.server.urlContext=http://localhost:8080", "ecf.jaxrs.jersey.server.alias=/",
        "service.pid=org.eclipse.userstorage.service.host.UserStorageComponent" })

public class UserStorageComponent
    implements ManagedService, IUserStorageService {

    private String id;
    private String database;
    private String user;
    private String password;
    private String create;
    private final Set<String> applicationTokens = new HashSet<>();

    private final File applicationFolder =
        new File(System.getProperty("java.io.tmpdir"), "uss-server");
    private final static String userApp = "eclipse_test_123456789";

    @PUT
    @Path("/{token}/{filename}")
    @Override
    public Response updateBlob(@PathParam("token") String token, @PathParam("filename") String filename,
        InputStream blob, @Context HttpHeaders headers)
        throws IOException {

        if (!this.isExistAppToken(token))
        {
            return Response.status(404).build();
        }

        File etagFile = getUserFile(userApp, token, filename, USSServer.ETAG_EXTENSION);
        String ifMatch = getEtag(headers, "If-Match");

        if (etagFile.exists())
        {
            String etag = IOUtil.readUTF(etagFile);

            if (StringUtil.isEmpty(ifMatch) || !ifMatch.equals(etag))
            {
                return Response.status(409).header("ETag", "\"" + etag + "\"").build();
            }
        }

        String etag = UUID.randomUUID().toString();

        File blobFile = getUserFile(userApp, token, filename, USSServer.BLOB_EXTENSION);
        IOUtil.mkdirs(blobFile.getParentFile());
        FileOutputStream out = new FileOutputStream(blobFile);
        Map<String, Object> value = JSONUtil.parse(blob, "value");

        try
        {
            IOUtil.copy((InputStream)value.get("value"), out);
        }
        finally
        {
            IOUtil.closeSilent((InputStream)value.get("value"));
            IOUtil.close(out);
        }

        IOUtil.writeUTF(etagFile, etag);

        return Response.status(etagFile.exists() ? 200 : 201).header("Etag", "\"" + etag + "\"").build();

    }

    @DELETE
    @Path("/{token}/{filename}")
    @Override
    public Response deleteBlob(@PathParam("token") String token, @PathParam("filename") String filename,
        @Context HttpHeaders headers) {

        File etagFile = getUserFile(userApp, token, filename, USSServer.ETAG_EXTENSION);

        if (!this.isExistAppToken(token) || !etagFile.exists())
        {
            return Response.status(404).build();
        }

        String etag = IOUtil.readUTF(etagFile);
        String ifMatch = getEtag(headers, "If-Match");
        if (ifMatch != null && !ifMatch.equals(etag))
        {
            return Response.status(494).build();
        }

        File blobFile = getUserFile(userApp, token, filename, USSServer.BLOB_EXTENSION);

        IOUtil.delete(blobFile);
        IOUtil.delete(etagFile);

        return Response.noContent().build();
    }

    @GET
    @Produces("application/json")
    @Path("/{token}/{filename}")
    @Override
    public Response getBlob(@PathParam("token") String token, @PathParam("filename") String filename,
        @Context HttpHeaders headers)
        throws IOException {

        File etagFile = getUserFile(userApp, token, filename, USSServer.ETAG_EXTENSION);

        if (!this.isExistAppToken(token) || !etagFile.exists())
        {
            return Response.status(404).build();
        }

        String etag = IOUtil.readUTF(etagFile);
        String ifNoneMatch = getEtag(headers, "If-None-Match");
        if (ifNoneMatch != null && ifNoneMatch.equals(etag))
        {
            return Response.status(304).build();
        }

        File blobFile = getUserFile(userApp, token, filename, USSServer.BLOB_EXTENSION);

        InputStream body = JSONUtil.build(Collections.singletonMap("value", new FileInputStream(blobFile)));

            StreamingOutput stream = new StreamingOutput()
            {
                @Override
                public void write(OutputStream os) throws IOException, WebApplicationException {
                    IOUtil.copy(body, os);
                    os.flush();
                IOUtil.closeSilent(body);
                }
            };

        return Response.ok().header("Etag", "\"" + etag + "\"").entity(stream).type(MediaType.APPLICATION_JSON).build();
    }

    private Set<String> getApplicationTokens() {
        return this.applicationTokens;
    }

    private boolean isExistAppToken(String appToken) {
        return this.applicationTokens.contains(appToken);
    }

    private File getUserFile(String user, String applicationToken, String key, String extension) {
        return new File(getApplicationFolder(user, applicationToken), key + StringUtil.safe(extension));
    }

    private File getApplicationFolder(String user, String applicationFolder) {
        return new File(new File(this.applicationFolder, user), applicationFolder);
    }

    private String getEtag(HttpHeaders headers, String headerName) {
        List<String> h = headers.getRequestHeader(headerName);
        String eTag = h == null ? null : h.get(0);
        if (eTag != null)
        {
            eTag = eTag.substring(1, eTag.length() - 1);
        }
        return eTag;
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        // TODO Auto-generated method stub

    }

    @Activate
    public void activate(ComponentContext context) throws IOException {
        Dictionary<String, Object> properties = context.getProperties();
        id = (String)properties.get("database.id");
        database = (String)properties.get("database");
        user = (String)properties.get("user");
        password = (String)properties.get("password");
        create = (String)properties.get("create");
        this.getApplicationTokens().add("pDKTqBfDuNxlAKydhEwxBZPxa4q");
        System.out.println("USS service started");

    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        System.out.println("USS service stopped");
    }

    @Modified
    public void modify() {
        System.out.println("USS service modified");
    }

}
