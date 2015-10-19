package org.hibernate.test.converter.map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * @author Janario Oliveira
 */
@Converter
public class ExplicitEnumMapKeyConverter implements AttributeConverter<EnumMapKey, String> {
	@Override
	public String convertToDatabaseColumn(EnumMapKey attribute) {
		return attribute == null ? null : attribute.name().substring( attribute.name().length() - 1 );
	}

	@Override
	public EnumMapKey convertToEntityAttribute(String dbData) {
		return dbData == null ? null : EnumMapKey.valueOf( "VALUE_" + dbData );
	}
}
