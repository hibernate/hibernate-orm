/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.collections.embeddable;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Immutable;

/**
 * @author Chris Cranford
 */
@Entity
@Immutable
public class Type {
	@Id
	private Integer id;
	private String name;

	Type() {

	}

	public Type(Integer id) {
		this( id, null );
	}

	public Type(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Type type = (Type) o;
		return Objects.equals( id, type.id ) &&
				Objects.equals( name, type.name );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, name );
	}
}
