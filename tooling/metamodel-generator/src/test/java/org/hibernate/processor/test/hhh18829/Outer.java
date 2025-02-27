/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hhh18829;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

public class Outer {

	@Entity
	static class Inner {
		@Id
		String empName;
		@Id
		Integer empId;
		String address;
	}

	@MappedSuperclass
	static class Super {
		@Id
		String empName;
		@Id
		Integer empId;
		String address;
	}
}
