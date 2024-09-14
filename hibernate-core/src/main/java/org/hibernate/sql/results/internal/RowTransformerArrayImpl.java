/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
