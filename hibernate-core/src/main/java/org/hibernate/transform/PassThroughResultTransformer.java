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
import java.util.Arrays;
import java.util.List;

/**
 * ???
 *
 * @author max
 */
public class PassThroughResultTransformer extends BasicTransformerAdapter implements TupleSubsetResultTransformer {

	public static final PassThroughResultTransformer INSTANCE = new PassThroughResultTransformer();

	/**
	 * Disallow instantiation of PassThroughResultTransformer.
	 */
	private PassThroughResultTransformer() {
	}

	@Override
	public Object transformTuple(Object[] tuple, String[] aliases) {
		return tuple.length==1 ? tuple[0] : tuple;
	}

	@Override
	public boolean isTransformedValueATupleElement(String[] aliases, int tupleLength) {
		return tupleLength == 1;
	}

	@Override
	public boolean[] includeInTransform(String[] aliases, int tupleLength) {
		boolean[] includeInTransformedResult = new boolean[tupleLength];
		Arrays.fill( includeInTransformedResult, true );
		return includeInTransformedResult;
	}

	/* package-protected */
	List untransformToTuples(List results, boolean isSingleResult) {
		// untransform only if necessary; if transformed, do it in place;
		if ( isSingleResult ) {
			for ( int i = 0 ; i < results.size() ; i++ ) {
				Object[] tuple = untransformToTuple( results.get( i ), isSingleResult);
				results.set( i, tuple );
			}
		}
		return results;
	}

	/* package-protected */
	Object[] untransformToTuple(Object transformed, boolean isSingleResult ) {
		return isSingleResult ? new Object[] { transformed } : ( Object[] ) transformed;
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
