/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
