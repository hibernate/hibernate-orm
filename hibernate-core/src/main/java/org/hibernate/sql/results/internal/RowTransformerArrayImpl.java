/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.internal;

import org.hibernate.sql.results.spi.RowTransformer;

/**
 * RowTransformer used when an array is explicitly specified as the return type
 *
 * @author Steve Ebersole
 */
public class RowTransformerArrayImpl implements RowTransformer<Object[]> {
	/**
	 * Singleton access
	 */
	private static final RowTransformerArrayImpl INSTANCE = new RowTransformerArrayImpl();

	public static RowTransformerArrayImpl instance() {
		return INSTANCE;
	}

	@Override
	public Object[] transformRow(Object[] row) {
		return row;
	}
}
