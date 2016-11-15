/**
 * Copyright (C) 2016, 1C
 */
package org.eclipse.userstorage.service.host;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

/**
 * @author admin
 *
 */

@Component(enabled = true, immediate = true)
public class UserStorageConfigurer
    implements ManagedService {

    @Reference
    public void bindCm(ConfigurationAdmin configAdmin) throws IOException {
        if (configAdmin != null)
        {
            Configuration config =
                configAdmin.getConfiguration("org.eclipse.userstorage.service.host.UserStorageComponent", null);

            Dictionary<String, Object> props = config.getProperties();
            if (props == null)
            {
                props = new Hashtable<>();
            }
            // configure the Dictionary
            props.put("key", "keyalue");

            // push the configuration dictionary to the SmsService
            config.update(props);
        }
    }

    public void unbindCm(ConfigurationAdmin configAdmin) {

    }

    @Activate
    public void activate(ComponentContext context) {
        System.out.println("UserStorageConfigurer service started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        System.out.println("UserStorageConfigurer service stopped");
    }

    @Modified
    public void modify() {
        System.out.println("UserStorageConfigurer service modified");
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        // TODO Auto-generated method stub

    }

}
