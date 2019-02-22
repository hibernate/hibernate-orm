/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.notinsertable;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class NotInsertableTestEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Column(name = "data")
	private String data;

	@Column(name = "data", insertable = false, updatable = false)
	private String dataCopy;

	public NotInsertableTestEntity() {
	}

	public NotInsertableTestEntity(Integer id, String data, String dataCopy) {
		this.id = id;
		this.data = data;
		this.dataCopy = dataCopy;
	}

	public NotInsertableTestEntity(String data) {
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

	public String getDataCopy() {
		return dataCopy;
	}

	public void setDataCopy(String dataCopy) {
		this.dataCopy = dataCopy;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		NotInsertableTestEntity that = (NotInsertableTestEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data, that.data ) &&
				Objects.equals( dataCopy, that.dataCopy );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data, dataCopy );
	}
}