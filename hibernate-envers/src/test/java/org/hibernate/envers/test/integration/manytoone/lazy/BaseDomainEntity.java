/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.manytoone.lazy;

import java.time.Instant;
import java.util.Objects;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * @author Chris Cranford
 */
@MappedSuperclass
public abstract class BaseDomainEntity extends BaseDomainEntityMetadata {
    private static final long serialVersionUID = 1023010094948580516L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected long id = 0;

    BaseDomainEntity() {

    }

    BaseDomainEntity(Instant timestamp, String who) {
        super( timestamp, who );
    }

    public long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }
        BaseDomainEntity that = (BaseDomainEntity) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash( id );
    }
}
