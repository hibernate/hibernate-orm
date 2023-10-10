/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.softdelete;

import jakarta.persistence.AttributeConverter;

/**
 * @author Steve Ebersole
 */
public class ReverseYesNoConverter implements AttributeConverter<Boolean,Character> {
	@Override
	public Character convertToDatabaseColumn(Boolean attribute) {
		return attribute ? 'N' : 'Y';
	}

	@Override
	public Boolean convertToEntityAttribute(Character dbData) {
		if ( dbData == 'Y' ) {
			return false;
		}

		if ( dbData == 'N' ) {
			return true;
		}

		throw new IllegalArgumentException( "Illegal value [" + dbData + "]; expecting 'Y' or 'N'" );
	}
}
