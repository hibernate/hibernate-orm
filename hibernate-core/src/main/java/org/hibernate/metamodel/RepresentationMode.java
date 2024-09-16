/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
