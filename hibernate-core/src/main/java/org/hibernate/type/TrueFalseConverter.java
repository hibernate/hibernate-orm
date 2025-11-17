/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import jakarta.persistence.Converter;

/**
 * Handles conversion to/from {@code Boolean} as {@code 'T'} or {@code 'F'}
 *
 * @author Steve Ebersole
 */
@Converter
public class TrueFalseConverter extends CharBooleanConverter {
	/**
	 * Singleton access
	 */
	public static final TrueFalseConverter INSTANCE = new TrueFalseConverter();

	private static final String[] VALUES = {"F", "T"};

	@Override
	protected String[] getValues() {
		return VALUES;
	}

	@Override
	public Boolean toDomainValue(Character relationalForm) {
		if ( relationalForm == null ) {
			return null;
		}
		else {
			return switch ( relationalForm ) {
				case 'T' -> true;
				case 'F' -> false;
				default -> null;
			};
		}
	}

	@Override
	public Character toRelationalValue(Boolean domainForm) {
		if ( domainForm == null ) {
			return null;
		}
		else {
			return domainForm ? 'T' : 'F';
		}
	}
}
