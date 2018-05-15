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

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
public class FieldAccessTypeEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	private String data;

	public FieldAccessTypeEntity() {

	}

	public FieldAccessTypeEntity(Integer id, String data) {
		this( data );
		this.id = id;
	}

	public FieldAccessTypeEntity(String data) {
		this.data = data;
	}

	public Integer getId() {
		throw new RuntimeException( "This method should never be called." );
	}

	public void setId(Integer id) {
		throw new RuntimeException( "This method should never be called." );
	}

	public String getData() {
		throw new RuntimeException( "This method should never be called." );
	}

	public void setData(String data) {
		throw new RuntimeException( "This method should never be called." );
	}

	public Integer getIdSafe() {
		return id;
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
		FieldAccessTypeEntity that = (FieldAccessTypeEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data, that.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}

	@Override
	public String toString() {
		return "FieldAccessTypeEntity{" +
				"id=" + id +
				", data='" + data + '\'' +
				'}';
	}
}
