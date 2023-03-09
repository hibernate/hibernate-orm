/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.JdbcParameterRenderer;

/**
 * A SQL {@code DELETE} statement.
 *
 * @author Gavin King
 */
@Internal
public class Delete {

	protected String tableName;
	protected String comment;

	protected final List<String> whereFragments = new ArrayList<>();

	private final JdbcParameterRenderer jdbcParameterRenderer;
	private final boolean standardParamRendering;
	private int parameterCount;

	public Delete(SessionFactoryImplementor factory) {
		this( factory.getServiceRegistry().getService( JdbcParameterRenderer.class ) );
	}

	public Delete(JdbcParameterRenderer jdbcParameterRenderer) {
		this.jdbcParameterRenderer = jdbcParameterRenderer;
		this.standardParamRendering = JdbcParameterRenderer.isStandardRenderer( jdbcParameterRenderer );
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
		final String paramMarker = jdbcParameterRenderer.renderJdbcParameter( ++parameterCount, null );
		this.whereFragments.add( columnName + "=" + paramMarker );
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
	public Delete addColumnNullnessRestriction(String columnName) {
		addColumnNullnessRestriction( columnName, false );
		return this;
	}

	@SuppressWarnings("UnusedReturnValue")
	public Delete addColumnNullnessRestriction(String columnName, boolean negate) {
		final String fragment = negate
				? columnName + " is not null"
				: columnName + " is null";
		whereFragments.add( fragment );
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

	private void applyRestrictions(StringBuilder buf) {
		if ( whereFragments.isEmpty() ) {
			return;
		}

		buf.append( " where " );

		for ( int i = 0; i < whereFragments.size(); i++ ) {
			if ( i > 0 ) {
				buf.append( " and " );
			}
			buf.append( whereFragments.get(i) );
		}
	}

	private void applyComment(StringBuilder buf) {
		if ( comment != null ) {
			buf.append( "/* " ).append( Dialect.escapeComment( comment ) ).append( " */ " );
		}
	}

}
