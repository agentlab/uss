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
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.internal.util.JSONUtil;
import org.eclipse.userstorage.service.IUserStorageService;
import org.eclipse.userstorage.tests.util.USSServer;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final static File applicationFolder =
        new File(System.getProperty("java.io.tmpdir"),
            "uss-server/eclipse_test_123456789");

    @PUT
    @Path("/{token}/{filename}")
    @Override
    public void updateBlob(@PathParam("token") String token, @PathParam("filename") String filename, InputStream blob)
        throws IOException {

        Map<String, Object> value = JSONUtil.parse(blob, "value");

        String etag = UUID.randomUUID().toString();

        File blobFile = new File(applicationFolder + "/" + token + "/" + filename + USSServer.BLOB_EXTENSION);
        IOUtil.mkdirs(blobFile.getParentFile());
        FileOutputStream out = new FileOutputStream(blobFile);

        try
        {
            IOUtil.copy((InputStream)value.get("value"), out);
        }
        finally
        {
            IOUtil.closeSilent((InputStream)value.get("value"));
            IOUtil.close(out);
        }

    }

    @DELETE
    @Path("/{token}/{filename}")
    @Override
    public void deleteBlob(@PathParam("token") String token, @PathParam("filename") String filename) {
        File fblob = new File(applicationFolder + "/" + token + "/" + filename + USSServer.BLOB_EXTENSION);
        File fetag = new File(applicationFolder + "/" + token + "/" + filename + USSServer.ETAG_EXTENSION);


        IOUtil.delete(fblob);
        IOUtil.delete(fetag);
    }

    @GET
    @Produces("application/json")
    @Path("/{token}/{filename}")
    @Override
    public Response getBlob(@PathParam("token") String token, @PathParam("filename") String filename)
        throws IOException {
        File blobFile = new File(applicationFolder + "/" + token + "/" + filename + USSServer.BLOB_EXTENSION);

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

            return Response.ok().entity(stream).type(MediaType.APPLICATION_JSON).build();

    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        // TODO Auto-generated method stub

    }

    @Activate
    public void activate(ComponentContext context) throws IOException {
        Dictionary<String, Object> properties = context.getProperties();
        //properties.put("database.id", "wewe");
        id = (String)properties.get("database.id");
        database = (String)properties.get("database");
        user = (String)properties.get("user");
        password = (String)properties.get("password");
        create = (String)properties.get("create");
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
