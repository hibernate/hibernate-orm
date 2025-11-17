/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;

import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;

@Embeddable
public class LastName {
	@Convert( converter = ToUpperConverter.class )
	private String lastName;

	public String getName() {
		return lastName;
	}

	public void setName(String lowerCaseName) {
		this.lastName = lowerCaseName;
	}


}
