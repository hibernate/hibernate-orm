/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.onetoone.bidirectional;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class BiRefedOptionalEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@OneToOne(mappedBy = "reference", optional = true)
	private BiRefingOptionalEntity referencing;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public BiRefingOptionalEntity getReferencing() {
		return referencing;
	}

	public void setReferencing(BiRefingOptionalEntity referencing) {
		this.referencing = referencing;
	}

	@Override
	public int hashCode() {
		return ( id != null ? id.hashCode() : 0 );
	}

	@Override
	public boolean equals(Object object) {
		if ( object == this ) {
			return true;
		}
		if ( !( object instanceof BiRefedOptionalEntity ) ) {
			return false;
		}
		BiRefedOptionalEntity that = (BiRefedOptionalEntity) object;
		return !( id != null ? !id.equals( that.id ) : that.id != null );
	}
}
