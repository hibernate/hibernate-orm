package org.hibernate.test.annotations.derivedidentities.e2.a;

import java.io.Serializable;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

/**
 * @author Emmanuel Bernard
 */
public class DependentId implements Serializable {
	String name; // matches name of @Id attribute
	@Embedded
	EmployeeId emp; //matches name of attribute and type of Employee PK
}
