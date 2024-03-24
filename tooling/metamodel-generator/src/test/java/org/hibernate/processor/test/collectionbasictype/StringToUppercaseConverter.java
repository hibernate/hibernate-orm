/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.collectionbasictype;

import jakarta.persistence.AttributeConverter;

/**
 * @author Chris Cranford
 */
public class StringToUppercaseConverter implements AttributeConverter<String, String> {
	@Override
	public String convertToDatabaseColumn(String s) {
		return s == null ? null : s.toUpperCase();
	}

	@Override
	public String convertToEntityAttribute(String s) {
		return s;
	}
}
