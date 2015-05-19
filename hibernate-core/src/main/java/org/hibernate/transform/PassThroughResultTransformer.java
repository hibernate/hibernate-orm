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
