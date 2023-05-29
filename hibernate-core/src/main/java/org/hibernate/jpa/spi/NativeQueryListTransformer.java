/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.spi;

import org.hibernate.query.TupleTransformer;

import java.util.List;

/**
 * A {@link TupleTransformer} for handling {@link List} results from native queries.
 *
 * @author Gavin King
 */
public class NativeQueryListTransformer implements TupleTransformer<List<Object>> {
	@Override
	public List<Object> transformTuple(Object[] tuple, String[] aliases) {
		return List.of( tuple );
	}
}
