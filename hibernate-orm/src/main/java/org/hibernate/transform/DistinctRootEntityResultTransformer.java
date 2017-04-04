/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.transform;
import java.util.List;

/**
 * Much like {@link RootEntityResultTransformer}, but we also distinct
 * the entity in the final result.
 * <p/>
 * Since this transformer is stateless, all instances would be considered equal.
 * So for optimization purposes we limit it to a single, singleton {@link #INSTANCE instance}.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DistinctRootEntityResultTransformer implements TupleSubsetResultTransformer {

	public static final DistinctRootEntityResultTransformer INSTANCE = new DistinctRootEntityResultTransformer();

	/**
	 * Disallow instantiation of DistinctRootEntityResultTransformer.
	 */
	private DistinctRootEntityResultTransformer() {
	}

	/**
	 * Simply delegates to {@link RootEntityResultTransformer#transformTuple}.
	 *
	 * @param tuple The tuple to transform
	 * @param aliases The tuple aliases
	 * @return The transformed tuple row.
	 */
	@Override
	public Object transformTuple(Object[] tuple, String[] aliases) {
		return RootEntityResultTransformer.INSTANCE.transformTuple( tuple, aliases );
	}

	/**
	 * Simply delegates to {@link DistinctResultTransformer#transformList}.
	 *
	 * @param list The list to transform.
	 * @return The transformed List.
	 */
	@Override
	public List transformList(List list) {
		return DistinctResultTransformer.INSTANCE.transformList( list );
	}

	@Override
	public boolean[] includeInTransform(String[] aliases, int tupleLength) {
		return RootEntityResultTransformer.INSTANCE.includeInTransform( aliases, tupleLength );
	}

	@Override
	public boolean isTransformedValueATupleElement(String[] aliases, int tupleLength) {
		return RootEntityResultTransformer.INSTANCE.isTransformedValueATupleElement( null, tupleLength );
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
