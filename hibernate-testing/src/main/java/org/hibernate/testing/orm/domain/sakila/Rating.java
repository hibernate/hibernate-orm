/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.sakila;

import java.util.Locale;

/**
 * @author Nathan Xu
 */
public enum Rating {
	G("G"),
	PG("PG"),
	PG_13("PG-13"),
	R("R"),
	NC_17("NC-17");

	private final String text;
	Rating(String text) {
		this.text = text;
	}

	public static Rating fromText(String text) {
		switch(text.toUpperCase( Locale.ROOT )) {
			case "G":
				return G;
			case "PG":
				return PG;
			case "PG-13":
				return PG_13;
			case "R":
				return R;
			case "NC-17":
				return NC_17;
			default:
				throw new IllegalArgumentException( "unknown Rating text: " + text );
		}
	}

	@Override
	public String toString() {
		return text;
	}
}
