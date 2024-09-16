/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.internal;

import org.hibernate.sql.results.spi.RowTransformer;

import java.util.List;

/**
 * {@link RowTransformer} instantiating a {@link List}
 *
 * @author Gavin King
 */
public class RowTransformerListImpl<T> implements RowTransformer<List<Object>> {
	/**
	 * Singleton access
	 */
	@SuppressWarnings( "rawtypes" )
	private static final RowTransformerListImpl INSTANCE = new RowTransformerListImpl();

	@SuppressWarnings( "unchecked" )
	public static <X> RowTransformerListImpl<X> instance() {
		return INSTANCE;
	}

	@Override
	public List<Object> transformRow(Object[] row) {
		return List.of( row );
	}
}
