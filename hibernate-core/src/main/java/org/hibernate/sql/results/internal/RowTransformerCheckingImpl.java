/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.results.internal;

import org.hibernate.TypeMismatchException;
import org.hibernate.sql.results.spi.RowTransformer;

/**
 * @author Gavin King
 */
public class RowTransformerCheckingImpl<R> implements RowTransformer<R> {

	private final Class<R> type;

	public RowTransformerCheckingImpl(Class<R> type) {
		this.type = type;
	}

	@Override
	@SuppressWarnings("unchecked")
	public R transformRow(Object[] row) {
		final Object result = row[0];
		if ( result == null || type.isInstance( result ) ) {
			return (R) result;
		}
		else {
			throw new TypeMismatchException( "Result type is '" + type.getSimpleName()
					+ "' but the query returned a '" + result.getClass().getSimpleName() + "'" );
		}
	}

	@Override
	public int determineNumberOfResultElements(int rawElementCount) {
		return 1;
	}
}
