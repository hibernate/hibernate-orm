/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
