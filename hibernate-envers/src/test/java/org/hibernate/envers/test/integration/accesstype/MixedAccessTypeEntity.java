/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.integration.accesstype;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class MixedAccessTypeEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Access(value = AccessType.PROPERTY)
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