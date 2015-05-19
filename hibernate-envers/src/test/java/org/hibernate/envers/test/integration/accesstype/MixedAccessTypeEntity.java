/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.accesstype;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.hibernate.annotations.AccessType;
import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class MixedAccessTypeEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@AccessType("property")
	private String data;

	@Transient
	private boolean dataSet;

	public MixedAccessTypeEntity() {
	}

	public MixedAccessTypeEntity(String data) {
		this.data = data;
	}

	public MixedAccessTypeEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public Integer getId() {
		throw new RuntimeException();
	}

	public void setId(Integer id) {
		throw new RuntimeException();
	}

	// TODO: this should be on the property. But how to discover in AnnotationsMetadataReader that the
	// we should read annotations from fields, even though the access type is "property"?
	@Audited
	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
		dataSet = true;
	}

	public boolean isDataSet() {
		return dataSet;
	}

	public Integer readId() {
		return id;
	}

	public void writeData(String data) {
		this.data = data;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof MixedAccessTypeEntity) ) {
			return false;
		}

		MixedAccessTypeEntity that = (MixedAccessTypeEntity) o;

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