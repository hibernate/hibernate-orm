/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable.discriminatorvalues;

import jakarta.persistence.Embeddable;

@Embeddable
public class Dog extends Mammal {
	// [...]
}
