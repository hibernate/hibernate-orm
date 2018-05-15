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

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
public class BasicAuditedEntity {
	@Id
	@GeneratedValue
	private Integer id;
	@Audited
	private String str1;
	@Audited
	private long long1;

	public BasicAuditedEntity() {

	}

	public BasicAuditedEntity(Integer id, String str1, long long1) {
		this( str1, long1 );
		this.id = id;
	}

	public BasicAuditedEntity(String str1, long long1) {
		this.str1 = str1;
		this.long1 = long1;
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

	public long getLong1() {
		return long1;
	}

	public void setLong1(long long1) {
		this.long1 = long1;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		BasicAuditedEntity that = (BasicAuditedEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( str1, that.str1 ) &&
				Objects.equals( long1, that.long1 );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, str1, long1 );
	}

	@Override
	public String toString() {
		return "BasicAuditedEntity{" +
				"id=" + id +
				", str1='" + str1 + '\'' +
				", long1=" + long1 +
				'}';
	}
}
