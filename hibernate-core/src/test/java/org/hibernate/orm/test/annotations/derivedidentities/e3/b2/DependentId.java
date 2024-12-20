/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e3.b2;

import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class DependentId implements Serializable {
	String name;
	EmployeeId empPK;
}
