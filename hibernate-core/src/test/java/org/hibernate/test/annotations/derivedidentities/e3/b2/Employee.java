package org.hibernate.test.annotations.derivedidentities.e3.b2;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;


@Entity
public class Employee {
	@EmbeddedId
	EmployeeId empId;
}
