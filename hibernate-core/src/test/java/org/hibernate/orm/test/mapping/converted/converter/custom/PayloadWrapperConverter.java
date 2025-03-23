/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.custom;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * @author Steve Ebersole
 */
@Converter( autoApply = true )
public class PayloadWrapperConverter implements AttributeConverter<PayloadWrapper, String> {
	@Override
	public String convertToDatabaseColumn(PayloadWrapper attribute) {
		return null;
	}

	@Override
	public PayloadWrapper convertToEntityAttribute(String dbData) {
		return null;
	}
}
