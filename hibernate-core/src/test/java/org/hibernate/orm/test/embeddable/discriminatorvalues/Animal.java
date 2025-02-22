/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable.discriminatorvalues;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Embeddable;

@Embeddable
@DiscriminatorColumn(name = "animal_type", length = 1)
public class Animal {
	private int age;

	private String name;
}
