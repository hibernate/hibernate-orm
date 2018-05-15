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
import javax.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Table(name = "STR_TEST")
public class StrTestEntity {
	@Id
	@GeneratedValue
	private Integer id;
	@Audited
	private String str;

	public StrTestEntity() {

	}

	public StrTestEntity(Integer id, String str) {
		this( str );
		this.id = id;
	}

	public StrTestEntity(String str) {
		this.str = str;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		StrTestEntity that = (StrTestEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( str, that.str );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, str );
	}

	@Override
	public String toString() {
		return "StrTestEntity{" +
				"id=" + id +
				", str='" + str + '\'' +
				'}';
	}
}
