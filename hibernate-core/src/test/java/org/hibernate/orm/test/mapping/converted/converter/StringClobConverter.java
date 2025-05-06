/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.sql.Clob;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * @author Brett Meyer
 */
@Converter( autoApply = true )
public class StringClobConverter implements AttributeConverter<String,Clob> {

	@Override
	public Clob convertToDatabaseColumn(String attribute) {
		return null;
	}

	@Override
	public String convertToEntityAttribute(Clob dbData) {
		return null;
	}
}
