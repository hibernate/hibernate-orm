/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.discriminator.joinedsubclass;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * @author Andrea Boriero
 */
@Entity
@DiscriminatorValue("SUB-SUB")
public class SubSubEntity extends SubEntity {
	private String SubSubString;

	public String getSubSubString() {
		return SubSubString;
	}

	public void setSubSubString(String subSubString) {
		SubSubString = subSubString;
	}
}
