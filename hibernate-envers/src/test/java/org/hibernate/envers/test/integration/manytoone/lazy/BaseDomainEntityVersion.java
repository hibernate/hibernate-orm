/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.manytoone.lazy;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * @author Chris Cranford
 */
@MappedSuperclass
public abstract class BaseDomainEntityVersion extends BaseDomainEntityMetadata {
    private static final long serialVersionUID = 1564895954324242368L;

    @Id
    @Column(name = "version", nullable = false, updatable = false)
    private long version;

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public abstract Object getId();

    @Override
    public boolean equals(Object o) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }
        BaseDomainEntityVersion that = (BaseDomainEntityVersion) o;
        return Objects.equals( getId(), that.getId() ) && version == that.version;
    }

    @Override
    public int hashCode() {
        return Objects.hash( getId(), version );
    }
}