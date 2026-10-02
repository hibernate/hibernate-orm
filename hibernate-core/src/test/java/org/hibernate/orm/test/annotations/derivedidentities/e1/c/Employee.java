package org.hibernate.orm.test.annotations.derivedidentities.e1.c;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Employee {
	@Id
	long empId;
	String empName;
}
