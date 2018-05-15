/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.access;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.hibernate.annotations.AccessType;
import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
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

	public MixedAccessTypeEntity(Integer id, String data) {
		this( data );
		this.id = id;
	}

	public MixedAccessTypeEntity(String data) {
		this.data = data;
	}

	public Integer getId() {
		throw new RuntimeException( "This method should never be called" );
	}

	public void setId(Integer id) {
		throw new RuntimeException( "This method should never be called" );
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

	public Integer getIdSafe() {
		return this.id;
	}

	public void setDataSafe(String data) {
		this.data = data;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		MixedAccessTypeEntity that = (MixedAccessTypeEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data, that.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}

	@Override
	public String toString() {
		return "MixedAccessTypeEntity{" +
				"id=" + id +
				", data='" + data + '\'' +
				", dataSet=" + dataSet +
				'}';
	}
}
