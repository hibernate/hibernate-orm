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
public class ToDataBaseColumnThrowRuntimeExceptionConverter
		implements AttributeConverter<String, String> {

	public String convertToDatabaseColumn(String attribute) {
		throw new RuntimeException( "Exception was thrown from the converter" );
	}

	public String convertToEntityAttribute(String dbData) {
		return dbData;
	}
}
