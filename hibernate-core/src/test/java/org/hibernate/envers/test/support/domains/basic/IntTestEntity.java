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
 * @author Chris Cranford
 */
@Entity
public class IntTestEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	@Column(name = "NUMERIC_VALUE")
	private Integer number;

	public IntTestEntity() {

	}

	public IntTestEntity(Integer number) {
		this.number = number;
	}

	public IntTestEntity(Integer id, Integer number) {
		this( number );
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
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
		IntTestEntity that = (IntTestEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( number, that.number );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, number );
	}

	@Override
	public String toString() {
		return "IntTestEntity{" +
				"id=" + id +
				", number=" + number +
				'}';
	}
}
