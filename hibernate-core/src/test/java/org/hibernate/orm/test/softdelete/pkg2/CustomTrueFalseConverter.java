/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.softdelete.pkg2;

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
		throw new IllegalArgumentException( "Unexpected database value - `" + dbData + "`, expected 'T' or 'F'" );
	}
}
