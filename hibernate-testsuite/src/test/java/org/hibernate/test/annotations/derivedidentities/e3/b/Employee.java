package org.hibernate.test.annotations.derivedidentities.e3.b;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;


/**
 * @author Emmanuel Bernard
 */
@Entity
public class Employee {
	@EmbeddedId
	EmployeeId empId;
}
