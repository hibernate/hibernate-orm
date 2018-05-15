/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.basic;

import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class BasicPartialNotAuditedEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Basic
	private String data1;

	@Basic
	@NotAudited
	private String data2;

	public BasicPartialNotAuditedEntity() {

	}

	public BasicPartialNotAuditedEntity(String data1, String data2) {
		this.data1 = data1;
		this.data2 = data2;
	}

	public BasicPartialNotAuditedEntity(Integer id, String data1, String data2) {
		this( data1, data2 );
		this.id = id;
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

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		BasicPartialNotAuditedEntity that = (BasicPartialNotAuditedEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data1, that.data1 ) &&
				Objects.equals( data2, that.data2 );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data1, data2 );
	}

	@Override
	public String toString() {
		return "UnversionedEntity{" +
				"id=" + id +
				", data1='" + data1 + '\'' +
				", data2='" + data2 + '\'' +
				'}';
	}
}
