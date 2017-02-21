/**
 *
 */
package org.eclipse.userstorage.session.service;

/**
 * @author Zagrebaev_D
 *
 */
public interface IUserStorageSessionService {

    boolean isAuth(String csrfToken, String sessionID);

    String getUserLogin(String sessionID);
}
