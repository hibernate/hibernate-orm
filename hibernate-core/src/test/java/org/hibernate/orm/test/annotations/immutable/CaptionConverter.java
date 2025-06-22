/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.immutable;

import jakarta.persistence.AttributeConverter;

/**
 * Created by soldier on 12.04.16.
 */
public class CaptionConverter implements AttributeConverter<Caption, String> {

	@Override
	public String convertToDatabaseColumn(Caption attribute) {
		return attribute.getText();
	}

	@Override
	public Caption convertToEntityAttribute(String dbData) {
		return new Caption( dbData );
	}
}
