package org.hibernate.test.annotations.derivedidentities.e2.a;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(EmployeeId.class)
public class Employee {
	@Id String firstName;
	@Id String lastName;
}
