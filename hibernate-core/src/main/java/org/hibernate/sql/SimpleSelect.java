/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
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
	private LockOptions lockOptions = new LockOptions( LockMode.READ);
	private String comment;

	private List columns = new ArrayList();
	private Map aliases = new HashMap();
	private List whereTokens = new ArrayList();

	public SimpleSelect addColumns(String[] columnNames, String[] columnAliases) {
		for ( int i=0; i<columnNames.length; i++ ) {
			if ( columnNames[i]!=null  ) {
				addColumn( columnNames[i], columnAliases[i] );
			}
		}
		return this;
	}

	public SimpleSelect addColumns(String[] columns, String[] aliases, boolean[] ignore) {
		for ( int i=0; i<ignore.length; i++ ) {
			if ( !ignore[i] && columns[i]!=null ) {
				addColumn( columns[i], aliases[i] );
			}
		}
		return this;
	}

	public SimpleSelect addColumns(String[] columnNames) {
		for ( int i=0; i<columnNames.length; i++ ) {
			if ( columnNames[i]!=null ) addColumn( columnNames[i] );
		}
		return this;
	}
	public SimpleSelect addColumn(String columnName) {
		columns.add(columnName);
		//aliases.put( columnName, DEFAULT_ALIAS.toAliasString(columnName) );
		return this;
	}

	public SimpleSelect addColumn(String columnName, String alias) {
		columns.add(columnName);
		aliases.put(columnName, alias);
		return this;
	}

	public SimpleSelect setTableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	public SimpleSelect setLockOptions( LockOptions lockOptions ) {
	   LockOptions.copy(lockOptions, this.lockOptions);
		return this;
	}

	public SimpleSelect setLockMode(LockMode lockMode) {
		this.lockOptions.setLockMode( lockMode );
		return this;
	}

	public SimpleSelect addWhereToken(String token) {
		whereTokens.add(token);
		return this;
	}
	
	private void and() {
		if ( whereTokens.size()>0 ) {
			whereTokens.add("and");
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
		for ( int i=0; i<lhs.length; i++ ) {
			addCondition( lhs[i], op, rhs[i] );
		}
		return this;
	}

	public SimpleSelect addCondition(String[] lhs, String condition) {
		for ( int i=0; i<lhs.length; i++ ) {
			if ( lhs[i]!=null ) addCondition( lhs[i], condition );
		}
		return this;
	}

	public String toStatementString() {
		StringBuilder buf = new StringBuilder( 
				columns.size()*10 + 
				tableName.length() + 
				whereTokens.size() * 10 + 
				10 
			);
		
		if ( comment!=null ) {
			buf.append("/* ").append(comment).append(" */ ");
		}
		
		buf.append("select ");
		Set uniqueColumns = new HashSet();
		Iterator iter = columns.iterator();
		boolean appendComma = false;
		while ( iter.hasNext() ) {
			String col = (String) iter.next();
			String alias = (String) aliases.get(col);
			if ( uniqueColumns.add(alias==null ? col : alias) ) {
				if (appendComma) buf.append(", ");
				buf.append(col);
				if ( alias!=null && !alias.equals(col) ) {
					buf.append(" as ")
						.append(alias);
				}
				appendComma = true;
			}
		}
		
		buf.append(" from ")
			.append( dialect.appendLockHint(lockOptions, tableName) );
		
		if ( whereTokens.size() > 0 ) {
			buf.append(" where ")
				.append( toWhereClause() );
		}
		
		if (orderBy!=null) buf.append(orderBy);
		
		if (lockOptions!=null) {
			buf.append( dialect.getForUpdateString(lockOptions) );
		}

		return dialect.transformSelectString( buf.toString() );
	}

	public String toWhereClause() {
		StringBuilder buf = new StringBuilder( whereTokens.size() * 5 );
		Iterator iter = whereTokens.iterator();
		while ( iter.hasNext() ) {
			buf.append( iter.next() );
			if ( iter.hasNext() ) buf.append(' ');
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
