/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * @author Steve Ebersole
 */
@Converter(autoApply = true)
public class ColorTypeConverter implements AttributeConverter<ColorType, String> {

	@Override
	public String convertToDatabaseColumn(ColorType attribute) {
		return attribute == null ? null : attribute.toExternalForm();
	}

	@Override
	public ColorType convertToEntityAttribute(String dbData) {
		return dbData == null ? null : ColorType.fromExternalForm( dbData );
	}
}
