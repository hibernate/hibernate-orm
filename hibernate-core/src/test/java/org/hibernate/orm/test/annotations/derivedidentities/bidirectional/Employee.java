/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.bidirectional;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Employee {
	@Id
	long empId;
	String empName;

	@OneToMany(mappedBy = "emp", fetch = FetchType.LAZY)
	private Set<Dependent> dependents;
}
