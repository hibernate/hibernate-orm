/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.manytoone.bidirectional;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

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

	@OneToMany(mappedBy = "reference")
	private List<BiRefingOptionalEntity> references = new ArrayList<>();

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public List<BiRefingOptionalEntity> getReferences() {
		return references;
	}

	public void setReferences(List<BiRefingOptionalEntity> references) {
		this.references = references;
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
