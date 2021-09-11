/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.tenantid;

import org.hibernate.annotations.TenantId;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Client {
    @Id
    @GeneratedValue
    Long id;

    String name;

    @TenantId
    String tenantId;

    @OneToMany(mappedBy = "client")
    Set<Account> accounts = new HashSet<>();

    public Client(String name) {
        this.name = name;
    }

    Client() {}
}
