/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.sql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.dialect.Dialect;

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
