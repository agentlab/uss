/**
 * Copyright (C) 2016, 1C
 */
package org.eclipse.userstorage.service.host;

import java.io.ByteArrayOutputStream;
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

import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.internal.util.JSONUtil;
import org.eclipse.userstorage.service.IUserStorageService;
import org.eclipse.userstorage.service.model.BlobRequest;
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
        "ecf.jaxrs.jersey.server.urlContext=http://localhost:8080", "ecf.jaxrs.jersey.server.alias=/uss",
        "service.pid=org.eclipse.userstorage.service.host.UserStorageComponent" })
public class UserStorageComponent
    implements ManagedService, IUserStorageService {

    private String id;
    private String database;
    private String user;
    private String password;
    private String create;

    @Override
    public void deleteBlob(BlobRequest blob) {
        // TODO Auto-generated method stub

        File fblob = new File(blob.getBlob());
        File fetag = new File(blob.getBlob());

        IOUtil.delete(fblob);

    }

    @Override
    public OutputStream retrieveBlob(BlobRequest blob) throws IOException {
        // TODO Auto-generated method stub

        File blobFile = new File(blob.getBlob());

        InputStream body = JSONUtil.build(Collections.singletonMap("value", new FileInputStream(blobFile)));

        try
        {
//            ServletOutputStream out = response.getOutputStream();
            OutputStream out = new ByteArrayOutputStream();
            IOUtil.copy(body, out);
            out.flush();
            return out;
        }
        finally
        {
            IOUtil.closeSilent(body);
        }

    }

    @Override
    public void getBlobs() {
        // TODO Auto-generated method stub
    }

    @Override
    public void updateBlob(InputStream blob) throws IOException {
        // TODO Auto-generated method stub


        String etag = UUID.randomUUID().toString();

        File blobFile = new File(
            "/var/folders/zw/_z3g2qqs0vd9h1fl5lb2qc_r0000gn/T/uss-server/eclipse_test_123456789/"
                + "pDKTqBfDuNxlAKydhEwxBZPxa4q/blob.blob");
        IOUtil.mkdirs(blobFile.getParentFile());
        FileOutputStream out = new FileOutputStream(blobFile);
        InputStream body = null;

        try
        {
          Map<String, Object> requestObject = JSONUtil.parse(blob, "value");
          body = (InputStream)requestObject.get("value");

            IOUtil.copy(body, out);
        }
        finally
        {
          IOUtil.closeSilent(body);
//          IOUtil.close(out);
        }

        System.out.println("LOL");
//        blob.
        int a = 0;
        a = 4;

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
