/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converter;

import java.time.Period;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * @author Vlad Mihalcea
 */
//tag::basic-jpa-convert-period-string-converter-example[]
@Converter
public class PeriodStringConverter
		implements AttributeConverter<Period, String> {

	@Override
	public String convertToDatabaseColumn(Period attribute) {
		return attribute.toString();
	}

	@Override
	public Period convertToEntityAttribute(String dbData) {
		return Period.parse(dbData);
	}
}
//end::basic-jpa-convert-period-string-converter-example[]
