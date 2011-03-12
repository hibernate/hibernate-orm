package org.hibernate.test.annotations.derivedidentities.e3.b;

import java.io.Serializable;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class EmployeeId implements Serializable {
	String firstName;
	String lastName;
}
