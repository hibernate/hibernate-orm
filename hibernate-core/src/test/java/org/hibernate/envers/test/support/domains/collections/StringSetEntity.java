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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class StringSetEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	@ElementCollection
	private Set<String> strings;

	public StringSetEntity() {
		strings = new HashSet<>();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<String> getStrings() {
		return strings;
	}

	public void setStrings(Set<String> strings) {
		this.strings = strings;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		StringSetEntity that = (StringSetEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( strings, that.strings );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, strings );
	}

	@Override
	public String toString() {
		return "StringSetEntity{" +
				"id=" + id +
				", strings=" + strings +
				'}';
	}
}
