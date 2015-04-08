package org.hibernate.test.annotations.derivedidentities.e3.b3;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;


@Entity
public class Employee {
	@EmbeddedId
	EmployeeId empId;
}
