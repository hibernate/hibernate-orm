package org.hibernate.test.annotations.derivedidentities.e1.a;

import java.io.Serializable;

/**
 * @author Emmanuel Bernard
 */
public class DependentId implements Serializable {
	String name;
	long emp;	// corresponds to PK type of Employee
}