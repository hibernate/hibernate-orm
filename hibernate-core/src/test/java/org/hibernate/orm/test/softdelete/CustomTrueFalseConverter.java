/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete;

import jakarta.persistence.AttributeConverter;

/**
 * A non-standard boolean converter to make sure check constraints get applied properly
 *
 * @author Steve Ebersole
 */
public class CustomTrueFalseConverter implements AttributeConverter<Boolean,Character> {
	@Override
	public Character convertToDatabaseColumn(Boolean attribute) {
		if ( attribute == null ) {
			return null;
		}
		return attribute ? 'T' : 'F';
	}

	@Override
	public Boolean convertToEntityAttribute(Character dbData) {
		if ( dbData == null ) {
			// NOTE : we assume active
			return true;
		}
		if ( dbData.equals( 'T' ) ) {
			return true;
		}
		if ( dbData.equals( 'F' ) ) {
			return false;
		}
		throw new IllegalArgumentException( "Unexpected database value - '" + dbData + "', expected 'T' or 'F'" );
	}
}
