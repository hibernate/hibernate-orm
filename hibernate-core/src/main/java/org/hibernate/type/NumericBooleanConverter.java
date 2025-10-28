/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.BooleanJavaType;
import org.hibernate.type.descriptor.java.IntegerJavaType;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.Converter;

/**
 * Handles conversion to/from {@code Boolean} as {@code 0} (false) or {@code 1} (true)
 *
 * @author Steve Ebersole
 */
@Converter
public class NumericBooleanConverter implements StandardBooleanConverter<Integer> {
	/**
	 * Singleton access
	 */
	public static final NumericBooleanConverter INSTANCE = new NumericBooleanConverter();

	@Override
	public Integer convertToDatabaseColumn(Boolean attribute) {
		return toRelationalValue( attribute );
	}

	@Override
	public Boolean convertToEntityAttribute(Integer dbData) {
		return toDomainValue( dbData );
	}

	@Override
	public Boolean toDomainValue(Integer relationalForm) {
		if ( relationalForm == null ) {
			return null;
		}
		else {
			return switch ( relationalForm ) {
				case 0 -> false;
				case 1 -> true;
				default -> null;
			};
		}
	}

	@Override
	public Integer toRelationalValue(Boolean domainForm) {
		if ( domainForm == null ) {
			return null;
		}
		else {
			return domainForm ? 1 : 0;
		}
	}

	@Override
	public JavaType<Boolean> getDomainJavaType() {
		return BooleanJavaType.INSTANCE;
	}

	@Override
	public JavaType<Integer> getRelationalJavaType() {
		return IntegerJavaType.INSTANCE;
	}
}
