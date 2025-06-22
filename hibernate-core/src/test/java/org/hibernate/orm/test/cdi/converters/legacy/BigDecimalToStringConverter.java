/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.converters.legacy;

import java.math.BigDecimal;

import jakarta.persistence.AttributeConverter;

/**
 * @author Etienne Miret
 */
public class BigDecimalToStringConverter implements AttributeConverter<BigDecimal, String> {

	@Override
	public String convertToDatabaseColumn(BigDecimal attribute) {
		if ( attribute == null ) {
			return null;
		}
		else {
			return attribute.toString();
		}
	}

	@Override
	public BigDecimal convertToEntityAttribute(String dbData) {
		if ( dbData == null ) {
			return null;
		}
		else {
			return new BigDecimal( dbData );
		}
	}

}
