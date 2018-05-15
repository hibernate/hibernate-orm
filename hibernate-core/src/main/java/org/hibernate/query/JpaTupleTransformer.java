/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.sql.exec.internal.TupleElementImpl;
import org.hibernate.sql.exec.internal.TupleImpl;

/**
 * User extension to TupleTransformer indicating that the transformation
 * results in JPA {@link Tuple}.
 *
 * Hibernate uses this distinction to better understand
 * how this TupleTransformer part in shaping the query result row type.
 *
 * @author Steve Ebersole
 *
 * @deprecated The more appropriate approach is to specify the
 * Query's "result type" as {@link Tuple} - Hibernate will
 * properly generate a {@link Tuple}-generating
 * {@link org.hibernate.sql.exec.spi.RowTransformer}
 * which does this same logic
 */
@Deprecated
public class JpaTupleTransformer implements TupleTransformer<Tuple> {
	private List<TupleElement<?>> tupleElements;

	@Override
	public Tuple transformTuple(Object[] row, String[] aliases) {
		if ( tupleElements == null ) {
			tupleElements = generateTupleElements( row, aliases );
		}
		return new TupleImpl( tupleElements, row );
	}

	private static List<TupleElement<?>> generateTupleElements(Object[] row, String[] aliases) {
		final ArrayList<TupleElement<?>> elements = CollectionHelper.arrayList( row.length );
		for ( int i = 0, count = row.length; i < count; i++ ) {
			String alias = null;
			if ( aliases != null ) {
				alias = aliases[i];
			}
			elements.add( new TupleElementImpl<>( row[1].getClass(), alias ) );
		}
		return elements;
	}

	@Override
	public int determineNumberOfResultElements(int rawElementCount) {
		return 1;
	}
}
