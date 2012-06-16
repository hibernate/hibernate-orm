package org.hibernate.test.annotations.collectionelement;
import javax.persistence.Embeddable;

@Embeddable
public class Person {

	String lastName;
	String firstName;

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
}
