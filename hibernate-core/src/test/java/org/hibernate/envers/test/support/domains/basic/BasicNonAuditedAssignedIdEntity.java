/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.basic;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Chris Cranford
 */
@Entity
public class BasicNonAuditedAssignedIdEntity {
	@Id
	private Integer id;
	private String str1;

	public BasicNonAuditedAssignedIdEntity() {

	}

	public BasicNonAuditedAssignedIdEntity(Integer id, String str1) {
		this.str1 = str1;
		this.id = id;
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

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		BasicNonAuditedAssignedIdEntity that = (BasicNonAuditedAssignedIdEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( str1, that.str1 );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, str1 );
	}

	@Override
	public String toString() {
		return "BasicNonAuditedAssignedIdEntity{" +
				"id=" + id +
				", str1='" + str1 + '\'' +
				'}';
	}
}
