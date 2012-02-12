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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.LiteralType;

/**
 * An SQL <tt>UPDATE</tt> statement
 *
 * @author Gavin King
 */
public class Update {

	private String tableName;
	private String versionColumnName;
	private String where;
	private String assignments;
	private String comment;

	private Map primaryKeyColumns = new LinkedHashMap();
	private Map columns = new LinkedHashMap();
	private Map whereColumns = new LinkedHashMap();
	
	private Dialect dialect;
	
	public Update(Dialect dialect) {
		this.dialect = dialect;
	}

	public String getTableName() {
		return tableName;
	}

	public Update appendAssignmentFragment(String fragment) {
		if ( assignments == null ) {
			assignments = fragment;
		}
		else {
			assignments += ", " + fragment;
		}
		return this;
	}

	public Update setTableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	public Update setPrimaryKeyColumnNames(String[] columnNames) {
		this.primaryKeyColumns.clear();
		addPrimaryKeyColumns(columnNames);
		return this;
	}	
	
	public Update addPrimaryKeyColumns(String[] columnNames) {
		for ( int i=0; i<columnNames.length; i++ ) {
			addPrimaryKeyColumn( columnNames[i], "?" );
		}
		return this;
	}
	
	public Update addPrimaryKeyColumns(String[] columnNames, boolean[] includeColumns, String[] valueExpressions) {
		for ( int i=0; i<columnNames.length; i++ ) {
			if( includeColumns[i] ) addPrimaryKeyColumn( columnNames[i], valueExpressions[i] );
		}
		return this;
	}
	
	public Update addPrimaryKeyColumns(String[] columnNames, String[] valueExpressions) {
		for ( int i=0; i<columnNames.length; i++ ) {
			addPrimaryKeyColumn( columnNames[i], valueExpressions[i] );
		}
		return this;
	}	

	public Update addPrimaryKeyColumn(String columnName, String valueExpression) {
		this.primaryKeyColumns.put(columnName, valueExpression);
		return this;
	}
	
	public Update setVersionColumnName(String versionColumnName) {
		this.versionColumnName = versionColumnName;
		return this;
	}


	public Update setComment(String comment) {
		this.comment = comment;
		return this;
	}
	
	public Update addColumns(String[] columnNames) {
		for ( int i=0; i<columnNames.length; i++ ) {
			addColumn( columnNames[i] );
		}
		return this;
	}

	public Update addColumns(String[] columnNames, boolean[] updateable, String[] valueExpressions) {
		for ( int i=0; i<columnNames.length; i++ ) {
			if ( updateable[i] ) addColumn( columnNames[i], valueExpressions[i] );
		}
		return this;
	}

	public Update addColumns(String[] columnNames, String valueExpression) {
		for ( int i=0; i<columnNames.length; i++ ) {
			addColumn( columnNames[i], valueExpression );
		}
		return this;
	}

	public Update addColumn(String columnName) {
		return addColumn(columnName, "?");
	}

	public Update addColumn(String columnName, String valueExpression) {
		columns.put(columnName, valueExpression);
		return this;
	}

	public Update addColumn(String columnName, Object value, LiteralType type) throws Exception {
		return addColumn( columnName, type.objectToSQLString(value, dialect) );
	}

	public Update addWhereColumns(String[] columnNames) {
		for ( int i=0; i<columnNames.length; i++ ) {
			addWhereColumn( columnNames[i] );
		}
		return this;
	}

	public Update addWhereColumns(String[] columnNames, String valueExpression) {
		for ( int i=0; i<columnNames.length; i++ ) {
			addWhereColumn( columnNames[i], valueExpression );
		}
		return this;
	}

	public Update addWhereColumn(String columnName) {
		return addWhereColumn(columnName, "=?");
	}

	public Update addWhereColumn(String columnName, String valueExpression) {
		whereColumns.put(columnName, valueExpression);
		return this;
	}

	public Update setWhere(String where) {
		this.where=where;
		return this;
	}

	public String toStatementString() {
		StringBuilder buf = new StringBuilder( (columns.size() * 15) + tableName.length() + 10 );
		if ( comment!=null ) {
			buf.append( "/* " ).append( comment ).append( " */ " );
		}
		buf.append( "update " ).append( tableName ).append( " set " );
		boolean assignmentsAppended = false;
		Iterator iter = columns.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry e = (Map.Entry) iter.next();
			buf.append( e.getKey() ).append( '=' ).append( e.getValue() );
			if ( iter.hasNext() ) {
				buf.append( ", " );
			}
			assignmentsAppended = true;
		}
		if ( assignments != null ) {
			if ( assignmentsAppended ) {
				buf.append( ", " );
			}
			buf.append( assignments );
		}

		boolean conditionsAppended = false;
		if ( !primaryKeyColumns.isEmpty() || where != null || !whereColumns.isEmpty() || versionColumnName != null ) {
			buf.append( " where " );
		}
		iter = primaryKeyColumns.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry e = (Map.Entry) iter.next();
			buf.append( e.getKey() ).append( '=' ).append( e.getValue() );
			if ( iter.hasNext() ) {
				buf.append( " and " );
			}
			conditionsAppended = true;
		}
		if ( where != null ) {
			if ( conditionsAppended ) {
				buf.append( " and " );
			}
			buf.append( where );
			conditionsAppended = true;
		}
		iter = whereColumns.entrySet().iterator();
		while ( iter.hasNext() ) {
			final Map.Entry e = (Map.Entry) iter.next();
			if ( conditionsAppended ) {
				buf.append( " and " );
			}
			buf.append( e.getKey() ).append( e.getValue() );
			conditionsAppended = true;
		}
		if ( versionColumnName != null ) {
			if ( conditionsAppended ) {
				buf.append( " and " );
			}
			buf.append( versionColumnName ).append( "=?" );
		}

		return buf.toString();
	}
}
