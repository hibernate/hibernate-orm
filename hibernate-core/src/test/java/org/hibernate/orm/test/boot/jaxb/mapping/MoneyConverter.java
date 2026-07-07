/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping;

import jakarta.persistence.AttributeConverter;

public class MoneyConverter implements AttributeConverter<Money, Long> {
	@Override
	public Long convertToDatabaseColumn(Money attribute) {
		return attribute == null ? null : attribute.getCents();
	}

	@Override
	public Money convertToEntityAttribute(Long dbData) {
		return dbData == null ? null : new Money( dbData );
	}
}
