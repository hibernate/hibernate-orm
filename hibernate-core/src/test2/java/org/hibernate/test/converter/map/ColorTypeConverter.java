/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter.map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * @author Steve Ebersole
 */
@Converter(autoApply = true)
public class ColorTypeConverter implements AttributeConverter<ColorType, String> {

	@Override
	public String convertToDatabaseColumn(ColorType attribute) {
		return attribute == null ? null : attribute.toExternalForm();
	}

	@Override
	public ColorType convertToEntityAttribute(String dbData) {
		return dbData == null ? null : ColorType.fromExternalForm( dbData );
	}
}