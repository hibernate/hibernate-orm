/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.gambit;

import javax.persistence.AttributeConverter;

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
