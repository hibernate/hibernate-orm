/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.internal;

import java.util.List;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.hibernate.sql.exec.spi.RowTransformer;

/**
 * RowTransformer generating a JPA {@link Tuple}
 *
 * @author Steve Ebersole
 */
public class RowTransformerJpaTupleImpl implements RowTransformer<Tuple> {
	private final List<TupleElement<?>> tupleElements;

	public RowTransformerJpaTupleImpl(List<TupleElement<?>> tupleElements) {
		this.tupleElements = tupleElements;
	}

	@Override
	public Tuple transformRow(Object[] row) {
		return new TupleImpl( tupleElements, row );
	}

	@Override
	public int determineNumberOfResultElements(int rawElementCount) {
		return 1;
	}
}
