/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.gambit;

import jakarta.persistence.AttributeConverter;

/**
 * @author Steve Ebersole
 */
public class EnumValueConverter implements AttributeConverter<EnumValue, String> {
	@Override
	public String convertToDatabaseColumn(EnumValue domainValue) {
		return domainValue == null ? null : domainValue.getCode();
	}

	@Override
	public EnumValue convertToEntityAttribute(String dbData) {
		return EnumValue.fromCode( dbData );
	}
}
