/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e3.b3;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class EmployeeId implements Serializable {
	@Column(name="firstname", length = 32)
	String firstName;
	@Column(name="lastname", length = 32)
	String lastName;
}
