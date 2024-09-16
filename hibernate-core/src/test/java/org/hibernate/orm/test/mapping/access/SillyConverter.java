/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.access;

import jakarta.persistence.AttributeConverter;

/**
 * @author Steve Ebersole
 */
public class SillyConverter implements AttributeConverter<String, String> {
	@Override
	public String convertToDatabaseColumn(String attribute) {
		return attribute;
	}

	@Override
	public String convertToEntityAttribute(String dbData) {
		return dbData;
	}
}
