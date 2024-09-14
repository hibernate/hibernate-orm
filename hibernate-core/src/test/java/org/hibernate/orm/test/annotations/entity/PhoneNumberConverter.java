/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * @author Steve Ebersole
 */
@Converter( autoApply = true )
public class PhoneNumberConverter implements AttributeConverter<PhoneNumber, String> {
	@Override
	public String convertToDatabaseColumn(PhoneNumber phoneNumber) {
		if ( phoneNumber == null ) {
			return null;
		}
		return phoneNumber.getNumber();
	}

	@Override
	public PhoneNumber convertToEntityAttribute(String value) {
		if ( value == null ) {
			return null;
		}

		if ( value.length() <= 6 ) {
			return new PhoneNumber( value );
		}
		else {
			return new OverseasPhoneNumber( value );
		}
	}
}
