/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.collections;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class EnumSetEntity {
	public enum E1 {
		X,
		Y
	}

	public enum E2 {
		A,
		B
	}

	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	@ElementCollection
	@Enumerated(EnumType.STRING)
	private Set<E1> enums1 = new HashSet<>();

	@Audited
	@ElementCollection
	@Enumerated(EnumType.ORDINAL)
	private Set<E2> enums2 = new HashSet<>();

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<E1> getEnums1() {
		return enums1;
	}

	public void setEnums1(Set<E1> enums1) {
		this.enums1 = enums1;
	}

	public Set<E2> getEnums2() {
		return enums2;
	}

	public void setEnums2(Set<E2> enums2) {
		this.enums2 = enums2;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		EnumSetEntity that = (EnumSetEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( enums1, that.enums1 ) &&
				Objects.equals( enums2, that.enums2 );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, enums1, enums2 );
	}

	@Override
	public String toString() {
		return "EnumSetEntity{" +
				"id=" + id +
				", enums1=" + enums1 +
				", enums2=" + enums2 +
				'}';
	}
}
