package org.hibernate.test.annotations.derivedidentities.e3.b3;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class EmployeeId implements Serializable {
	@Column(length = 80) // for some reason db2 complains about too large PK if this is set to default (255)
	String firstName;
	String lastName;
}
