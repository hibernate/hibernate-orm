/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.manytoone.lazy;

import java.time.Instant;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author Chris Cranford
 */
@Entity
@Table(name = "address_version")
public class AddressVersion extends BaseDomainEntityVersion {
    private static final long serialVersionUID = 1100389518057335117L;

    @Id
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id", referencedColumnName = "id", updatable = false, nullable = false)
    private Address id;

    @Column(name = "description", updatable = false)
    private String description;

    AddressVersion() {
    }

    AddressVersion(Instant when, String who, Address id, long version, String description) {
        setCreatedAt( when );
        setCreatedBy( who );
        setVersion( version );
        this.id = Objects.requireNonNull(id );
        this.description = description;
    }

    @Override
    public Address getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public AddressVersion update(Instant when, String who, String description) {
        AddressVersion version = new AddressVersion( when, who, id, getVersion() + 1, description );
        id.versions.add( version );
        return version;
    }
}