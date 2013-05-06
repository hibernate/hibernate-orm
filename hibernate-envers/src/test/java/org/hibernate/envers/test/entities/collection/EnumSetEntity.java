/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.entities.collection;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.HashSet;
import java.util.Set;

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