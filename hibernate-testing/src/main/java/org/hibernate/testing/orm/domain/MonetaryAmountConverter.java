/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain;

import java.util.Locale;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * @author Steve Ebersole
 */
@Converter( autoApply = true )
public class MonetaryAmountConverter implements AttributeConverter<MonetaryAmount,Double> {
	@Override
	public Double convertToDatabaseColumn(MonetaryAmount attribute) {
		if ( attribute == null ) {
			return null;
		}
		return attribute.getNumber().numberValueExact( Double.class );
	}

	@Override
	public MonetaryAmount convertToEntityAttribute(Double dbData) {
		if ( dbData == null ) {
			return null;
		}

		return Monetary.getDefaultAmountFactory().setNumber( dbData ).setCurrency( Monetary.getCurrency( Locale.US ) ).create();
	}
}
