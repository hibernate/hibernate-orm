/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converter.hbm;

import jakarta.persistence.AttributeConverter;

//tag::basic-hbm-attribute-converter-mapping-moneyconverter-example[]
public class MoneyConverter
		implements AttributeConverter<Money, Long> {

	@Override
	public Long convertToDatabaseColumn(Money attribute) {
		return attribute == null ? null : attribute.getCents();
	}

	@Override
	public Money convertToEntityAttribute(Long dbData) {
		return dbData == null ? null : new Money(dbData);
	}
}
//end::basic-hbm-attribute-converter-mapping-moneyconverter-example[]
