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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.eclipse.userstorage.internal.util.IOUtil;
import org.eclipse.userstorage.internal.util.JSONUtil;
import org.eclipse.userstorage.internal.util.StringUtil;
import org.eclipse.userstorage.service.IApiBlobService;
import org.eclipse.userstorage.service.host.utils.ServiceUtils;
import org.eclipse.userstorage.session.service.IUserStorageSessionService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com._1c.cloud.edt.workspace.setup.security.keycloak.KeycloakRestConstants;

/**
 * @author admin
 *
 */

@Component(property= {
	"service.exported.interfaces:String=*"
	, "service.exported.configs:String=ecf.jaxrs.jersey.server"
	, "ecf.jaxrs.jersey.server.uri:String=http://localhost:8080/api/blob"
	, "ecf.jaxrs.jersey.server.exported.interfaces=org.eclipse.userstorage.service.IApiBlobService"}
	, immediate=true)
@Path("/")
public class UserStorageComponent implements ManagedService, IApiBlobService {

	private String id;
	private String database;
	private String user;
	private String password;
	private String create;

	private final Pattern uuidPatern = Pattern.compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");

	private final Set<String> applicationTokens = new HashSet<>();

	private final File applicationFolder = new File(System.getProperty("java.io.tmpdir"), "uss-server"); //$NON-NLS-1$ //$NON-NLS-2$
	private final static String userApp = "eclipse_test_123456789"; //$NON-NLS-1$

	private IUserStorageSessionService ussSessionsService;

	@Override
	@PUT
	@Path("{token}/{filename}")
	public Response put(@PathParam("token") String urltoken, @PathParam("filename") String urlfilename,
		InputStream blob, @HeaderParam("If-Match") String headerIfMatch,
		@HeaderParam("X-CSRF-Token") String headerxCsrfToken, @HeaderParam(KeycloakRestConstants.AUTH_HEADER) String authorization) throws IOException {

		if (authorization == null || !this.isAutorized(headerxCsrfToken, authorization)) {
			return Response.status(HttpServletResponse.SC_UNAUTHORIZED).build();
		}

		if (!this.isExistAppToken(urltoken)) {
			return Response.status(HttpServletResponse.SC_NOT_FOUND).build();
		}

		File etagFile = getUserFile(this.getUserFolder(authorization), urltoken, urlfilename, ServiceUtils.ETAG_EXTENSION);

		headerIfMatch = getEtag(headerIfMatch);

		if (etagFile.exists()) {
			String etag = IOUtil.readUTF(etagFile);

			if (StringUtil.isEmpty(headerIfMatch) || !headerIfMatch.equals(etag)) {
				return Response.status(HttpServletResponse.SC_CONFLICT).header("Etag", createEtagHeaderValue(etag)).build(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}

		String etag = UUID.randomUUID().toString();

		File blobFile = getUserFile(this.getUserFolder(authorization), urltoken, urlfilename, ServiceUtils.BLOB_EXTENSION);
		IOUtil.mkdirs(blobFile.getParentFile());

		Map<String, InputStream> value = null;

		try {
			value = JSONUtil.parse(blob, "value"); //$NON-NLS-1$
		}
		catch (IOException e) {
			return Response.status(HttpServletResponse.SC_EXPECTATION_FAILED).build();
		}

		FileOutputStream out = new FileOutputStream(blobFile);

		try {
			IOUtil.copy(value.get("value"), out); //$NON-NLS-1$
		}
		finally {
			IOUtil.closeSilent(value.get("value")); //$NON-NLS-1$
			IOUtil.close(out);
		}

		IOUtil.writeUTF(etagFile, etag);

		return Response.status(etagFile.exists() ? HttpServletResponse.SC_OK : HttpServletResponse.SC_CREATED).header("Etag", createEtagHeaderValue(etag)).build(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	}

	@Override
	@DELETE
	@Path("{token}/{filename}")
	public Response delete(@PathParam("token") String token, @PathParam("filename") String filename,
		@HeaderParam("If-Match") String headerIfMatch, @HeaderParam("X-CSRF-Token") String headerxCsrfToken,
		@HeaderParam(KeycloakRestConstants.AUTH_HEADER) String authorization) {

		if (authorization == null || !this.isAutorized(headerxCsrfToken, authorization)) {
			return Response.status(HttpServletResponse.SC_UNAUTHORIZED).build();
		}

		File etagFile = getUserFile(this.getUserFolder(authorization), token, filename, ServiceUtils.ETAG_EXTENSION);

		if (!this.isExistAppToken(token) || !etagFile.exists()) {
			return Response.status(HttpServletResponse.SC_NOT_FOUND).build();
		}

		String etag = IOUtil.readUTF(etagFile);
		String ifMatch = getEtag(headerIfMatch/*, "If-Match"*/);
		if (ifMatch != null && !ifMatch.equals(etag)) {
			return Response.status(494).build();
		}

		File blobFile = getUserFile(this.getUserFolder(authorization), token, filename, ServiceUtils.BLOB_EXTENSION);

		if (blobFile.exists()) {
			IOUtil.delete(blobFile);
		}
		IOUtil.delete(etagFile);

		return Response.noContent().build();
	}

	@Override
	@GET
	@Path("{token}/{filename}")
	public Response get(@PathParam("token") String urltoken, @PathParam("filename") String urlfilename,
		@HeaderParam("If-None-Match") String headerIfNoneMatch, @QueryParam("pageSize") String queryPageSize,
		@QueryParam("page") String queryPage, @HeaderParam("X-CSRF-Token") String headerxCsrfToken,
		@HeaderParam(KeycloakRestConstants.AUTH_HEADER) String authorization) throws IOException {

		if (authorization == null ||!this.isAutorized(headerxCsrfToken, authorization)) {
			return Response.status(HttpServletResponse.SC_UNAUTHORIZED).build();
		}

		File etagFile = getUserFile(this.getUserFolder(authorization), urltoken, urlfilename, ServiceUtils.ETAG_EXTENSION);

		if (!this.isExistAppToken(urltoken) || !etagFile.exists()) {
			return Response.status(HttpServletResponse.SC_NOT_FOUND).build();
		}

		String etag = IOUtil.readUTF(etagFile);

		headerIfNoneMatch = getEtag(headerIfNoneMatch);

		if (headerIfNoneMatch != null && headerIfNoneMatch.equals(etag)) {
			return Response.status(HttpServletResponse.SC_NOT_MODIFIED).build();
		}

		if (urlfilename == null) {
			File applicationFolder = getApplicationFolder(user, urltoken);
			return retrieveProperties(applicationFolder, queryPageSize, queryPage);
		}

		File blobFile = getUserFile(this.getUserFolder(authorization), urltoken, urlfilename, ServiceUtils.BLOB_EXTENSION);

		FileInputStream blobInputStream = new FileInputStream(blobFile);

		InputStream body = JSONUtil.build(Collections.singletonMap("value", blobInputStream)); //$NON-NLS-1$

		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {
				IOUtil.copy(body, os);
				os.flush();
				IOUtil.closeSilent(body);
				blobInputStream.close();
			}
		};

		return Response.ok().header("Etag", createEtagHeaderValue(etag)).entity(stream).type(MediaType.APPLICATION_JSON).build(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private String createEtagHeaderValue(String etag) {

		int initCapacity = 128;
		StringBuilder etagHeaderBuilder = new StringBuilder(initCapacity);

		etagHeaderBuilder.append('"');
		etagHeaderBuilder.append(etag);
		etagHeaderBuilder.append('"');

		return etagHeaderBuilder.toString();
	}

	protected Response retrieveProperties(File applicationFolder, String queryPageSize, String queryPage) {

		String applicationToken = applicationFolder.getName();

		int pageSize = getIntParameter(queryPageSize, 20);
		if (pageSize < 1 || pageSize > 100) {
			return Response.status(HttpServletResponse.SC_BAD_REQUEST).build();
		}

		int page = getIntParameter(queryPage, 20);
		if (page < 1) {
			return Response.status(HttpServletResponse.SC_BAD_REQUEST).build();
		}

		boolean empty = true;

		StringBuilder builder = new StringBuilder();
		builder.append('[');

		File[] files = applicationFolder.listFiles();
		if (files != null) {
			int first = (page - 1) * pageSize + 1;
			System.out.println("##### " + first); //$NON-NLS-1$
			int i = 0;

			for (File file : files) {
				String name = file.getName();
				if (name.endsWith(ServiceUtils.ETAG_EXTENSION)) {
					if (++i >= first) {
						String key = name.substring(0, name.length() - ServiceUtils.ETAG_EXTENSION.length());
						System.out.println("##### " + key); //$NON-NLS-1$
						String etag = IOUtil.readUTF(file);

						if (empty) {
							empty = false;
						}
						else {
							builder.append(","); //$NON-NLS-1$
						}

						builder.append("{\"application_token\":\""); //$NON-NLS-1$
						builder.append(applicationToken);
						builder.append("\",\"key\":\""); //$NON-NLS-1$
						builder.append(key);
						builder.append("\",\"etag\":\""); //$NON-NLS-1$
						builder.append(etag);
						builder.append("\"}"); //$NON-NLS-1$

						if (--pageSize == 0) {
							break;
						}
					}
				}
			}
		}

		builder.append(']');
		System.out.println(builder);

		InputStream body = IOUtil.streamUTF(builder.toString());

		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {
				IOUtil.copy(body, os);
				os.flush();
				IOUtil.closeSilent(body);
			}
		};

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
		if (header != null) {
			Matcher matcher = uuidPatern.matcher(header);
			if (matcher.find()) {
				return matcher.group(0);
			}
		}
		return null;
	}

	private int getIntParameter(String queryParam, int defValue) {
		if (queryParam != null) {
			try {
				return Integer.parseInt(queryParam);
			}
			catch (NumberFormatException ex) {
				return defValue;
			}
		}

		return defValue;
	}

	private boolean isAutorized(String csrfToken, String keycloackToken) {
		keycloackToken = keycloackToken.replace(KeycloakRestConstants.BEARER, "");
		return this.ussSessionsService.isAuth(csrfToken, keycloackToken);
	}

	private String getUserFolder(String sessionID) {
		return this.ussSessionsService.getUserLogin(sessionID);
	}

	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		// TODO Auto-generated method stub

	}

	@Activate
	public void activate(ComponentContext context) throws IOException {
		Dictionary<String, Object> properties = context.getProperties();
		id = (String)properties.get("database.id"); //$NON-NLS-1$
		database = (String)properties.get("database"); //$NON-NLS-1$
		user = (String)properties.get("user"); //$NON-NLS-1$
		password = (String)properties.get("password"); //$NON-NLS-1$
		create = (String)properties.get("create"); //$NON-NLS-1$
		this.getApplicationTokens().add("pDKTqBfDuNxlAKydhEwxBZPxa4q"); //$NON-NLS-1$
		this.getApplicationTokens().add("cNhDr0INs8T109P8h6E1r_GvU3I"); //$NON-NLS-1$
		System.out.println(applicationFolder);
		System.err.println("USS service started"); //$NON-NLS-1$

	}

	@Deactivate
	public void deactivate(ComponentContext context) {
		System.out.println("USS service stopped"); //$NON-NLS-1$
	}

	@Modified
	public void modify() {
		System.out.println("USS service modified"); //$NON-NLS-1$
	}

	@Reference
	public synchronized void bindSessionService(IUserStorageSessionService ussSessionsService) {
		this.ussSessionsService = ussSessionsService;
	}

	public synchronized void unbindSessionService(IUserStorageSessionService ussSessionsService) {
		if (this.ussSessionsService == ussSessionsService) {
			this.ussSessionsService = null;
		}

	}

}
