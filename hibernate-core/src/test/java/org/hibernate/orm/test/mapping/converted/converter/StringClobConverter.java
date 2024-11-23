/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
