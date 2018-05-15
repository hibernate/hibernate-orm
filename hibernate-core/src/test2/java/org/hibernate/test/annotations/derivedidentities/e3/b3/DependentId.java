package org.hibernate.test.annotations.derivedidentities.e3.b3;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class DependentId implements Serializable {
	@Column(length = 32)
	String name;
	EmployeeId empPK;
}
