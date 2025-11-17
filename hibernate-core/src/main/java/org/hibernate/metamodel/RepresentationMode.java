/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel;


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
		return switch ( this ) {
			case POJO -> "pojo";
			case MAP -> "dynamic-map";
		};
	}

	public static RepresentationMode fromExternalName(String externalName) {
		return externalName == null
				? POJO
				: switch ( externalName.toLowerCase( Locale.ROOT ) ) {
					case "pojo" -> POJO;
					case "dynamic-map", "map" -> MAP;
					default -> throw new IllegalArgumentException( "Unknown RepresentationMode" );
				};
	}
}
