/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	private final Operator operator;
	private final String rhs;

	public ComparisonRestriction(String lhs) {
		this( lhs, "?" );
	}

	public ComparisonRestriction(String lhs, String rhs) {
		this( lhs, Operator.EQ, rhs );
	}

	public ComparisonRestriction(String lhs, Operator operator, String rhs) {
		this.lhs = lhs;
		this.operator = operator;
		this.rhs = rhs;
	}

	@Override
	public void render(StringBuilder sqlBuffer, RestrictionRenderingContext context) {
		sqlBuffer.append( lhs );
		sqlBuffer.append( operator.getSqlText() );

		if ( "?".equals( rhs ) ) {
			sqlBuffer.append( context.makeParameterMarker() );
		}
		else {
			sqlBuffer.append( rhs );
		}
	}

	public enum Operator {
		EQ( "=" ),
		NE( "<>" )
		;

		private final String sqlText;

		Operator(String sqlText) {
			this.sqlText = sqlText;
		}

		public String getSqlText() {
			return sqlText;
		}
	}
}
