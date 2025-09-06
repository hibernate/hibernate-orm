/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import jakarta.persistence.Converter;

/**
 * Handles conversion to/from {@code Boolean} as {@code 'Y'} or {@code 'N'}
 *
 * @author Steve Ebersole
 */
@Converter
public class YesNoConverter extends CharBooleanConverter {
	/**
	 * Singleton access
	 */
	public static final YesNoConverter INSTANCE = new YesNoConverter();

	private static final String[] VALUES = {"N", "Y"};

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
				case 'Y' -> true;
				case 'N' -> false;
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
			return domainForm ? 'Y' : 'N';
		}
	}
}
