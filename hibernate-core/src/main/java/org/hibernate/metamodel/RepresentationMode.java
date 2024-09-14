/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel;

import org.hibernate.AssertionFailure;

import java.util.Locale;

/**
 * Enumeration of the built-in ways that Hibernate can represent the
 * application's domain model.
 *
 * @author Steve Ebersole
 */
public enum RepresentationMode {
	POJO,
	MAP;

	public String getExternalName() {
		switch (this) {
			case POJO:
				return "pojo";
			case MAP:
				return "dynamic-map";
			default:
				throw new AssertionFailure("Unknown RepresentationMode");
		}
	}

	public static RepresentationMode fromExternalName(String externalName) {
		if ( externalName == null ) {
			return POJO;
		}
		switch ( externalName.toLowerCase(Locale.ROOT) ) {
			case "pojo":
				return POJO;
			case "dynamic-map":
			case "map":
				return MAP;
			default:
				throw new IllegalArgumentException("Unknown RepresentationMode");
		}
	}
}
