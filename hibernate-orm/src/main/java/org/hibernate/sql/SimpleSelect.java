/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.Dialect;

/**
 * An SQL <tt>SELECT</tt> statement with no table joins
 *
 * @author Gavin King
 */
public class SimpleSelect {

	public SimpleSelect(Dialect dialect) {
		this.dialect = dialect;
	}

	//private static final Alias DEFAULT_ALIAS = new Alias(10, null);

	private String tableName;
	private String orderBy;
	private Dialect dialect;
	private LockOptions lockOptions = new LockOptions( LockMode.READ );
	private String comment;

	private List<String> columns = new ArrayList<String>();
	private Map<String, String> aliases = new HashMap<String, String>();
	private List<String> whereTokens = new ArrayList<String>();

	public SimpleSelect addColumns(String[] columnNames, String[] columnAliases) {
		for ( int i = 0; i < columnNames.length; i++ ) {
			if ( columnNames[i] != null ) {
				addColumn( columnNames[i], columnAliases[i] );
			}
		}
		return this;
	}

	public SimpleSelect addColumns(String[] columns, String[] aliases, boolean[] ignore) {
		for ( int i = 0; i < ignore.length; i++ ) {
			if ( !ignore[i] && columns[i] != null ) {
				addColumn( columns[i], aliases[i] );
			}
		}
		return this;
	}

	public SimpleSelect addColumns(String[] columnNames) {
		for ( String columnName : columnNames ) {
			if ( columnName != null ) {
				addColumn( columnName );
			}
		}
		return this;
	}

	public SimpleSelect addColumn(String columnName) {
		columns.add( columnName );
		//aliases.put( columnName, DEFAULT_ALIAS.toAliasString(columnName) );
		return this;
	}

	public SimpleSelect addColumn(String columnName, String alias) {
		columns.add( columnName );
		aliases.put( columnName, alias );
		return this;
	}

	public SimpleSelect setTableName(String tableName) {
		this.tableName = tableName;
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

	public SimpleSelect addWhereToken(String token) {
		if (token != null ) {
			if (!whereTokens.isEmpty()) {
				and();
			}
			whereTokens.add( token );
		}
		return this;
	}

	private void and() {
		if ( whereTokens.size() > 0 ) {
			whereTokens.add( "and" );
		}
	}

	public SimpleSelect addCondition(String lhs, String op, String rhs) {
		and();
		whereTokens.add( lhs + ' ' + op + ' ' + rhs );
		return this;
	}

	public SimpleSelect addCondition(String lhs, String condition) {
		and();
		whereTokens.add( lhs + ' ' + condition );
		return this;
	}

	public SimpleSelect addCondition(String[] lhs, String op, String[] rhs) {
		for ( int i = 0; i < lhs.length; i++ ) {
			addCondition( lhs[i], op, rhs[i] );
		}
		return this;
	}

	public SimpleSelect addCondition(String[] lhs, String condition) {
		for ( String lh : lhs ) {
			if ( lh != null ) {
				addCondition( lh, condition );
			}
		}
		return this;
	}

	public String toStatementString() {
		StringBuilder buf = new StringBuilder(
				columns.size() * 10 +
						tableName.length() +
						whereTokens.size() * 10 +
						10
		);

		if ( comment != null ) {
			buf.append( "/* " ).append( comment ).append( " */ " );
		}

		buf.append( "select " );
		Set<String> uniqueColumns = new HashSet<String>();
		Iterator<String> iter = columns.iterator();
		boolean appendComma = false;
		while ( iter.hasNext() ) {
			String col = iter.next();
			String alias = aliases.get( col );
			if ( uniqueColumns.add( alias == null ? col : alias ) ) {
				if ( appendComma ) {
					buf.append( ", " );
				}
				buf.append( col );
				if ( alias != null && !alias.equals( col ) ) {
					buf.append( " as " )
							.append( alias );
				}
				appendComma = true;
			}
		}

		buf.append( " from " )
				.append( dialect.appendLockHint( lockOptions, tableName ) );

		if ( whereTokens.size() > 0 ) {
			buf.append( " where " )
					.append( toWhereClause() );
		}

		if ( orderBy != null ) {
			buf.append( orderBy );
		}

		if ( lockOptions != null ) {
			buf = new StringBuilder(dialect.applyLocksToSql( buf.toString(), lockOptions, null ) );
		}

		return dialect.transformSelectString( buf.toString() );
	}

	public String toWhereClause() {
		StringBuilder buf = new StringBuilder( whereTokens.size() * 5 );
		Iterator<String> iter = whereTokens.iterator();
		while ( iter.hasNext() ) {
			buf.append( iter.next() );
			if ( iter.hasNext() ) {
				buf.append( ' ' );
			}
		}
		return buf.toString();
	}

	public SimpleSelect setOrderBy(String orderBy) {
		this.orderBy = orderBy;
		return this;
	}

	public SimpleSelect setComment(String comment) {
		this.comment = comment;
		return this;
	}

}
