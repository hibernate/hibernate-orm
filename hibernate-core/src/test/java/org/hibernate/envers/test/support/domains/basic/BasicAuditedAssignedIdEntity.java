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

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
public class BasicAuditedAssignedIdEntity {
	@Id
	private Integer id;
	@Audited
	private String str1;

	public BasicAuditedAssignedIdEntity() {

	}

	public BasicAuditedAssignedIdEntity(Integer id, String str1) {
		this.id = id;
		this.str1 = str1;
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
		BasicAuditedAssignedIdEntity that = (BasicAuditedAssignedIdEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( str1, that.str1 );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, str1 );
	}

	@Override
	public String toString() {
		return "BasicAuditedAssignedIdEntity{" +
				"id=" + id +
				", str1='" + str1 + '\'' +
				'}';
	}
}
