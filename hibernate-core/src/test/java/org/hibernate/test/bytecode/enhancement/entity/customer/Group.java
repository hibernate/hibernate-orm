/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.entity.customer;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@Entity
@Table(name = "GROUP")
@SequenceGenerator(name = "GROUP_SEQUENCE", sequenceName = "GROUP_SEQUENCE", allocationSize = 1, initialValue = 0)
public class Group {

    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GROUP_SEQUENCE")
    private int id;

    @Column
    private String name;

    @ManyToMany(mappedBy = "groups")
    private Set<User> users = new HashSet<User>();

    public Group() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public void removeUser(User user) {
        Set<User> set = this.users;
        set.remove(user);
        this.users = set;
    }
}