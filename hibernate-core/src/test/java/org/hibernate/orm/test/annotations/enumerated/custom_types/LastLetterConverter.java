/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.enumerated.custom_types;

import org.hibernate.orm.test.annotations.enumerated.enums.FirstLetter;

import jakarta.persistence.AttributeConverter;

/**
 * @author Steve Ebersole
 */
public class LastLetterConverter implements AttributeConverter<FirstLetter,String> {
	@Override
	public String convertToDatabaseColumn(FirstLetter value) {
		if ( value == null ) {
			return null;
		}

		final String name = value.name();
		return name.substring( 0, 1 );
	}

	@Override
	public FirstLetter convertToEntityAttribute(String value) {
		if ( value == null ) {
			return null;
		}
		return Enum.valueOf( FirstLetter.class, value + "_LETTER" );

	}
}
