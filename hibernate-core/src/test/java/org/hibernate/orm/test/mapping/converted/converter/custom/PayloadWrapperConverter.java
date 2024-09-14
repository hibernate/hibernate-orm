/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
