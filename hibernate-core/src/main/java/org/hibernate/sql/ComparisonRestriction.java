/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql;

import org.hibernate.Internal;

/**
 * A binary-comparison restriction
 *
 * @author Steve Ebersole
 */
@Internal
public class ComparisonRestriction implements Restriction {
	private final String lhs;
	private final String operator;
	private final String rhs;

	public ComparisonRestriction(String lhs) {
		this( lhs, "?" );
	}

	public ComparisonRestriction(String lhs, String rhs) {
		this( lhs, "=", rhs );
	}

	public ComparisonRestriction(String lhs, String operator, String rhs) {
		this.lhs = lhs;
		this.operator = operator;
		this.rhs = rhs;
	}

	@Override
	public void render(StringBuilder sqlBuffer, RestrictionRenderingContext context) {
		sqlBuffer.append( lhs );
		sqlBuffer.append( operator );

		if ( "?".equals( rhs ) ) {
			sqlBuffer.append( context.makeParameterMarker() );
		}
		else {
			sqlBuffer.append( rhs );
		}
	}
}
