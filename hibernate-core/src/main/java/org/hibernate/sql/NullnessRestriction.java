/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql;

import org.hibernate.Internal;

/**
 * Nullness restriction - IS (NOT)? NULL
 *
 * @author Steve Ebersole
 */
@Internal
public class NullnessRestriction implements Restriction {
	private final String columnName;
	private final boolean affirmative;

	public NullnessRestriction(String columnName) {
		this( columnName, true );
	}

	public NullnessRestriction(String columnName, boolean affirmative) {
		this.columnName = columnName;
		this.affirmative = affirmative;
	}

	@Override
	public void render(StringBuilder sqlBuffer, RestrictionRenderingContext context) {
		sqlBuffer.append( columnName );
		if ( affirmative ) {
			sqlBuffer.append( " is null" );
		}
		else {
			sqlBuffer.append( " is not null" );
		}
	}
}
