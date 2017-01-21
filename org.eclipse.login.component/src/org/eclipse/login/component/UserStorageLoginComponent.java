/**
 *
 */
package org.eclipse.login.component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.eclipse.login.service.IUserStorageLoginService;
import org.eclipse.login.session.Session;
import org.eclipse.login.user.User;
import org.eclipse.session.service.IUserStorageSessionService;
import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.internal.util.JSONUtil;
import org.eclipse.userstorage.spi.Credentials;
import org.eclipse.userstorage.tests.util.FixedCredentialsProvider;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

/**
 * @author Zagrebaev_D
 *
 */

@Component(enabled = true, immediate = true,
    property = {
        "service.exported.interfaces=*",
        "service.exported.configs=ecf.jaxrs.jersey.server",
        "ecf.jaxrs.jersey.server.urlContext=http://localhost:8080", "ecf.jaxrs.jersey.server.alias=/api",
        "ecf.jaxrs.jersey.server.service.alias=/user",
        "service.pid=org.eclipse.userstorage.service.host.UserStorageComponent" })

public class UserStorageLoginComponent
    implements IUserStorageLoginService, ManagedService, IUserStorageSessionService {

    private final Map<String, User> users = new HashMap<>();

    private final Map<String, Session> sessions = new HashMap<>();

    @Override
    public Response postLogin(InputStream creditianals) {

        Map<String, Object> requestObject = null;
        try
        {
            requestObject = JSONUtil.parse(creditianals, null);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String username = (String)requestObject.get("username"); //$NON-NLS-1$
        String password = (String)requestObject.get("password"); //$NON-NLS-1$

        User user = users.get(username);
        if (user == null || password == null || !password.equals(user.getPassword()))
        {
            return Response.status(HttpServletResponse.SC_UNAUTHORIZED).build();
        }

        Session session = addSession(user);
        NewCookie cookie = new NewCookie("SESSION", session.getID(), "/", "", "uss", 100, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        Map<String, Object> responseObject = new LinkedHashMap<>();
        responseObject.put("sessid", session.getID()); //$NON-NLS-1$
        responseObject.put("token", session.getCSRFToken()); //$NON-NLS-1$
        InputStream body = JSONUtil.build(responseObject);

        StreamingOutput stream = new StreamingOutput()
        {
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                IOUtil.copy(body, os);
                os.flush();
                IOUtil.closeSilent(body);
            }
        };

        return Response.ok().entity(stream).type(MediaType.APPLICATION_JSON).cookie(cookie).build();

    }

    public User addUser(Credentials credentials) {
        return addUser(credentials.getUsername(), credentials.getPassword());
    }

    public User addUser(String username, String password) {
        User user = new User(username, password);
        users.put(user.getUsername(), user);
        return user;
    }

    private Session addSession(User user) {
        Session session = new Session(user);
        sessions.put(session.getID(), session);
        return session;
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        // TODO Auto-generated method stub

    }

    @Activate
    public void activate(ComponentContext context) throws IOException {
        addUser(FixedCredentialsProvider.DEFAULT_CREDENTIALS);
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        System.out.println("Login service stopped"); //$NON-NLS-1$
    }

    @Modified
    public void modify() {
        System.out.println("Login service modified"); //$NON-NLS-1$
    }

    @Override
    public boolean isAuth(String csrfToken, String sessionID) {
        return getSession(csrfToken, sessionID) != null ? true : false;
    }

    @Override
    public String getUserLogin(String sessionID) {
        Session session = sessions.get(sessionID);
        return session == null ? null : session.getUser().getUsername();
    }

    private Session getSession(String csrfToken, String sessionID) {
        if (csrfToken != null)
        {
            Session session = sessions.get(sessionID);

            if (session != null && session.getCSRFToken().equals(csrfToken))
            {
                return session;
            }
        }

        return null;
    }

}
