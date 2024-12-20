/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public enum CascadingFetchProfile {
	MERGE( "merge", CascadingActions.MERGE ),
	REFRESH( "refresh", CascadingActions.REFRESH );

	private final String legacyName;
	private final CascadingAction cascadingAction;

	CascadingFetchProfile(String legacyName, CascadingAction cascadingAction) {
		this.legacyName = legacyName;
		this.cascadingAction = cascadingAction;
	}

	public String getLegacyName() {
		return legacyName;
	}

	public CascadingAction getCascadingAction() {
		return cascadingAction;
	}

	public static CascadingFetchProfile fromLegacyName(String legacyName) {
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
