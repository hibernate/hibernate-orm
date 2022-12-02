/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.tuple.InDatabaseGenerator;

/**
 * An SQL {@code INSERT} statement
 *
 * @author Gavin King
 */
@Internal
public class Insert {
	private final Dialect dialect;

	protected String tableName;
	protected String comment;

	protected Map<String,String> columns = new LinkedHashMap<>();
	protected Map<String,String> lobColumns;

	public Insert(Dialect dialect) {
		this.dialect = dialect;
	}

	protected Dialect getDialect() {
		return dialect;
	}

	public Insert setComment(String comment) {
		this.comment = comment;
		return this;
	}

	public Map<String, String> getColumns() {
		return columns;
	}

	public Insert addColumn(String columnName) {
		return addColumn( columnName, "?" );
	}

	public Insert addColumns(String[] columnNames) {
		for ( String columnName : columnNames ) {
			addColumn( columnName );
		}
		return this;
	}

	public Insert addColumns(String[] columnNames, boolean[] insertable) {
		for ( int i=0; i<columnNames.length; i++ ) {
			if ( insertable[i] ) {
				addColumn( columnNames[i] );
			}
		}
		return this;
	}

	public Insert addColumns(String[] columnNames, boolean[] insertable, String[] valueExpressions) {
		for ( int i=0; i<columnNames.length; i++ ) {
			if ( insertable[i] ) {
				addColumn( columnNames[i], valueExpressions[i] );
			}
		}
		return this;
	}

	public Insert addColumn(String columnName, String valueExpression) {
		columns.put( columnName, valueExpression );
		return this;
	}

	public void addLobColumn(String columnName, String valueExpression) {
		assert dialect.forceLobAsLastValue();
		if ( lobColumns == null ) {
			lobColumns = new HashMap<>();
		}
		lobColumns.put( columnName, valueExpression );
	}

	public Insert addIdentityColumn(String columnName) {
		final IdentityColumnSupport identityColumnSupport = dialect.getIdentityColumnSupport();
		if ( identityColumnSupport.hasIdentityInsertKeyword() ) {
			addColumn( columnName, identityColumnSupport.getIdentityInsertString() );
		}
		return this;
	}

	public Insert addGeneratedColumns(String[] columnNames, InDatabaseGenerator generator) {
		if ( generator.referenceColumnsInSql( dialect ) ) {
			String[] columnValues = generator.getReferencedColumnValues( dialect );
			if ( columnNames.length != columnValues.length ) {
				throw new MappingException("wrong number of generated columns"); //TODO!
			}
			for ( int i = 0; i < columnNames.length; i++ ) {
				addColumn( columnNames[i], columnValues[i] );
			}
		}
		return this;
	}

	public Insert setTableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	public String toStatementString() {
		StringBuilder buf = new StringBuilder( columns.size()*15 + tableName.length() + 10 );
		if ( comment != null ) {
			buf.append( "/* " ).append( Dialect.escapeComment( comment ) ).append( " */ " );
		}

		buf.append("insert into ").append(tableName);

		if ( columns.size()==0 && lobColumns == null ) {
			if ( dialect.supportsNoColumnsInsert() ) {
				buf.append( ' ' ).append( dialect.getNoColumnsInsertString() );
			}
			else {
				throw new MappingException(
						String.format(
								"The INSERT statement for table [%s] contains no column, and this is not supported by [%s]",
								tableName,
								dialect
						)
				);
			}
		}
		else {
			buf.append( " (" );
			renderColumnsClause( buf );
			buf.append( ") values (" );
			renderValuesClause( buf );
			buf.append(')');
		}
		return buf.toString();
	}

	private void renderColumnsClause(StringBuilder buf) {
		final Iterator<String> itr = columns.keySet().iterator();
		while ( itr.hasNext() ) {
			buf.append( itr.next() );
			if ( itr.hasNext() || lobColumns != null ) {
				buf.append( ", " );
			}
		}

		if ( lobColumns != null ) {
			final Iterator<String> columnsAtEndItr = lobColumns.keySet().iterator();
			while ( columnsAtEndItr.hasNext() ) {
				buf.append( columnsAtEndItr.next() );
				if ( columnsAtEndItr.hasNext() ) {
					buf.append( ", " );
				}
			}
		}
	}

	private void renderValuesClause(StringBuilder buf) {
		final Iterator<String> itr = columns.values().iterator();
		while ( itr.hasNext() ) {
			buf.append( itr.next() );
			if ( itr.hasNext() || lobColumns != null ) {
				buf.append( ", " );
			}
		}

		if ( lobColumns != null ) {
			final Iterator<String> columnsAtEndItr = lobColumns.values().iterator();
			while ( columnsAtEndItr.hasNext() ) {
				buf.append( columnsAtEndItr.next() );
				if ( columnsAtEndItr.hasNext() ) {
					buf.append( ", " );
				}
			}
		}
	}
}
