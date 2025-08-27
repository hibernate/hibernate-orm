/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.gambit;

/**
 * @author Steve Ebersole
 */
public enum EnumValue {
	ONE( "first" ),
	TWO( "second" ),
	THREE( "third" );

	private final String code;

	EnumValue(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public static EnumValue fromCode(String code) {
		if ( code == null || code.isEmpty() ) {
			return null;
		}

		switch ( code ) {
			case "first": {
				return ONE;
			}
			case "second": {
				return TWO;
			}
			case "third": {
				return THREE;
			}
			default: {
				throw new RuntimeException( "Could not convert enum code : " + code );
			}
		}
	}
}
