package org.hibernate.test.converter.map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * @author Janario Oliveira
 */
@Converter
public class CustomColorTypeConverter implements AttributeConverter<ColorType, String> {
	@Override
	public String convertToDatabaseColumn(ColorType attribute) {
		return attribute == null ? null : "COLOR-" + attribute.toExternalForm();
	}

	@Override
	public ColorType convertToEntityAttribute(String dbData) {
		return dbData == null ? null : ColorType.fromExternalForm( dbData.substring( 6 ) );
	}
}
