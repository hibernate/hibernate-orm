/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.mutable;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.persistence.AttributeConverter;

public class MapStringConverter implements AttributeConverter<Map<String, String>, String> {

	@Override
	public String convertToDatabaseColumn(Map<String, String> attribute) {
		if ( attribute == null ) {
			return null;
		}
		return attribute.entrySet().stream()
				.map( entry -> entry.getKey() + ";" + entry.getValue() )
				.collect( Collectors.joining( ";" ) );
	}

	@Override
	public Map<String, String> convertToEntityAttribute(String dbData) {
		if ( dbData == null ) {
			return null;
		}
		String[] strings = dbData.split( ";" );
		Map<String, String> map = new HashMap<>();
		for ( int i = 0; i < strings.length; i += 2 ) {
			map.put( strings[i], strings[i + 1] );
		}
		return map;
	}
}
