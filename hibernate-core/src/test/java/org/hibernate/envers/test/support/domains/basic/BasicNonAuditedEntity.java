/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.basic;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Chris Cranford
 */
@Entity
public class BasicNonAuditedEntity {
	@Id
	@GeneratedValue
	private Integer id;
	private String str1;
	private String str2;

	public BasicNonAuditedEntity() {

	}

	public BasicNonAuditedEntity(Integer id, String str1, String str2) {
		this( str1, str2 );
		this.id = id;
	}

	public BasicNonAuditedEntity(String str1, String str2) {
		this.str1 = str1;
		this.str2 = str2;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getStr1() {
		return str1;
	}

	public void setStr1(String str1) {
		this.str1 = str1;
	}

	public String getStr2() {
		return str2;
	}

	public void setStr2(String str2) {
		this.str2 = str2;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		BasicNonAuditedEntity that = (BasicNonAuditedEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( str1, that.str1 ) &&
				Objects.equals( str2, that.str2 );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, str1, str2 );
	}

	@Override
	public String toString() {
		return "BasicNonAuditedEntity{" +
				"id=" + id +
				", str1='" + str1 + '\'' +
				", str2='" + str2 + '\'' +
				'}';
	}
}
