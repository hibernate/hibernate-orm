//$Id: Update.java 7825 2005-08-10 20:23:55Z oneovthafew $
package org.hibernate.sql;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.LiteralType;
import org.hibernate.util.StringHelper;

import org.apache.commons.collections.SequencedHashMap;

/**
 * An SQL <tt>UPDATE</tt> statement
 *
 * @author Gavin King
 */
public class Update {

	private String tableName;
	private String[] primaryKeyColumnNames;
	private String versionColumnName;
	private String where;
	private String assignments;
	private String comment;

	private Map columns = new SequencedHashMap();
	private Map whereColumns = new SequencedHashMap();
	
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

	public Update setPrimaryKeyColumnNames(String[] primaryKeyColumnNames) {
		this.primaryKeyColumnNames = primaryKeyColumnNames;
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

	public Update addColumns(String[] columnNames, boolean[] updateable) {
		for ( int i=0; i<columnNames.length; i++ ) {
			if ( updateable[i] ) addColumn( columnNames[i] );
		}
		return this;
	}

	public Update addColumns(String[] columnNames, String value) {
		for ( int i=0; i<columnNames.length; i++ ) {
			addColumn( columnNames[i], value );
		}
		return this;
	}

	public Update addColumn(String columnName) {
		return addColumn(columnName, "?");
	}

	public Update addColumn(String columnName, String value) {
		columns.put(columnName, value);
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

	public Update addWhereColumns(String[] columnNames, String value) {
		for ( int i=0; i<columnNames.length; i++ ) {
			addWhereColumn( columnNames[i], value );
		}
		return this;
	}

	public Update addWhereColumn(String columnName) {
		return addWhereColumn(columnName, "=?");
	}

	public Update addWhereColumn(String columnName, String value) {
		whereColumns.put(columnName, value);
		return this;
	}

	public Update setWhere(String where) {
		this.where=where;
		return this;
	}

	public String toStatementString() {
		StringBuffer buf = new StringBuffer( (columns.size() * 15) + tableName.length() + 10 );
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
		if ( primaryKeyColumnNames != null || where != null || !whereColumns.isEmpty() || versionColumnName != null ) {
			buf.append( " where " );
		}
		if ( primaryKeyColumnNames != null ) {
			buf.append( StringHelper.join( "=? and ", primaryKeyColumnNames ) ).append( "=?" );
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
