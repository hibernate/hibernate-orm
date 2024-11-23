/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.test.bytecode.enhancement.mutable;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.AttributeConverter;

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