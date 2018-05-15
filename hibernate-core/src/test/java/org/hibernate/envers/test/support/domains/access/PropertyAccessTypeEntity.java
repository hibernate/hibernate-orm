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

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
public class PropertyAccessTypeEntity {
	private Integer id;
	private String data;
	private boolean idSet;
	private boolean dataSet;

	public PropertyAccessTypeEntity() {

	}

	public PropertyAccessTypeEntity(Integer id, String data) {
		this( data );
		this.id = id;
	}

	public PropertyAccessTypeEntity(String data) {
		this.data = data;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
		this.idSet = true;
	}

	@Audited
	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
		this.dataSet = true;
	}

	@Transient
	public boolean isIdSet() {
		return idSet;
	}

	@Transient
	public boolean isDataSet() {
		return dataSet;
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
		PropertyAccessTypeEntity that = (PropertyAccessTypeEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data, that.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}

	@Override
	public String toString() {
		return "PropertyAccessTypeEntity{" +
				"id=" + id +
				", data='" + data + '\'' +
				", idSet=" + idSet +
				", dataSet=" + dataSet +
				'}';
	}
}
