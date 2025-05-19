/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.spi;

import org.hibernate.query.TupleTransformer;

/**
 * A {@link TupleTransformer} for handling {@code Object[]} results from native queries.
 */
public class NativeQueryArrayTransformer implements TupleTransformer<Object[]> {

	public static final NativeQueryArrayTransformer INSTANCE = new NativeQueryArrayTransformer();

	@Override
	public Object[] transformTuple(Object[] tuple, String[] aliases) {
		return tuple;
	}
}
