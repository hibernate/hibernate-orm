/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.embeddedid.withoutinheritance;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Person {
	@EmbeddedId
	private PersonId id;

	private String address;
}
