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
