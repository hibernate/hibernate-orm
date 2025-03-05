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

		switch ( relationalForm ) {
			case 'Y':
				return true;
			case 'N':
				return false;
		}

		return null;
	}

	@Override
	public Character toRelationalValue(Boolean domainForm) {
		if ( domainForm == null ) {
			return null;
		}

		return domainForm ? 'Y' : 'N';
	}
}
