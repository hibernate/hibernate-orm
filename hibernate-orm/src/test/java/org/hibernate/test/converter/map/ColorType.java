/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter.map;

/**
 * @author Steve Ebersole
 * an enum-like class (converters are technically not allowed to apply to enums)
 */
public class ColorType {
	public static ColorType BLUE = new ColorType( "blue" );
	public static ColorType RED = new ColorType( "red" );
	public static ColorType YELLOW = new ColorType( "yellow" );

	private final String color;

	public ColorType(String color) {
		this.color = color;
	}

	public String toExternalForm() {
		return color;
	}

	public static ColorType fromExternalForm(String color) {
		if ( BLUE.color.equals( color ) ) {
			return BLUE;
		}
		else if ( RED.color.equals( color ) ) {
			return RED;
		}
		else if ( YELLOW.color.equals( color ) ) {
			return YELLOW;
		}
		else {
			throw new RuntimeException( "Unknown color : " + color );
		}
	}
}