/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.notinsertable;

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

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof NotInsertableTestEntity) ) {
			return false;
		}

		NotInsertableTestEntity that = (NotInsertableTestEntity) o;

		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
		if ( dataCopy != null ? !dataCopy.equals( that.dataCopy ) : that.dataCopy != null ) {
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
		result = 31 * result + (dataCopy != null ? dataCopy.hashCode() : 0);
		return result;
	}
}