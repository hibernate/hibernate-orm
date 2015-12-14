/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.immutable;

import javax.persistence.AttributeConverter;

/**
 * Created by soldier on 12.04.16.
 */
public class CaptionConverter implements AttributeConverter<Caption, String> {

	@Override
	public String convertToDatabaseColumn(Caption attribute) {
		return attribute.getText();
	}

	@Override
	public Caption convertToEntityAttribute(String dbData) {
		return new Caption( dbData );
	}
}
