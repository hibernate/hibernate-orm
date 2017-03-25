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
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToOne;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class BiRefingOptionalEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@OneToOne(optional = true)
	@JoinTable(name = "A_B", joinColumns = @JoinColumn(name = "a_id", unique = true), inverseJoinColumns = @JoinColumn(name = "b_id") )
	private BiRefedOptionalEntity reference;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public BiRefedOptionalEntity getReference() {
		return reference;
	}

	public void setReference(BiRefedOptionalEntity reference) {
		this.reference = reference;
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
		if ( !( object instanceof BiRefingOptionalEntity ) ) {
			return false;
		}
		BiRefingOptionalEntity that = (BiRefingOptionalEntity) object;
		return !( id != null ? !id.equals( that.id ) : that.id != null );
	}
}
