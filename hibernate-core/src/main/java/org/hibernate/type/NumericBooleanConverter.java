/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Handles conversion to/from Boolean as `0` (false) or `1` (true)
 *
 * @author Steve Ebersole
 */
@Converter
public class NumericBooleanConverter implements AttributeConverter<Boolean,Integer> {
	@Override
	public Integer convertToDatabaseColumn(Boolean attribute) {
		return NumericBooleanType.NumericConverter.toRelational( attribute );
	}

	@Override
	public Boolean convertToEntityAttribute(Integer dbData) {
		return NumericBooleanType.NumericConverter.toDomain( dbData );
	}
}
