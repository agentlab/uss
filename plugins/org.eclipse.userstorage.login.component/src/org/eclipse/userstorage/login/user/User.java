/**
 *
 */
package org.eclipse.userstorage.login.user;

import org.eclipse.userstorage.internal.util.StringUtil;

/**
 * @author Zagrebaev_D
 *
 */
public final class User {
    private final String username;

    private final byte[] password;

    public User(String username, String password) {
        this.username = username;
        this.password = StringUtil.encrypt(password);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return StringUtil.decrypt(password);
    }

    @Override
    public String toString() {
        return username;
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
        {
            return true;
        }

        if (obj == null)
        {
            return false;
        }

        if (getClass() != obj.getClass())
        {
            return false;
        }

        User other = (User)obj;
        if (username == null)
        {
            if (other.username != null)
            {
                return false;
            }
        }
        else if (!username.equals(other.username))
        {
            return false;
        }

        return true;
    }
}
