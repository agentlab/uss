/**
 *
 */
package org.eclipse.userstorage.login.component;

/**
 * @author Zagrebaev_D
 *
 */
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
public class UserStorageLoginConfigurer
    implements ManagedService {

    @Reference
    public void bindCm(ConfigurationAdmin configAdmin) throws IOException {
        if (configAdmin != null)
        {
            Configuration config =
                configAdmin.getConfiguration("org.eclipse.userstorage.login.component.UserStorageLoginComponent", null); //$NON-NLS-1$

            Dictionary<String, Object> props = config.getProperties();
            if (props == null)
            {
                props = new Hashtable<>();
            }
            // configure the Dictionary
            props.put("key", "keyalue"); //$NON-NLS-1$ //$NON-NLS-2$

            // push the configuration dictionary to the SmsService
            config.update(props);
        }
    }

    public void unbindCm(ConfigurationAdmin configAdmin) {

    }

    @Activate
    public void activate(ComponentContext context) {
        System.out.println("UserStorageLoginComponent service started"); //$NON-NLS-1$
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        System.out.println("UserStorageLoginComponent service stopped"); //$NON-NLS-1$
    }

    @Modified
    public void modify() {
        System.out.println("UserStorageLoginComponent service modified"); //$NON-NLS-1$
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        // TODO Auto-generated method stub

    }

}

