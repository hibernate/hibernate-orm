/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel;

import java.util.Locale;

import org.hibernate.EntityMode;

/**
 * Enumeration of the built-in ways that Hibernate can represent the
 * application's domain model.
 *
 * @author Steve Ebersole
 */
public enum RepresentationMode {
	POJO( "pojo", EntityMode.POJO ),
	MAP( "map", "dynamic-map", EntityMode.MAP );

	private final String externalName;
	private final String alternativeExternalName;

	private final EntityMode legacyEntityMode;

	RepresentationMode(String externalName, EntityMode legacyEntityMode) {
		this ( externalName, null, legacyEntityMode );
	}

	RepresentationMode(String externalName, String alternativeExternalName, EntityMode legacyEntityMode) {
		this.externalName = externalName;
		this.alternativeExternalName = alternativeExternalName;
		this.legacyEntityMode = legacyEntityMode;
	}

	public String getExternalName() {
		return externalName;
	}

	/**
	 * @deprecated {@link EntityMode} is deprecated itself
	 */
	@Deprecated
	public EntityMode getLegacyEntityMode() {
		return legacyEntityMode;
	}

	public static RepresentationMode fromExternalName(String externalName) {
		if ( externalName == null ) {
			return POJO;
		}

		if ( MAP.externalName.equalsIgnoreCase( externalName )
				|| MAP.alternativeExternalName.equalsIgnoreCase( externalName ) ) {
			return MAP;
		}

		if ( POJO.externalName.equalsIgnoreCase( externalName ) ) {
			return POJO;
		}

		return valueOf( externalName.toUpperCase( Locale.ROOT ) );
	}
}
