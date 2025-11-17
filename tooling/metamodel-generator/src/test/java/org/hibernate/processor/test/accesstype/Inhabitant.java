/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.accesstype;

import java.util.Set;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Access;
import jakarta.persistence.ElementCollection;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
@Access(jakarta.persistence.AccessType.FIELD)
public class Inhabitant {
	private String name;
	@ElementCollection
	private Set<Pet> pets;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
