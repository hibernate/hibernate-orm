/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel.generics.embeddable;

import jakarta.persistence.Embeddable;

@Embeddable
public class SomeNumber extends AbstractValueObject<Integer> {
	protected SomeNumber() {
	}

	public SomeNumber(final Integer value) {
		super( value );
	}
}
