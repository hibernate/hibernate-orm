/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.transform;
import java.util.Map;

import org.hibernate.internal.util.collections.CollectionHelper;

/**
 * {@link ResultTransformer} implementation which builds a map for each "row",
 * made up of each aliased value where the alias is the map key.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class AliasToEntityMapResultTransformer implements ResultTransformer<Map<String,Object>> {

	public static final AliasToEntityMapResultTransformer INSTANCE = new AliasToEntityMapResultTransformer();

	/**
	 * Disallow instantiation of AliasToEntityMapResultTransformer.
	 */
	private AliasToEntityMapResultTransformer() {
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
