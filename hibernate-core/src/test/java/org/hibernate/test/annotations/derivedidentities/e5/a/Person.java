package org.hibernate.test.annotations.derivedidentities.e5.a;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(PersonId.class)
public class Person {
	@Id String firstName;
	@Id String lastName;

	public Person() {
	}

	public Person(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}
}
