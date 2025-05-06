/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
