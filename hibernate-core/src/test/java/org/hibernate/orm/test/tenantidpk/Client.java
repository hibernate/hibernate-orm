/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tenantidpk;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.TenantId;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
public class Client {
    @Id
    @GeneratedValue
    Long id;

    String name;

    @Id @TenantId
    UUID tenantId;

    @OneToMany(mappedBy = "client")
    Set<Account> accounts = new HashSet<>();

    public Client(String name) {
        this.name = name;
    }

    Client() {}
}

