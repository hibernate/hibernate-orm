/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.process;

import java.util.UUID;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * @author Steve Ebersole
 */
@Converter
public class MyUuidConverter implements AttributeConverter<UUID, String> {
	@Override
	public String convertToDatabaseColumn(UUID attribute) {
		return null;
	}

	@Override
	public UUID convertToEntityAttribute(String dbData) {
		return null;
	}
}
