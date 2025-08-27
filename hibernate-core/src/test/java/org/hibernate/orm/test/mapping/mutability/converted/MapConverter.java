/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.mutability.converted;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.internal.util.collections.CollectionHelper;

import jakarta.persistence.AttributeConverter;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.join;
import static org.hibernate.internal.util.StringHelper.split;

/**
 * @author Steve Ebersole
 */
public class MapConverter implements AttributeConverter<Map<String, String>, String> {
	@Override
	public String convertToDatabaseColumn(Map<String, String> map) {
		return CollectionHelper.isEmpty( map ) ? null : join( ", ", asPairs( map ) );
	}

	@Override
	public Map<String, String> convertToEntityAttribute(String pairs) {
		return isEmpty( pairs ) ? null : toMap( split( ", ", pairs ) );
	}

	public static Map<String,String> toMap(String... pairs) {
		assert pairs.length % 2 == 0;
		switch ( pairs.length ) {
			case 0:
				return emptyMap();
			case 2:
				return singletonMap( pairs[0], pairs[1] );
			default:
				final Map<String,String> result = new HashMap<>();
				for ( int i = 0; i < pairs.length; i+=2 ) {
					result.put( pairs[i], pairs[i+1] );
				}
				return result;
		}
	}

	public static String[] asPairs(Map<String,String> map) {
		final String[] pairs = new String[ map.size() * 2 ];
		int i = 0;
		for ( Map.Entry<String,String> entry : map.entrySet() ) {
			pairs[i++] = entry.getKey();
			pairs[i++] = entry.getValue();
		}
		return pairs;
	}
}
