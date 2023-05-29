/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.spi;

import org.hibernate.query.TupleTransformer;

import java.util.HashMap;
import java.util.Map;

import static java.util.Locale.ROOT;

/**
 * A {@link TupleTransformer} for handling {@link Map} results from native queries.
 *
 * @author Gavin King
 */
public class NativeQueryMapTransformer implements TupleTransformer<Map<String,Object>> {
	@Override
	public Map<String,Object> transformTuple(Object[] tuple, String[] aliases) {
		Map<String,Object> map = new HashMap<>( aliases.length );
		for ( int i = 0; i < aliases.length; i++ ) {
			map.put( aliases[i].toLowerCase(ROOT), tuple[i] );
		}
		return map;
	}
}
