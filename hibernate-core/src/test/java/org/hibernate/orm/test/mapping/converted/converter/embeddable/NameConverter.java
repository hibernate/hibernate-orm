/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.mapping.converted.converter.embeddable;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * @author Steve Ebersole
 */
@Converter( autoApply = true )
public class NameConverter implements AttributeConverter<Name, String[]> {
	@Override
	public String[] convertToDatabaseColumn(Name attribute) {
		final String[] parts = new String[2];
		// Hibernate expects these alphabetically
		parts[0] = attribute.getFamily();
		parts[1] = attribute.getPersonal();
		return parts;
	}

	@Override
	public Name convertToEntityAttribute(String[] dbData) {
		// again, alphabetically
		return new Name( dbData[1], dbData[0] );
	}
}
