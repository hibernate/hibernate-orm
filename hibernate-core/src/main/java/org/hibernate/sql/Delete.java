/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

/**
 * A SQL {@code DELETE} statement.
 *
 * @author Gavin King
 */
@Internal
public class Delete implements RestrictionRenderingContext {
	protected String tableName;
	protected String comment;
	protected final List<Restriction> restrictions = new ArrayList<>();

	private final ParameterMarkerStrategy parameterMarkerStrategy;
	private int parameterCount;

	public Delete(SessionFactoryImplementor factory) {
		this( factory.getParameterMarkerStrategy() );
	}

	public Delete(ParameterMarkerStrategy parameterMarkerStrategy) {
		this.parameterMarkerStrategy = parameterMarkerStrategy;
	}

	public Delete setTableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	public Delete setComment(String comment) {
		this.comment = comment;
		return this;
	}

	@SuppressWarnings("UnusedReturnValue")
	public Delete addColumnRestriction(String columnName) {
		restrictions.add( new ComparisonRestriction( columnName ) );
		return this;
	}

	@SuppressWarnings("UnusedReturnValue")
	public Delete addColumnRestriction(String... columnNames) {
		for ( int i = 0; i < columnNames.length; i++ ) {
			if ( columnNames[i] == null ) {
				continue;
			}
			addColumnRestriction( columnNames[i] );
		}
		return this;
	}

	@SuppressWarnings("UnusedReturnValue")
	public Delete addColumnIsNullRestriction(String columnName) {
		restrictions.add( new NullnessRestriction( columnName ) );
		return this;
	}

	@SuppressWarnings("UnusedReturnValue")
	public Delete addColumnIsNotNullRestriction(String columnName) {
		restrictions.add( new NullnessRestriction( columnName, false ) );
		return this;
	}

	public Delete setVersionColumnName(String versionColumnName) {
		if ( versionColumnName != null ) {
			addColumnRestriction( versionColumnName );
		}
		return this;
	}

	public String toStatementString() {
		final StringBuilder buf = new StringBuilder( tableName.length() + 10 );

		applyComment( buf );
		buf.append( "delete from " ).append( tableName );
		applyRestrictions( buf );

		return buf.toString();
	}

	private void applyComment(StringBuilder buf) {
		if ( comment != null ) {
			buf.append( "/* " ).append( Dialect.escapeComment( comment ) ).append( " */ " );
		}
	}

	private void applyRestrictions(StringBuilder buf) {
		if ( restrictions.isEmpty() ) {
			return;
		}

		buf.append( " where " );

		for ( int i = 0; i < restrictions.size(); i++ ) {
			if ( i > 0 ) {
				buf.append( " and " );
			}
			restrictions.get( i ).render( buf, this );
		}
	}

	@Override
	public String makeParameterMarker() {
		return parameterMarkerStrategy.createMarker( ++parameterCount, null );
	}
}
