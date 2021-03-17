/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.manytoone.lazy;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedList;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * @author Chris Cranford
 */
@Entity
@Table(name = "address")
public class Address extends BaseDomainEntity {
    private static final long serialVersionUID = 7380477602657080463L;

    @Column(name = "name")
    private String name;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "id", cascade = CascadeType.ALL)
    Collection<AddressVersion> versions = new LinkedList<>();

    Address() {
    }

    Address(Instant when, String who, String name) {
        super( when, who );
        this.name = name;
    }

    public AddressVersion addInitialVersion(String description) {
        AddressVersion version = new AddressVersion( getCreatedAt(), getCreatedBy(), this, 0, description );
        versions.add( version );
        return version;
    }

    public String getName() {
        return name;
    }

    public Collection<AddressVersion> getVersions() {
        return versions;
    }
}