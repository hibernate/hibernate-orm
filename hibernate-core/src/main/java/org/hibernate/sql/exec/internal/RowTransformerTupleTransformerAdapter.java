/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.internal;

import org.hibernate.query.TupleTransformer;
import org.hibernate.sql.exec.spi.RowTransformer;

/**
 * An adapter for treating a {@link TupleTransformer} as a {@link RowTransformer}
 *
 * @author Steve Ebersole
 */
public class RowTransformerTupleTransformerAdapter<T> implements RowTransformer<T> {
	private final String[] aliases;
	private final TupleTransformer<T> tupleTransformer;

	public RowTransformerTupleTransformerAdapter(String[] aliases, TupleTransformer<T> tupleTransformer) {
		this.aliases = aliases;
		this.tupleTransformer = tupleTransformer;
	}

	@Override
	public T transformRow(Object[] row) {
		assert aliases == null || row.length == aliases.length;
		return tupleTransformer.transformTuple( row, aliases );
	}

	@Override
	public int determineNumberOfResultElements(int rawElementCount) {
		return tupleTransformer.determineNumberOfResultElements( rawElementCount );
	}
}
