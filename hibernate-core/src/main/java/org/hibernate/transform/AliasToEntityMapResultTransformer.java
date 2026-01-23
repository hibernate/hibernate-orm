/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.transform;
import java.util.Map;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.query.TypedTupleTransformer;

/**
 * {@link TypedTupleTransformer} implementation which builds a map for each "row",
 * made up of each aliased value where the alias is the map key.
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @deprecated Defines a {@linkplain jakarta.persistence.Tuple} mapping
 * and there are many well-defined ways to use that approach.
 */
@Deprecated
public class AliasToEntityMapResultTransformer implements TypedTupleTransformer<Map<String,Object>> {
	/**
	 * Singleton instance.
	 */
	public static final AliasToEntityMapResultTransformer INSTANCE = new AliasToEntityMapResultTransformer();

	/**
	 * Disallow instantiation of AliasToEntityMapResultTransformer.
	 */
	private AliasToEntityMapResultTransformer() {
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Class getTransformedType() {
		return Map.class;
	}

	@Override
	public Map<String,Object> transformTuple(Object[] tuple, String[] aliases) {
		Map<String,Object> result = CollectionHelper.mapOfSize( tuple.length );
		for ( int i = 0; i < tuple.length; i++ ) {
			String alias = aliases[i];
			if ( alias != null ) {
				result.put( alias, tuple[i] );
			}
		}
		return result;
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
