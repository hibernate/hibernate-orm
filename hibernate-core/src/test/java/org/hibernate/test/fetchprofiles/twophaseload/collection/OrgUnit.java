/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.fetchprofiles.twophaseload.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;

import static org.hibernate.test.fetchprofiles.twophaseload.collection.CollectionLoadedInTwoPhaseLoadTest.FETCH_PROFILE_NAME;

@Entity
@FetchProfile(name = FETCH_PROFILE_NAME, fetchOverrides = {
		@FetchProfile.FetchOverride(entity = OrgUnit.class, association = "people", mode = FetchMode.JOIN)
})
public class OrgUnit {

	@Id
	private String name;

	private String description;

	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "orgUnits", cascade = CascadeType.PERSIST)
	private List<Person> people = new ArrayList<>();

	public OrgUnit() {
	}

	public OrgUnit(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public Person findPerson(String personName) {
		if (people == null) {
			return null;
		}
		for ( Person person : people ) {
			if (person.getName().equals( personName )) return person;
		}
		return null;
	}

	public void addPerson(Person person) {
		if (people.contains( person )) return;
		people.add(person);
		person.addOrgUnit( this);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<Person> getPeople() {
		return people;
	}

	public void setPeople(List<Person> people) {
		this.people = people;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		OrgUnit group = (OrgUnit) o;
		return Objects.equals( name, group.name );
	}

	@Override
	public int hashCode() {
		return Objects.hash( name );
	}

	@Override
	public String toString() {
		return "OrgUnit{" +
				"name='" + name + '\'' +
				", description='" + description + '\'' +
				'}';
	}

}
