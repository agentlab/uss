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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.internal.util.JSONUtil;
import org.eclipse.userstorage.internal.util.StringUtil;
import org.eclipse.userstorage.service.IApiBlobService;
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
        "ecf.jaxrs.jersey.server.urlContext=http://localhost:8080", "ecf.jaxrs.jersey.server.alias=/api",
        "ecf.jaxrs.jersey.server.service.alias=/blob",
        "service.pid=org.eclipse.userstorage.service.host.UserStorageComponent" })

public class UserStorageComponent
    implements ManagedService, IApiBlobService {

    private String id;
    private String database;
    private String user;
    private String password;
    private String create;
    private final Set<String> applicationTokens = new HashSet<>();

    private final File applicationFolder =
        new File(System.getProperty("java.io.tmpdir"), "uss-server");
    private final static String userApp = "eclipse_test_123456789";

//    @PUT
//    @Path("/{token}/{filename}")
//    @Override
    @Override
    public Response put(/*@PathParam("token")*/ String urltoken,
        /*@PathParam("filename")*/ String urlfilename,
        InputStream blob, /*@Context*/ String headerIf_Match) throws IOException

    {

        if (!this.isExistAppToken(urltoken))
        {
            return Response.status(404).build();
        }

        File etagFile = getUserFile(userApp, urltoken, urlfilename, USSServer.ETAG_EXTENSION);
//        String ifMatch = getEtag(headers, "If-Match");

        if (etagFile.exists())
        {
            String etag = IOUtil.readUTF(etagFile);

            if (StringUtil.isEmpty(headerIf_Match) || !headerIf_Match.equals(etag))
            {
                return Response.status(409).header("ETag", "\"" + etag + "\"").build();
            }
        }

        String etag = UUID.randomUUID().toString();

        File blobFile = getUserFile(userApp, urltoken, urlfilename, USSServer.BLOB_EXTENSION);
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

//    @DELETE
//    @Path("/{token}/{filename}")
    @Override
    public Response delete(/*(@PathParam("token")*/ String token, /*@PathParam("filename")*/ String filename,
        /*@Context HttpHeaders*/ String headerIfMatch) {

        File etagFile = getUserFile(userApp, token, filename, USSServer.ETAG_EXTENSION);

        if (!this.isExistAppToken(token) || !etagFile.exists())
        {
            return Response.status(404).build();
        }

        String etag = IOUtil.readUTF(etagFile);
        String ifMatch = getEtag(headerIfMatch/*, "If-Match"*/);
        if (ifMatch != null && !ifMatch.equals(etag))
        {
            return Response.status(494).build();
        }

        File blobFile = getUserFile(userApp, token, filename, USSServer.BLOB_EXTENSION);

        IOUtil.delete(blobFile);
        IOUtil.delete(etagFile);

        return Response.noContent().build();
    }

//    @GET
//    @Produces("application/json")
//    @Path("/{token}/{filename}")
    @Override
    public Response get(/*@PathParam("token")*/ String urltoken, /*@PathParam("filename")*/ String urlfilename,
        /*@Context*/ String headerIfMatch, String queryPageSize, String queryPage) throws IOException {

        File etagFile = getUserFile(userApp, urltoken, urlfilename, USSServer.ETAG_EXTENSION);

        if (!this.isExistAppToken(urltoken) || !etagFile.exists())
        {
            return Response.status(404).build();
        }

        String etag = IOUtil.readUTF(etagFile);
//        String ifNoneMatch = getEtag(headers, "If-None-Match");
        if (headerIfMatch != null && headerIfMatch.equals(etag))
        {
            return Response.status(304).build();
        }

        if (urlfilename == null)
        {
            File applicationFolder = getApplicationFolder(user, urltoken);
            return retrieveProperties(applicationFolder, queryPageSize, queryPage);
        }

        File blobFile = getUserFile(userApp, urltoken, urlfilename, USSServer.BLOB_EXTENSION);

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

    protected Response retrieveProperties(File applicationFolder, String queryPageSize, String queryPage) {

        String applicationToken = applicationFolder.getName();

        int pageSize = getIntParameter(queryPageSize, 20);
        if (pageSize < 1 || pageSize > 100)
        {
            return Response.status(HttpServletResponse.SC_BAD_REQUEST).build();
//            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid page size");
//            return;
        }

        int page = getIntParameter(queryPage, 20);
        if (page < 1)
        {
            return Response.status(HttpServletResponse.SC_BAD_REQUEST).build();
//            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid page");
//            return;
        }

        boolean empty = true;

        StringBuilder builder = new StringBuilder();
        builder.append('[');

        File[] files = applicationFolder.listFiles();
        if (files != null)
        {
            int first = (page - 1) * pageSize + 1;
            System.out.println("##### " + first);
            int i = 0;

            for (File file : files)
            {
                String name = file.getName();
                if (name.endsWith(USSServer.ETAG_EXTENSION))
                {
                    if (++i >= first)
                    {
                        String key = name.substring(0, name.length() - USSServer.ETAG_EXTENSION.length());
                        System.out.println("##### " + key);
                        String etag = IOUtil.readUTF(file);

                        if (empty)
                        {
                            empty = false;
                        }
                        else
                        {
                            builder.append(",");
                        }

                        builder.append("{\"application_token\":\"");
                        builder.append(applicationToken);
                        builder.append("\",\"key\":\"");
                        builder.append(key);
                        builder.append("\",\"etag\":\"");
                        builder.append(etag);
                        builder.append("\"}");

                        if (--pageSize == 0)
                        {
                            break;
                        }
                    }
                }
            }
        }

        builder.append(']');
        System.out.println(builder);

//        response.setStatus(HttpServletResponse.SC_OK);
//        response.setContentType("application/json");

        InputStream body = IOUtil.streamUTF(builder.toString());

        StreamingOutput stream = new StreamingOutput()
        {
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                IOUtil.copy(body, os);
                os.flush();
                IOUtil.closeSilent(body);
            }
        };

//        try
//        {
//            ServletOutputStream out = response.getOutputStream();
//            IOUtil.copy(body, out);
//            out.flush();
//        }
//        finally
//        {
//            IOUtil.closeSilent(body);
//        }

        return Response.ok().entity(stream).type(MediaType.APPLICATION_JSON).build();
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

    private String getEtag(String header) {
        if (header != null)
        {
            header = header.substring(1, header.length() - 1);
        }
        return header;
    }

    private int getIntParameter(String queryParam, int defValue) {
        if (queryParam != null)
        {
            try
            {
                return Integer.parseInt(queryParam);
            }
            catch (NumberFormatException ex)
            {
                //$FALL-THROUGH$
            }
        }

        return defValue;
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
