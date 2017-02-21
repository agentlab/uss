/**
 *
 */
package org.eclipse.userstorage.login.session;

import java.util.UUID;

import org.eclipse.userstorage.login.user.User;

/**
 * @author Zagrebaev_D
 *
 */
public final class Session {
    private final String id;

    private final String csrfToken;

    private final User user;

    public Session(User user) {
        id = UUID.randomUUID().toString();
        csrfToken = UUID.randomUUID().toString();
        this.user = user;
    }

    public String getID() {
        return id;
    }

    public String getCSRFToken() {
        return csrfToken;
    }

    public User getUser() {
        return user;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
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

        Session other = (Session)obj;
        if (id == null)
        {
            if (other.id != null)
            {
                return false;
            }
        }
        else if (!id.equals(other.id))
        {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return id + " -> " + user;
    }
}
