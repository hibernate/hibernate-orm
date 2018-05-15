/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.inheritance.joined.relation;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Table(name = "APP_ROLE")
@Audited
public class Role extends RightsSubject {
	private String name;

	@ManyToMany
	private Set<RightsSubject> members = new HashSet<RightsSubject>();

	public Role() {
	}

	public Role(Long id, String group, String name) {
		super( id, group );
		this.name = name;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof Role) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		Role role = (Role) o;

		if ( name != null ? !name.equals( role.name ) : role.name != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (name != null ? name.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Role(" + super.toString() + ", name = " + name + ")";
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<RightsSubject> getMembers() {
		return members;
	}

	public void setMembers(Set<RightsSubject> members) {
		this.members = members;
	}
}
