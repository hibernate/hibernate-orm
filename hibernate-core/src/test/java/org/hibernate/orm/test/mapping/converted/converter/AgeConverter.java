/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.converted.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AgeConverter implements AttributeConverter<String, Integer> {

	public Integer convertToDatabaseColumn(String attribute) {
		return Integer.valueOf( attribute.replace( ".", "" ) );
	}

	public String convertToEntityAttribute(Integer dbData) {
		return dbData.toString();
	}
}
