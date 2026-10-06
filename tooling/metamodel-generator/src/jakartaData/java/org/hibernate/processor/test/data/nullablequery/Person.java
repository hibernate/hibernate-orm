package org.hibernate.processor.test.data.nullablequery;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Person {
	@Id
	Long id;
	String firstName;
	String lastName;
	String[] aliases;
}
