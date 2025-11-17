/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.spi;

import org.hibernate.query.TupleTransformer;

import java.util.HashMap;
import java.util.Map;

import static java.util.Locale.ROOT;

/**
 * A {@link TupleTransformer} for handling {@link Map} results from native queries.
 *
 * @since 6.3
 *
 * @author Gavin King
 */
public class NativeQueryMapTransformer implements TupleTransformer<Map<String,Object>> {

	public static final NativeQueryMapTransformer INSTANCE = new NativeQueryMapTransformer();

	@Override
	public Map<String,Object> transformTuple(Object[] tuple, String[] aliases) {
		Map<String,Object> map = new HashMap<>( aliases.length );
		for ( int i = 0; i < aliases.length; i++ ) {
			map.put( aliases[i].toLowerCase(ROOT), tuple[i] );
		}
		return map;
	}
}
