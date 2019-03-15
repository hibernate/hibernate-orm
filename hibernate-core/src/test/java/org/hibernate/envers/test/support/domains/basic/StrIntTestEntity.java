/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.basic;

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
public class StrIntTestEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	private String str1;

	@Audited
	@Column(name = "NUM_VAL")
	private Integer number;

	public StrIntTestEntity() {
	}

	public StrIntTestEntity(String str1, Integer number, Integer id) {
		this.id = id;
		this.str1 = str1;
		this.number = number;
	}

	public StrIntTestEntity(String str1, Integer number) {
		this.str1 = str1;
		this.number = number;
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

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		StrIntTestEntity that = (StrIntTestEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( str1, that.str1 ) &&
				Objects.equals( number, that.number );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, str1, number );
	}

	@Override
	public String toString() {
		return "StrIntTestEntity{" +
				"id=" + id +
				", str1='" + str1 + '\'' +
				", number=" + number +
				'}';
	}
}