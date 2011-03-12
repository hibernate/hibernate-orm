package org.hibernate.test.annotations.derivedidentities.e3.a;

import java.io.Serializable;

/**
 * @author Emmanuel Bernard
 */
public class DependentId implements Serializable {
	String name;	// matches name of @Id attribute
	EmployeeId emp; // matches name of @Id attribute and type of embedded id of Employee
}
