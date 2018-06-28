/**
 *
 */
package org.eclipse.userstorage.login.component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.internal.util.JSONUtil;
import org.eclipse.userstorage.login.service.IUserStorageLoginService;
import org.eclipse.userstorage.session.service.IUserStorageSessionService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com._1c.cloud.edt.workspace.setup.security.keycloak.KeycloakAuthenticationException;
import com._1c.cloud.edt.workspace.setup.security.keycloak.KeycloakRestRequester;
import com._1c.cloud.edt.workspace.setup.security.keycloak.transfer.Credentials;

/**
 * @author Zagrebaev_D
 *
 */

@Component(property= {
	"service.exported.interfaces:String=*"
	, "service.exported.configs:String=ecf.jaxrs.jersey.server"
	, "ecf.jaxrs.jersey.server.uri:String=http://localhost:8080/api/user"
	, "ecf.jaxrs.jersey.server.exported.interfaces=org.eclipse.userstorage.login.service.IUserStorageLoginService"}
	, immediate=true)
@Path("/")
public class UserStorageLoginComponent implements IUserStorageLoginService, IUserStorageSessionService {

	private KeycloakRestRequester keycloakRestRequester = new KeycloakRestRequester();

	@Override
    @PUT
    @Path("login")
	public Response postLogin(InputStream creditianals) {

		Map<String, Object> requestObject = this.parseJson(creditianals);

		if (requestObject == null) {
			return Response.status(HttpServletResponse.SC_UNAUTHORIZED).build();
		}

		String username = (String)requestObject.get("username"); //$NON-NLS-1$
		String password = (String)requestObject.get("password"); //$NON-NLS-1$

		Credentials credentials;

		try {

			credentials = keycloakRestRequester.getToken(username, password);

		} catch(KeycloakAuthenticationException e) {
			return Response.status(HttpServletResponse.SC_UNAUTHORIZED).build();
		}

		return Response.ok().entity(credentials).type(MediaType.APPLICATION_JSON).build();

	}

	@Activate
	public void activate(ComponentContext context) throws IOException {
	}

	@Deactivate
	public void deactivate(ComponentContext context) {
		System.out.println("Login service stopped"); //$NON-NLS-1$
	}

	@Modified
	public void modify() {
		System.out.println("Login service modified"); //$NON-NLS-1$
	}

	private Map<String, Object> parseJson(InputStream json) {

		Map<String, Object> requestObject = null;

		try {
			requestObject = JSONUtil.parse(json, null);
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		return requestObject;
	}

	private StreamingOutput buildResponseStream(Map<String, Object> responseObject) {

		InputStream body = JSONUtil.build(responseObject);

		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {
				IOUtil.copy(body, os);
				os.flush();
				IOUtil.closeSilent(body);
			}
		};

		return stream;
	}

	@Override
	public boolean isAuth(String csrfToken, String keycloackToken) {
		try {
			Credentials credentials = keycloakRestRequester.validateToken(keycloackToken);
			return true;
		} catch(KeycloakAuthenticationException e) {
			return false;
		}
	}

	@Override
	public String getUserLogin(String keycloackToken) {
		try {
			Credentials credentials = keycloakRestRequester.validateToken(keycloackToken);
				return credentials.getUserId();
			} catch(KeycloakAuthenticationException e) {
				return null;
			}
	}

}
