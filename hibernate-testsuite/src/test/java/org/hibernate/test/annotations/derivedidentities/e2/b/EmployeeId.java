package org.hibernate.test.annotations.derivedidentities.e2.b;

import java.io.Serializable;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
public class EmployeeId implements Serializable {
	String firstName;
	String lastName;
}