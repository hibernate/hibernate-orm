/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.registrations;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * AttributeConverter associated with {@link Thing2}.
 *
 * @implSpec This converter IS auto-applied
 *
 * @author Steve Ebersole
 */
@Converter( autoApply = true )
public class Thing2Converter implements AttributeConverter<Thing2,String> {
	@Override
	public String convertToDatabaseColumn(Thing2 attribute) {
		return null;
	}

	@Override
	public Thing2 convertToEntityAttribute(String dbData) {
		return null;
	}
}
