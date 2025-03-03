/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.registrations;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * AttributeConverter associated with {@link Thing1}.
 *
 * @implSpec This converter IS NOT auto-applied
 *
 * @author Steve Ebersole
 */
@Converter( autoApply = false )
public class Thing1Converter implements AttributeConverter<Thing1,String> {
	@Override
	public String convertToDatabaseColumn(Thing1 attribute) {
		return null;
	}

	@Override
	public Thing1 convertToEntityAttribute(String dbData) {
		return null;
	}
}
