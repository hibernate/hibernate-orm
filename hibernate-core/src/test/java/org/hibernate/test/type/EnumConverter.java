package org.hibernate.test.type;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * @author Oleksander
 */
@Converter(autoApply = true)
public class EnumConverter implements AttributeConverter<ConvertibleEnum, String> {

	@Override
	public String convertToDatabaseColumn(ConvertibleEnum attribute) {
		return attribute.convertToString();
	}

	@Override
	public ConvertibleEnum convertToEntityAttribute(String dbData) {
		return ConvertibleEnum.valueOf( dbData );
	}
}
