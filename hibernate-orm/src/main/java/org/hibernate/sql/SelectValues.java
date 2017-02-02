/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql;

import java.util.ArrayList;
import java.util.HashSet;

import org.hibernate.dialect.Dialect;

import org.jboss.logging.Logger;

/**
 * Models a SELECT values lists.  Eventually, rather than Strings, pass in the Column/Formula representations (something
 * like {@link org.hibernate.sql.ordering.antlr.ColumnReference}/{@link org.hibernate.sql.ordering.antlr.FormulaReference}
 *
 * @author Steve Ebersole
 */
public class SelectValues {
	private static final Logger log = Logger.getLogger( SelectValues.class );

	private static class SelectValue {
		private final String qualifier;
		private final String value;
		private final String alias;

		private SelectValue(String qualifier, String value, String alias) {
			this.qualifier = qualifier;
			this.value = value;
			this.alias = alias;
		}
	}

	private final Dialect dialect;
	private final ArrayList<SelectValue> selectValueList = new ArrayList<SelectValue>();

	public SelectValues(Dialect dialect) {
		this.dialect = dialect;
	}

	public SelectValues addColumns(String qualifier, String[] columnNames, String[] columnAliases) {
		for ( int i = 0; i < columnNames.length; i++ ) {
			if ( columnNames[i] != null ) {
				addColumn( qualifier, columnNames[i], columnAliases[i] );
			}
		}
		return this;
	}

	public SelectValues addColumn(String qualifier, String columnName, String columnAlias) {
		selectValueList.add( new SelectValue( qualifier, columnName, columnAlias ) );
		return this;
	}

	public SelectValues addParameter(int jdbcTypeCode, int length) {
		final String selectExpression = dialect.requiresCastingOfParametersInSelectClause()
				? dialect.cast( "?", jdbcTypeCode, length )
				: "?";
		selectValueList.add( new SelectValue( null, selectExpression, null ) );
		return this;
	}

	public SelectValues addParameter(int jdbcTypeCode, int precision, int scale) {
		final String selectExpression = dialect.requiresCastingOfParametersInSelectClause()
				? dialect.cast( "?", jdbcTypeCode, precision, scale )
				: "?";
		selectValueList.add( new SelectValue( null, selectExpression, null ) );
		return this;
	}

	public String render() {
		final StringBuilder buf = new StringBuilder( selectValueList.size() * 10 );
		final HashSet<String> uniqueAliases = new HashSet<String>();
		boolean firstExpression = true;
		for ( SelectValue selectValue : selectValueList ) {
			if ( selectValue.alias != null ) {
				if ( ! uniqueAliases.add( selectValue.alias ) ) {
					log.debug( "Skipping select-value with non-unique alias" );
					continue;
				}
			}

			if ( firstExpression ) {
				firstExpression = false;
			}
			else {
				buf.append( ", " );
			}

			if ( selectValue.qualifier != null ) {
				buf.append( selectValue.qualifier ).append( '.' );
			}
			buf.append( selectValue.value );
			if ( selectValue.alias != null ) {
				buf.append( " as " ).append( selectValue.alias );
			}
		}
		return buf.toString();
	}
}
