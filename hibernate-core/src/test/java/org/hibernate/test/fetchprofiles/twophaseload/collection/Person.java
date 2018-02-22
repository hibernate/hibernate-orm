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

import static org.hibernate.test.fetchprofiles.twophaseload.collection.CollectionLoadedInTwoPhaseLoadTest.FETCH_PROFILE_NAME_2;

@Entity
@FetchProfile(name = FETCH_PROFILE_NAME_2, fetchOverrides = {
		@FetchProfile.FetchOverride(entity = Person.class, association = "orgUnits", mode = FetchMode.JOIN)
})
public class Person {

	@Id
	private String name;

	private String email;

	@ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
	private List<OrgUnit> orgUnits = new ArrayList<>();

	public Person() {
	}

	public Person(String name, String email) {
		this.name = name;
		this.email = email;
	}

	public OrgUnit findOrgUnit(String orgUnitName) {
		if ( orgUnits == null) {
			return null;
		}
		for ( OrgUnit orgUnit : orgUnits ) {
			if (orgUnit.getName().equals( orgUnitName )) return orgUnit;
		}
		return null;
	}

	public void addOrgUnit(OrgUnit orgUnit) {
		if ( orgUnits.contains( orgUnit)) return;
		orgUnits.add( orgUnit);
		orgUnit.addPerson(this);
	}

	public List<OrgUnit> getOrgUnits() {
		return orgUnits;
	}

	public void setOrgUnits(List<OrgUnit> orgUnits) {
		this.orgUnits = orgUnits;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
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
		return Objects.equals( name, person.name );
	}

	@Override
	public int hashCode() {
		return Objects.hash( name );
	}

	@Override
	public String toString() {
		return "Person{" +
				"name='" + name + '\'' +
				", email='" + email + '\'' +
				'}';
	}

}
