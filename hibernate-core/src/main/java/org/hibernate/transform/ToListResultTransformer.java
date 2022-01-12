/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.transform;

import java.util.Arrays;
import java.util.List;

/**
 * Transforms each result row from a tuple into a {@link List} whose elements are each tuple value
 */
public class ToListResultTransformer implements ResultTransformer<List<Object>> {
	public static final ToListResultTransformer INSTANCE = new ToListResultTransformer();

	/**
	 * Disallow instantiation of ToListResultTransformer.
	 */
	private ToListResultTransformer() {
	}

	@Override
	public List<Object> transformTuple(Object[] tuple, String[] aliases) {
		return Arrays.asList( tuple );
	}

	/**
	 * Serialization hook for ensuring singleton uniqueing.
	 *
	 * @return The singleton instance : {@link #INSTANCE}
	 */
	private Object readResolve() {
		return INSTANCE;
	}
}
