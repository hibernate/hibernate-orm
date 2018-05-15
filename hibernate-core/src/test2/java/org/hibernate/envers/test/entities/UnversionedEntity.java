/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class UnversionedEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Basic
	private String data1;

	@Basic
	@NotAudited
	private String data2;

	public UnversionedEntity() {
	}

	public UnversionedEntity(String data1, String data2) {
		this.data1 = data1;
		this.data2 = data2;
	}

	public UnversionedEntity(Integer id, String data1, String data2) {
		this.id = id;
		this.data1 = data1;
		this.data2 = data2;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getData1() {
		return data1;
	}

	public void setData1(String data1) {
		this.data1 = data1;
	}

	public String getData2() {
		return data2;
	}

	public void setData2(String data2) {
		this.data2 = data2;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof UnversionedEntity) ) {
			return false;
		}

		UnversionedEntity that = (UnversionedEntity) o;

		if ( data1 != null ? !data1.equals( that.data1 ) : that.data1 != null ) {
			return false;
		}
		if ( data2 != null ? !data2.equals( that.data2 ) : that.data2 != null ) {
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
		result = 31 * result + (data1 != null ? data1.hashCode() : 0);
		result = 31 * result + (data2 != null ? data2.hashCode() : 0);
		return result;
	}
}
