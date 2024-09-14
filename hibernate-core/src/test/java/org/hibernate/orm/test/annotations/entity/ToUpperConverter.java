/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.entity;

import java.util.Locale;

import jakarta.persistence.AttributeConverter;

/**
 * @author Steve Ebersole
 */
public class ToUpperConverter implements AttributeConverter<String,String> {
	@Override
	public String convertToDatabaseColumn(String value) {
		if ( value == null ) {
			return null;
		}

		return value.toUpperCase( Locale.ROOT );
	}

	@Override
	public String convertToEntityAttribute(String value) {
		if ( value == null ) {
			return null;
		}

		assert value.toUpperCase( Locale.ROOT ).equals( value );

		return value;
	}
}
