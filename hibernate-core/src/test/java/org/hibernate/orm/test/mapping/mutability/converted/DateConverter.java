/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.mutability.converted;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.hibernate.internal.util.StringHelper;

import jakarta.persistence.AttributeConverter;

/**
 * Handles Date as a character data on the database
 *
 * @author Steve Ebersole
 */
public class DateConverter implements AttributeConverter<Date, String> {
	@Override
	public String convertToDatabaseColumn(Date date) {
		if ( date == null ) {
			return null;
		}
		return DateTimeFormatter.ISO_INSTANT.format( date.toInstant() );
	}

	@Override
	public Date convertToEntityAttribute(String date) {
		if ( StringHelper.isEmpty( date ) ) {
			return null;
		}
		return Date.from( Instant.from( DateTimeFormatter.ISO_INSTANT.parse( date ) ) );
	}
}
