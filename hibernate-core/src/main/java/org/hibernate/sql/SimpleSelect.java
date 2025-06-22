/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

/**
 * A SQL {@code SELECT} statement with no table joins.
 *
 * @author Gavin King
 */
@Internal
public class SimpleSelect implements RestrictionRenderingContext {
	protected String tableName;
	protected String orderBy;
	protected String comment;

	protected List<String> columns = new ArrayList<>();
	protected Map<String, String> aliases = new HashMap<>();
	protected List<Restriction> restrictions = new ArrayList<>();

	protected LockOptions lockOptions = new LockOptions( LockMode.READ );

	private final Dialect dialect;
	private final ParameterMarkerStrategy parameterMarkerStrategy;
	private int parameterCount;

	public SimpleSelect(final SessionFactoryImplementor factory) {
		final JdbcServices jdbcServices = factory.getJdbcServices();
		this.dialect = jdbcServices.getDialect();
		this.parameterMarkerStrategy = jdbcServices.getParameterMarkerStrategy();
	}

	@Override
	public String makeParameterMarker() {
		return parameterMarkerStrategy.createMarker( ++parameterCount, null );
	}

	/**
	 * Sets the name of the table we are selecting from
	 */
	public SimpleSelect setTableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	/**
	 * Adds selections
	 */
	public SimpleSelect addColumns(String[] columnNames) {
		for ( String columnName : columnNames ) {
			if ( columnName != null ) {
				addColumn( columnName );
			}
		}
		return this;
	}

	/**
	 * Adds a selection
	 */
	public SimpleSelect addColumn(String columnName) {
		columns.add( columnName );
		return this;
	}

	/**
	 * Adds a selection, with an alias
	 */
	public SimpleSelect addColumn(String columnName, String alias) {
		columns.add( columnName );
		aliases.put( columnName, alias );
		return this;
	}

	/**
	 * Appends a complete {@linkplain org.hibernate.annotations.SQLRestriction where} condition.
	 * The {@code condition} is added as-is.
	 */
	public SimpleSelect addWhereToken(String condition) {
		if ( condition != null ) {
			restrictions.add( new CompleteRestriction( condition ) );
		}
		return this;
	}

	/**
	 * Appends a restriction comparing the {@code columnName} for equality with a parameter
	 *
	 * @see #addRestriction(String, ComparisonRestriction.Operator, String)
	 */
	public SimpleSelect addRestriction(String columnName) {
		restrictions.add( new ComparisonRestriction( columnName ) );
		return this;
	}

	/**
	 * Appends a restriction based on the comparison between {@code lhs} and {@code rhs}.
	 * <p/>
	 * The {@code rhs} is checked for parameter marker and processed via {@link ParameterMarkerStrategy}
	 * if needed.
	 */
	public SimpleSelect addRestriction(String lhs, ComparisonRestriction.Operator op, String rhs) {
		restrictions.add( new ComparisonRestriction( lhs, op, rhs ) );
		return this;
	}

	/**
	 * Appends a restriction comparing each name in {@code columnNames} for equality with a parameter
	 *
	 * @see #addRestriction(String)
	 */
	public SimpleSelect addRestriction(String... columnNames) {
		for ( int i = 0; i < columnNames.length; i++ ) {
			if ( columnNames[i] != null ) {
				addRestriction( columnNames[i] );
			}
		}
		return this;
	}

	public SimpleSelect setLockOptions(LockOptions lockOptions) {
		LockOptions.copy( lockOptions, this.lockOptions );
		return this;
	}

	public SimpleSelect setLockMode(LockMode lockMode) {
		this.lockOptions.setLockMode( lockMode );
		return this;
	}

	public SimpleSelect setOrderBy(String orderBy) {
		this.orderBy = orderBy;
		return this;
	}

	public SimpleSelect setComment(String comment) {
		this.comment = comment;
		return this;
	}

	public String toStatementString() {
		final StringBuilder buf = new StringBuilder(
				columns.size() * 10 +
						tableName.length() +
						restrictions.size() * 10 +
						10
		);

		applyComment( buf );
		applySelectClause( buf );
		applyFromClause( buf );
		applyWhereClause( buf );
		applyOrderBy( buf );

		final String selectString = (lockOptions != null)
				? dialect.applyLocksToSql( buf.toString(), lockOptions, null )
				: buf.toString();

		return dialect.transformSelectString( selectString );
	}

	private void applyComment(StringBuilder buf) {
		if ( comment != null ) {
			buf.append( "/* " ).append( Dialect.escapeComment( comment ) ).append( " */ " );
		}
	}

	private void applySelectClause(StringBuilder buf) {
		buf.append( "select " );

		boolean appendComma = false;
		final Set<String> uniqueColumns = new HashSet<>();
		for ( int i = 0; i < columns.size(); i++ ) {
			final String col = columns.get( i );
			final String alias = aliases.get( col );

			if ( uniqueColumns.add( alias == null ? col : alias ) ) {
				if ( appendComma ) {
					buf.append( ", " );
				}
				buf.append( col );
				if ( alias != null && !alias.equals( col ) ) {
					buf.append( " as " ).append( alias );
				}
				appendComma = true;
			}
		}
	}

	private void applyFromClause(StringBuilder buf) {
		buf.append( " from " ).append( dialect.appendLockHint( lockOptions, tableName ) );
	}

	private void applyWhereClause(StringBuilder buf) {
		if ( restrictions.isEmpty() ) {
			return;
		}

		buf.append( " where " );

		for ( int i = 0; i < restrictions.size(); i++ ) {
			if ( i > 0 ) {
				buf.append( " and " );
			}

			final Restriction restriction = restrictions.get( i );
			restriction.render( buf, this );
		}
	}

	private void applyOrderBy(StringBuilder buf) {
		if ( orderBy != null ) {
			buf.append( ' ' ).append( orderBy );
		}
	}

}
