package org.hibernate.envers.test.integration.inheritance.joined.relation;

import javax.persistence.Entity;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class Person extends RightsSubject {
	private String name;

	public Person() {
	}

	public Person(Long id, String group, String name) {
		super( id, group );
		this.name = name;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof Person) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		Person person = (Person) o;

		if ( name != null ? !name.equals( person.name ) : person.name != null ) {
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
		return "Person(" + super.toString() + ", name = " + name + ")";
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
