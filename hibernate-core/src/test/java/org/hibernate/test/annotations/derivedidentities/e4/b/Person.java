package org.hibernate.test.annotations.derivedidentities.e4.b;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Person {
	@Id
	String ssn;

	public Person() {
	}

	public Person(String ssn) {
		this.ssn = ssn;
	}
}
