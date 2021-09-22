/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import jakarta.persistence.AttributeConverter;

/**
 * @author Steve Ebersole
 */
public class NoAttributeConverter<O,R> implements AttributeConverter<O,R> {
	@Override
	public R convertToDatabaseColumn(Object attribute) {
		throw new UnsupportedOperationException();
	}

	@Override
	public O convertToEntityAttribute(Object dbData) {
		throw new UnsupportedOperationException();
	}
}
