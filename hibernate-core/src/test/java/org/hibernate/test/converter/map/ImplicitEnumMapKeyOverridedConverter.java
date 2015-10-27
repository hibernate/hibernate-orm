package org.hibernate.test.converter.map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * @author Janario Oliveira
 */
@Converter
public class ImplicitEnumMapKeyOverridedConverter implements AttributeConverter<ImplicitEnumMapKey, String> {
	@Override
	public String convertToDatabaseColumn(ImplicitEnumMapKey attribute) {
		return attribute == null ? null :
				( "O" + attribute.name().substring( attribute.name().length() - 1 ) );
	}

	@Override
	public ImplicitEnumMapKey convertToEntityAttribute(String dbData) {
		return dbData == null ? null : ImplicitEnumMapKey.valueOf( "VALUE_" + dbData.substring( 1 ) );
	}
}
