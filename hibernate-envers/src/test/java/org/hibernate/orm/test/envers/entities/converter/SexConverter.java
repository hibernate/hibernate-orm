/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.converter;

import jakarta.persistence.AttributeConverter;

/**
 * @author Steve Ebersole
 */
public class SexConverter implements AttributeConverter<Sex, String> {

	@Override
	public String convertToDatabaseColumn(Sex attribute) {
		if (attribute == null) {
			return null;
		}

		switch (attribute) {
			case MALE: {
				return "M";
			}
			case FEMALE: {
				return "F";
			}
			default: {
				throw new IllegalArgumentException( "Unexpected Sex model value [" + attribute + "]" );
			}
		}
	}

	@Override
	public Sex convertToEntityAttribute(String dbData) {
		if (dbData == null) {
			return null;
		}

		if ( "M".equals( dbData ) ) {
			return Sex.MALE;
		}
		else if ( "F".equals( dbData ) ) {
			return Sex.FEMALE;
		}

		throw new IllegalArgumentException( "Unexpected Sex db value [" + dbData + "]" );
	}

}
