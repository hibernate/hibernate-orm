/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.converter;

import javax.persistence.AttributeConverter;

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
