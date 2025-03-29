/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.collection;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class EnumSetEntity {
	public static enum E1 {X, Y}

	public static enum E2 {A, B}

	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	@ElementCollection
	@Enumerated(EnumType.STRING)
	private Set<E1> enums1;

	@Audited
	@ElementCollection
	@Enumerated(EnumType.ORDINAL)
	private Set<E2> enums2;

	public EnumSetEntity() {
		enums1 = new HashSet<E1>();
		enums2 = new HashSet<E2>();
	}

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

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof EnumSetEntity) ) {
			return false;
		}

		EnumSetEntity that = (EnumSetEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return (id != null ? id.hashCode() : 0);
	}

	public String toString() {
		return "ESE(id = " + id + ", enums1 = " + enums1 + ", enums2 = " + enums2 + ")";
	}
}
