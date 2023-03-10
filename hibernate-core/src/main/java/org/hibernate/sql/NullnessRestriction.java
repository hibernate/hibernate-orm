/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql;

import org.hibernate.Internal;

/**
 * Nullness restriction - is [not] null
 *
 * @author Steve Ebersole
 */
@Internal
public class NullnessRestriction implements Restriction {
	private final String columnName;
	private final boolean negated;

	public NullnessRestriction(String columnName) {
		this( columnName, false );
	}

	public NullnessRestriction(String columnName, boolean negated) {
		this.columnName = columnName;
		this.negated = negated;
	}

	@Override
	public void render(StringBuilder sqlBuffer, RestrictionRenderingContext context) {
		sqlBuffer.append( columnName );
		if ( negated ) {
			sqlBuffer.append( " is not null" );
		}
		else {
			sqlBuffer.append( " is null" );
		}
	}
}
