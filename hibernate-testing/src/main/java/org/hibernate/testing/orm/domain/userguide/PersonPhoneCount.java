/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.userguide;

import jakarta.persistence.Column;

/**
 * @author Vlad Mihalcea
 */
public class PersonPhoneCount {

	private final String name;

	@Column(name = "phone_count")
	private final Number phoneCount;

	public PersonPhoneCount(String name, Number phoneCount) {
		this.name = name;
		this.phoneCount = phoneCount;
	}

	public String getName() {
		return name;
	}

	public Number getPhoneCount() {
		return phoneCount;
	}
}
