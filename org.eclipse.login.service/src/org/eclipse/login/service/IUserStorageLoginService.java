/**
 *
 */
package org.eclipse.login.service;

import java.io.InputStream;

import javax.ws.rs.core.Response;;

/**
 * @author Zagrebaev_D
 *
 */
public interface IUserStorageLoginService {

    public Response postLogin(InputStream creditianals);

    public Response postCreate(InputStream creditianals);
}
