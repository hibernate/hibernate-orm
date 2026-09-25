package org.hibernate.orm.integrationtest.java.module.test.annotation;

import jakarta.persistence.AttributeConverter;

public class StubConverter implements AttributeConverter<StubConvertibleType, String> {
	@Override
	public String convertToDatabaseColumn(StubConvertibleType attribute) {
		return attribute == null ? null : attribute.value;
	}

	@Override
	public StubConvertibleType convertToEntityAttribute(String dbData) {
		return dbData == null ? null : new StubConvertibleType( dbData );
	}
}
