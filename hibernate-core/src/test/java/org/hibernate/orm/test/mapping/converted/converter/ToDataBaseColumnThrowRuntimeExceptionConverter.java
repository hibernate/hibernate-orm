/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
