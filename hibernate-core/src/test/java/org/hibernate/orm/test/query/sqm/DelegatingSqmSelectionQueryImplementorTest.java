/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm;

import org.hibernate.query.sqm.spi.DelegatingSqmSelectionQueryImplementor;
import org.hibernate.query.sqm.spi.SqmSelectionQueryImplementor;

/**
 * This class just serves as compilation unit to verify we implemented all methods in the {@link DelegatingSqmSelectionQueryImplementor} class.
 */
public class DelegatingSqmSelectionQueryImplementorTest<R> extends DelegatingSqmSelectionQueryImplementor<R> {
	@Override
	protected SqmSelectionQueryImplementor<R> getDelegate() {
		return null;
	}
}
