/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.results.internal;

import org.hibernate.Incubating;
import org.hibernate.sql.results.spi.RowTransformer;

/**
 * Returns the first object in each row, if there
 * is exactly one item in the selection list, or
 * the whole row otherwise.
 *
 * @author Steve Ebersole
 */
@Incubating
public class RowTransformerStandardImpl<T> implements RowTransformer<T> {
	/**
	 * Singleton access
	 */
	@SuppressWarnings("rawtypes")
	private static final RowTransformerStandardImpl INSTANCE = new RowTransformerStandardImpl();

	@SuppressWarnings("unchecked")
	public static <T> RowTransformer<T> instance() {
		return INSTANCE;
	}

	private RowTransformerStandardImpl() {
	}

	@Override
	@SuppressWarnings("unchecked")
	public T transformRow(Object[] row) {
		return row.length == 1 ? (T) row[0] : (T) row;
	}
}
