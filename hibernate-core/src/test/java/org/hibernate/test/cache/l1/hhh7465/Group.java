package org.hibernate.test.cache.l1.hhh7465;

import java.io.Serializable;
import java.util.Set;
import javax.persistence.*;

@Entity
@Table(name="groups")
public class Group implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private User user;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Group)) {
            return false;
        }
        final Group other = (Group) obj;
        if (this.id == null || other.id == null) {
            return this == obj;
        }
        if(!this.id.equals(other.id)) {
            return this == obj;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.clevercure.web.hibernateissuecache.Group[ id=" + id + " ]";
    }
}
