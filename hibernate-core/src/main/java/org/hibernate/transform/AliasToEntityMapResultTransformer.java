/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.transform;
import java.util.Map;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.query.TypedTupleTransformer;

/**
 * {@link ResultTransformer} implementation which builds a map for each "row",
 * made up of each aliased value where the alias is the map key.
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @deprecated since {@link ResultTransformer} is deprecated
 */
@Deprecated
public class AliasToEntityMapResultTransformer implements ResultTransformer<Map<String,Object>>, TypedTupleTransformer<Map<String,Object>> {

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
