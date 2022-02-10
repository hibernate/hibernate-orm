/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain;

import java.util.Locale;
import javax.money.Monetary;
import javax.money.MonetaryAmount;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * @author Steve Ebersole
 */
@Converter( autoApply = true )
public class MonetaryAmountConverter implements AttributeConverter<MonetaryAmount,Double> {
	@Override
	public Double convertToDatabaseColumn(MonetaryAmount attribute) {
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
