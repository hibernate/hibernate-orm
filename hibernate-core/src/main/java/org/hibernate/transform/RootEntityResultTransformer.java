/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
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

	/**
	 * {@inheritDoc}
	 */
	public boolean isTransformedValueATupleElement(String[] aliases, int tupleLength) {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
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
