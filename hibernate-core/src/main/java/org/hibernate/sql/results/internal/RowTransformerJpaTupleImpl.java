/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.results.internal;

import jakarta.persistence.Tuple;

import org.hibernate.sql.results.spi.RowTransformer;

/**
 * RowTransformer generating a JPA {@link Tuple}
 *
 * @author Steve Ebersole
 */
public class RowTransformerJpaTupleImpl implements RowTransformer<Tuple> {
	private final TupleMetadata tupleMetadata;

	public RowTransformerJpaTupleImpl(TupleMetadata tupleMetadata) {
		this.tupleMetadata = tupleMetadata;
	}

	@Override
	public Tuple transformRow(Object[] row) {
		return new TupleImpl( tupleMetadata, row );
	}

	@Override
	public int determineNumberOfResultElements(int rawElementCount) {
		return 1;
	}
}
