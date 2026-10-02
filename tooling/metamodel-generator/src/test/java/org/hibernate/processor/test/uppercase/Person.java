package org.hibernate.processor.test.uppercase;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Person {
	@Id String SSN;
	String UserID;
	String fullName;
	int X;
	int y;
}
