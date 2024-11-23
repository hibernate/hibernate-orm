/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import jakarta.persistence.AttributeConverter;

public class ToEntityAttributeThrowRuntimeExceptionConverter implements AttributeConverter<String, String> {

	public String convertToDatabaseColumn(String attribute) {
		return attribute;
	}

	public String convertToEntityAttribute(String dbData) {
		throw new RuntimeException( "Exception was thrown from the converter" );
	}
}
