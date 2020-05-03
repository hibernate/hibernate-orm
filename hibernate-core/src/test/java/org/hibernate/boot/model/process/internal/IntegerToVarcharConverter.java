package org.hibernate.boot.model.process.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * @author Vlad Mihalcea
 */
@Converter( autoApply = true )
class IntegerToVarcharConverter implements AttributeConverter<Integer,String> {
	@Override
	public String convertToDatabaseColumn(Integer attribute) {
		return attribute == null ? null : attribute.toString();
	}

	@Override
	public Integer convertToEntityAttribute(String dbData) {
		return dbData == null ? null : Integer.valueOf( dbData );
	}
}
