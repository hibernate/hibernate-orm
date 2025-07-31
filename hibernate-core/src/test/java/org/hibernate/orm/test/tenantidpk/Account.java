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
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
public class Account {

    @Id @GeneratedValue Long id;

    @Id @TenantId UUID tenantId;

    @ManyToOne(optional = false)
    Client client;

    public Account(Client client) {
        this.client = client;
    }

    Account() {}
}