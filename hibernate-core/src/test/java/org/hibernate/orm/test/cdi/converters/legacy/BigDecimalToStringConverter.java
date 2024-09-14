/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
