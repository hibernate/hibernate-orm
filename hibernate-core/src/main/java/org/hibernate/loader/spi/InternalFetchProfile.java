/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.spi;

import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public enum InternalFetchProfile {
	MERGE( "merge" ),
	REFRESH( "refresh" );

	private final String legacyName;

	InternalFetchProfile(String legacyName) {
		this.legacyName = legacyName;
	}

	public String getLegacyName() {
		return legacyName;
	}

	public static InternalFetchProfile fromLegacyName(String legacyName) {
		if ( StringHelper.isEmpty( legacyName ) ) {
			return null;
		}

		if ( MERGE.legacyName.equalsIgnoreCase( legacyName ) ) {
			return MERGE;
		}

		if ( REFRESH.legacyName.equalsIgnoreCase( legacyName ) ) {
			return REFRESH;
		}

		throw new IllegalArgumentException(
				"Passed name [" + legacyName + "] not recognized as a legacy internal fetch profile name; " +
						"supported values include: 'merge' and 'refresh'"
		);
	}
}
