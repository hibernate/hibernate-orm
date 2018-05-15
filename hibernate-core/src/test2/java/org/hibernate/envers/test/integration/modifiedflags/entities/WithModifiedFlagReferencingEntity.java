/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.modifiedflags.entities;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "WithModFlagRefIng")
@Audited(withModifiedFlag = true)
public class WithModifiedFlagReferencingEntity {
	@Id
	private Integer id;

	private String data;

	@OneToOne
	private PartialModifiedFlagsEntity reference;

	@OneToOne
	private PartialModifiedFlagsEntity secondReference;

	public WithModifiedFlagReferencingEntity() {
	}

	public WithModifiedFlagReferencingEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public PartialModifiedFlagsEntity getReference() {
		return reference;
	}

	public void setReference(PartialModifiedFlagsEntity reference) {
		this.reference = reference;
	}

	public PartialModifiedFlagsEntity getSecondReference() {
		return secondReference;
	}

	public void setSecondReference(PartialModifiedFlagsEntity reference) {
		this.secondReference = reference;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof WithModifiedFlagReferencingEntity) ) {
			return false;
		}

		WithModifiedFlagReferencingEntity that = (WithModifiedFlagReferencingEntity) o;

		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}
}
