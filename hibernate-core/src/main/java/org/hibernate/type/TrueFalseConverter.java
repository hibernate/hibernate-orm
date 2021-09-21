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
 * Handles conversion to/from Boolean as `T` or `F`
 *
 * @author Steve Ebersole
 */
@Converter
public class TrueFalseConverter implements AttributeConverter<Boolean,Character> {
	@Override
	public Character convertToDatabaseColumn(Boolean attribute) {
		return TrueFalseType.TrueFalseConverter.toRelational( attribute );
	}

	@Override
	public Boolean convertToEntityAttribute(Character dbData) {
		return TrueFalseType.TrueFalseConverter.toDomain( dbData );
	}
}
