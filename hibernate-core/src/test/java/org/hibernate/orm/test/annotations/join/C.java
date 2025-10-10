/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.join;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.SecondaryTable;

/**
 * @author Emmanuel Bernard
 */
@Entity
@DiscriminatorValue("C")
@SecondaryTable(name="C")
public class C extends B {
	@Column(table = "C") private int age;


	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}
}
