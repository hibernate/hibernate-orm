/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.reference;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;

@Entity
@Audited
public class GreetingSetPO {
	@Id
	@GeneratedValue
	private Long id;

	private String name;

	@OneToMany(mappedBy = "greetingSet")
	private Set<GreetingPO> greetings;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<GreetingPO> getGreetings() {
		return greetings;
	}

	public void setGreetings(Set<GreetingPO> greetings) {
		this.greetings = greetings;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof GreetingSetPO) ) {
			return false;
		}

		GreetingSetPO that = (GreetingSetPO) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( name != null ? !name.equals( that.name ) : that.name != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (name != null ? name.hashCode() : 0);
		return result;
	}
}