/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.transform;

import org.hibernate.internal.util.collections.ArrayHelper;

/**
 * {@link ResultTransformer} implementation which limits the result tuple
 * to only the "root entity".
 * <p/>
 * Since this transformer is stateless, all instances would be considered equal.
 * So for optimization purposes we limit it to a single, singleton {@link #INSTANCE instance}.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public final class RootEntityResultTransformer extends BasicTransformerAdapter implements TupleSubsetResultTransformer {

	public static final RootEntityResultTransformer INSTANCE = new RootEntityResultTransformer();

	/**
	 * Disallow instantiation of RootEntityResultTransformer.
	 */
	private RootEntityResultTransformer() {
	}

	/**
	 * Return just the root entity from the row tuple.
	 */
	@Override
	public Object transformTuple(Object[] tuple, String[] aliases) {
		return tuple[ tuple.length-1 ];
	}

	@Override
	public boolean isTransformedValueATupleElement(String[] aliases, int tupleLength) {
		return true;
	}

	@Override
	public boolean[] includeInTransform(String[] aliases, int tupleLength) {
		boolean[] includeInTransform;
		if ( tupleLength == 1 ) {
			includeInTransform = ArrayHelper.TRUE;
		}
		else {
			includeInTransform = new boolean[tupleLength];
			includeInTransform[ tupleLength - 1 ] = true;
		}
		return includeInTransform;
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
