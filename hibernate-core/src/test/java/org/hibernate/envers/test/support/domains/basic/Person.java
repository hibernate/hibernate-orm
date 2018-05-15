/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.basic;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class Person {
	@Id
	@GeneratedValue
	private Integer id;

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "person_id")
	@AuditMappedBy(mappedBy = "person")
	private Set<Name> names = new HashSet<>();

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<Name> getNames() {
		return names;
	}

	public void setNames(Set<Name> names) {
		this.names = names;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Person person = (Person) o;
		return Objects.equals( id, person.id ) &&
				Objects.equals( names, person.names );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, names );
	}

	@Override
	public String toString() {
		return "Person{" +
				"id=" + id +
				", names=" + names +
				'}';
	}
}
