/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;

import java.util.Locale;

import jakarta.persistence.AttributeConverter;

/**
 * @author Steve Ebersole
 */
public class ToLowerConverter implements AttributeConverter<String,String> {
	@Override
	public String convertToDatabaseColumn(String value) {
		if ( value == null ) {
			return null;
		}

		return value.toLowerCase( Locale.ROOT );
	}

	@Override
	public String convertToEntityAttribute(String value) {
		if ( value == null ) {
			return null;
		}

		assert value.toLowerCase( Locale.ROOT ).equals( value );

		return value;
	}
}
