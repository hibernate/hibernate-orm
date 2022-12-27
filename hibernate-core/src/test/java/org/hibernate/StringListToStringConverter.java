package org.hibernate;

import jakarta.persistence.AttributeConverter;

import java.util.Arrays;
import java.util.List;

public class StringListToStringConverter implements AttributeConverter<List<String>, String> {
	@Override
	public String convertToDatabaseColumn(List<String> attribute) {
		return attribute.toString();
	}

	@Override
	public List<String> convertToEntityAttribute(String dbData) {
		return Arrays.asList( dbData.split(",") );
	}
}
