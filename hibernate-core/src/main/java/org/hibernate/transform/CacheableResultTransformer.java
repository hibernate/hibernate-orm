/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.transform;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.type.Type;

/**
 * A ResultTransformer that is used to transform tuples to a value(s)
 * that can be cached.
 *
 * @author Gail Badner
 */
public class CacheableResultTransformer implements ResultTransformer {

	// would be nice to be able to have this class extend
	// PassThroughResultTransformer, but the default constructor
	// is private (as it should be for a singleton)
	private final static PassThroughResultTransformer ACTUAL_TRANSFORMER =
			PassThroughResultTransformer.INSTANCE;
	private final int tupleLength;
	private final int tupleSubsetLength;

	// array with the i-th element indicating whether the i-th
	// expression returned by a query is included in the tuple;
	// IMPLLEMENTATION NOTE:
	// "joined" and "fetched" associations may use the same SQL,
	// but result in different tuple and cached values. This is
	// because "fetched" associations are excluded from the tuple.
	//  includeInTuple provides a way to distinguish these 2 cases.
	private final boolean[] includeInTuple;

	// indexes for tuple that are included in the transformation;
	// set to null if all elements in the tuple are included
	private final int[] includeInTransformIndex;

	/**
	 * Returns a CacheableResultTransformer that is used to transform
	 * tuples to a value(s) that can be cached.
	 *
	 * @param transformer - result transformer that will ultimately be
	 *        be used (afterQuery caching results)
	 * @param aliases - the aliases that correspond to the tuple;
	 *        if it is non-null, its length must equal the number
	 *        of true elements in includeInTuple[]
	 * @param includeInTuple - array with the i-th element indicating
	 *        whether the i-th expression returned by a query is
	 *        included in the tuple; the number of true values equals
	 *        the length of the tuple that will be transformed;
	 *        must be non-null
	 *
	 * @return a CacheableResultTransformer that is used to transform
	 *         tuples to a value(s) that can be cached.
	 */
	public static CacheableResultTransformer create(
			ResultTransformer transformer,
			String[] aliases,
			boolean[] includeInTuple) {
		return transformer instanceof TupleSubsetResultTransformer
				? create( ( TupleSubsetResultTransformer ) transformer, aliases, includeInTuple )
				: create( includeInTuple );
	}

	/**
	 * Returns a CacheableResultTransformer that is used to transform
	 * tuples to a value(s) that can be cached.
	 *
	 * @param transformer - a tuple subset result transformer;
	 *        must be non-null;
	 * @param aliases - the aliases that correspond to the tuple;
	 *        if it is non-null, its length must equal the number
	 *        of true elements in includeInTuple[]
	 * @param includeInTuple - array with the i-th element indicating
	 *        whether the i-th expression returned by a query is
	 *        included in the tuple; the number of true values equals
	 *        the length of the tuple that will be transformed;
	 *        must be non-null
	 *
	 * @return a CacheableResultTransformer that is used to transform
	 *         tuples to a value(s) that can be cached.
	 */
	private static CacheableResultTransformer create(
			TupleSubsetResultTransformer transformer,
			String[] aliases,
			boolean[] includeInTuple) {
		if ( transformer == null ) {
			throw new IllegalArgumentException( "transformer cannot be null" );
		}
		int tupleLength = ArrayHelper.countTrue( includeInTuple );
		if ( aliases != null && aliases.length != tupleLength ) {
			throw new IllegalArgumentException(
					"if aliases is not null, then the length of aliases[] must equal the number of true elements in includeInTuple; " +
							"aliases.length=" + aliases.length + "tupleLength=" + tupleLength
			);
		}
		return new CacheableResultTransformer(
				includeInTuple,
				transformer.includeInTransform( aliases, tupleLength )
		);
	}

	/**
	 * Returns a CacheableResultTransformer that is used to transform
	 * tuples to a value(s) that can be cached.
	 *
	 * @param includeInTuple - array with the i-th element indicating
	 *        whether the i-th expression returned by a query is
	 *        included in the tuple; the number of true values equals
	 *        the length of the tuple that will be transformed;
	 *        must be non-null
	 *
	 * @return a CacheableResultTransformer that is used to transform
	 *         tuples to a value(s) that can be cached.
	 */
	private static CacheableResultTransformer create(boolean[] includeInTuple) {
		return new CacheableResultTransformer( includeInTuple, null );
	}

	private CacheableResultTransformer(boolean[] includeInTuple, boolean[] includeInTransform) {
		if ( includeInTuple == null ) {
			throw new IllegalArgumentException( "includeInTuple cannot be null" );
		}
		this.includeInTuple = includeInTuple;
		tupleLength = ArrayHelper.countTrue( includeInTuple );
		tupleSubsetLength = (
				includeInTransform == null ?
						tupleLength :
						ArrayHelper.countTrue( includeInTransform )
		);
		if ( tupleSubsetLength == tupleLength ) {
			includeInTransformIndex = null;
		}
		else {
			includeInTransformIndex = new int[tupleSubsetLength];
			for ( int i = 0, j = 0 ; i < includeInTransform.length ; i++ ) {
				if ( includeInTransform[ i ] ) {
					includeInTransformIndex[ j ] =  i;
					j++;
				}
			}
		}
	}

	@Override
	public Object transformTuple(Object[] tuple, String[] aliases) {
		if ( aliases != null && aliases.length != tupleLength ) {
			throw new IllegalStateException(
					"aliases expected length is " + tupleLength +
					"; actual length is " + aliases.length );
		}
		// really more correct to pass index( aliases.getClass(), aliases )
		// as the 2nd arg to the following statement;
		// passing null instead because it ends up being ignored.
		return ACTUAL_TRANSFORMER.transformTuple( index( tuple.getClass(), tuple ), null );
	}

	/**
	 * Re-transforms, if necessary, a List of values previously
	 * transformed by this (or an equivalent) CacheableResultTransformer.
	 * Each element of the list is re-transformed in place (i.e, List
	 * elements are replaced with re-transformed values) and the original
	 * List is returned.
	 * <p/>
	 * If re-transformation is unnecessary, the original List is returned
	 * unchanged.
	 *
	 * @param transformedResults - results that were previously transformed
	 * @param aliases - the aliases that correspond to the untransformed tuple;
	 * @param transformer - the transformer for the re-transformation
	 * @param includeInTuple indicates the indexes of
	 *
	 * @return transformedResults, with each element re-transformed (if nececessary)
	 */
	@SuppressWarnings( {"unchecked"})
	public List retransformResults(
			List transformedResults,
			String[] aliases,
			ResultTransformer transformer,
			boolean[] includeInTuple) {
		if ( transformer == null ) {
			throw new IllegalArgumentException( "transformer cannot be null" );
		}
		if ( ! this.equals( create( transformer, aliases, includeInTuple ) ) ) {
			throw new IllegalStateException(
					"this CacheableResultTransformer is inconsistent with specified arguments; cannot re-transform"
			);
		}
		boolean requiresRetransform = true;
		String[] aliasesToUse = aliases == null ? null : index( ( aliases.getClass() ), aliases );
		if ( transformer == ACTUAL_TRANSFORMER ) {
			requiresRetransform = false;
		}
		else if ( transformer instanceof TupleSubsetResultTransformer ) {
			requiresRetransform =  ! ( ( TupleSubsetResultTransformer ) transformer ).isTransformedValueATupleElement(
					aliasesToUse,
					tupleLength
			);
		}
		if ( requiresRetransform ) {
			for ( int i = 0 ; i < transformedResults.size() ; i++ ) {
				Object[] tuple = ACTUAL_TRANSFORMER.untransformToTuple(
									transformedResults.get( i ),
									tupleSubsetLength == 1
				);
				transformedResults.set( i, transformer.transformTuple( tuple, aliasesToUse ) );
			}
		}
		return transformedResults;
	}

	/**
	 * Untransforms, if necessary, a List of values previously
	 * transformed by this (or an equivalent) CacheableResultTransformer.
	 * Each element of the list is untransformed in place (i.e, List
	 * elements are replaced with untransformed values) and the original
	 * List is returned.
	 * <p/>
	 * If not unnecessary, the original List is returned
	 * unchanged.
	 * <p/>
	 * NOTE: If transformed values are a subset of the original
	 *       tuple, then, on return, elements corresponding to
	 *       excluded tuple elements will be null.
	 * @param results - results that were previously transformed
	 * @return results, with each element untransformed (if nececessary)
	 */
	@SuppressWarnings( {"unchecked"})
	public List untransformToTuples(List results) {
		if ( includeInTransformIndex == null ) {
			results = ACTUAL_TRANSFORMER.untransformToTuples(
					results,
					tupleSubsetLength == 1
			);
		}
		else {
			for ( int i = 0 ; i < results.size() ; i++ ) {
				Object[] tuple = ACTUAL_TRANSFORMER.untransformToTuple(
									results.get( i ),
									tupleSubsetLength == 1
				);
				results.set( i, unindex( tuple.getClass(), tuple ) );
			}

		}
		return results;
	}

	public Type[] getCachedResultTypes(Type[] tupleResultTypes) {
		return tupleLength != tupleSubsetLength
				? index( tupleResultTypes.getClass(), tupleResultTypes )
				: tupleResultTypes;
	}

	@Override
	public List transformList(List list) {
		return list;
	}

	private <T> T[] index(Class<? extends T[]> clazz, T[] objects) {
		T[] objectsIndexed = objects;
		if ( objects != null &&
				includeInTransformIndex != null &&
				objects.length != tupleSubsetLength ) {
			objectsIndexed = clazz.cast( Array.newInstance( clazz.getComponentType(), tupleSubsetLength ) );
			for ( int i = 0 ; i < tupleSubsetLength; i++ ) {
				objectsIndexed[ i ] = objects[ includeInTransformIndex[ i ] ];
			}
		}
		return objectsIndexed;
	}

	private <T> T[] unindex(Class<? extends T[]> clazz, T[] objects) {
		T[] objectsUnindexed = objects;
		if ( objects != null &&
				includeInTransformIndex != null &&
				objects.length != tupleLength ) {
			objectsUnindexed = clazz.cast( Array.newInstance( clazz.getComponentType(), tupleLength ) );
			for ( int i = 0 ; i < tupleSubsetLength; i++ ) {
				objectsUnindexed[ includeInTransformIndex[ i ] ] = objects[ i ];
			}
		}
		return objectsUnindexed;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		CacheableResultTransformer that = ( CacheableResultTransformer ) o;

		return tupleLength == that.tupleLength
				&& tupleSubsetLength == that.tupleSubsetLength
				&& Arrays.equals( includeInTuple, that.includeInTuple )
				&& Arrays.equals( includeInTransformIndex, that.includeInTransformIndex );
	}

	@Override
	public int hashCode() {
		int result = tupleLength;
		result = 31 * result + tupleSubsetLength;
		result = 31 * result + ( includeInTuple != null ? Arrays.hashCode( includeInTuple ) : 0 );
		result = 31 * result + ( includeInTransformIndex != null ? Arrays.hashCode( includeInTransformIndex ) : 0 );
		return result;
	}
}
