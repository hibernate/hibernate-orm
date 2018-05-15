/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.onetomany;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.envers.Audited;

/**
 * ReferencIng entity
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class SetRefIngEntity {
	@Id
	private Integer id;

	@Audited
	private String data;

	@Audited
	@ManyToOne
	private SetRefEdEntity reference;

	public SetRefIngEntity() {
	}

	public SetRefIngEntity(Integer id, String data, SetRefEdEntity reference) {
		this.id = id;
		this.data = data;
		this.reference = reference;
	}

	public SetRefIngEntity(String data, SetRefEdEntity reference) {
		this.data = data;
		this.reference = reference;
	}

	public SetRefIngEntity(Integer id, String data) {
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

	public SetRefEdEntity getReference() {
		return reference;
	}

	public void setReference(SetRefEdEntity reference) {
		this.reference = reference;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof SetRefIngEntity) ) {
			return false;
		}

		SetRefIngEntity that = (SetRefIngEntity) o;

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

	public String toString() {
		return "SetRefIngEntity(id = " + id + ", data = " + data + ")";
	}
}
