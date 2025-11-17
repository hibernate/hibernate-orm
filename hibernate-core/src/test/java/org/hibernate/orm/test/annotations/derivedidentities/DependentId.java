/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities;
import java.io.Serializable;
import jakarta.persistence.Embeddable;

/**
 * @author Hardy Ferentschik
 */
@Embeddable
public class DependentId implements Serializable {
	String name;

	long empPK;	// corresponds to PK type of Employee

	public DependentId() {
	}

	public DependentId(long empPK, String name) {
		this.empPK = empPK;
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
