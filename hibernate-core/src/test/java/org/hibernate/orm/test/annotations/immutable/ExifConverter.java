/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.immutable;

import java.util.Collections;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 *
 * @author soldierkam
 */
@Converter(autoApply=true)
public class ExifConverter implements AttributeConverter<String, Exif> {

	@Override
	public Exif convertToDatabaseColumn(String attribute) {
		return new Exif( Collections.singletonMap( "fakeAttr", attribute ) );
	}

	@Override
	public String convertToEntityAttribute(Exif dbData) {
		return dbData.getAttributes().get( "fakeAttr" );
	}

}
