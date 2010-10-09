package org.hibernate.test.annotations.derivedidentities.e5.b;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(PersonId.class)
public class Person {
	@Id
	String firstName;
	@Id
	String lastName;
}
