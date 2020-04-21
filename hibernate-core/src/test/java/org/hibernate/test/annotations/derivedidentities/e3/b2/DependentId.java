package org.hibernate.test.annotations.derivedidentities.e3.b2;

import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class DependentId implements Serializable {
	String name;
	EmployeeId empPK;
}
