//$Id: Delete.java 10226 2006-08-05 04:27:55Z steve.ebersole@jboss.com $
package org.hibernate.sql;

import org.hibernate.util.StringHelper;

/**
 * An SQL <tt>DELETE</tt> statement
 *
 * @author Gavin King
 */
public class Delete {

	private String tableName;
	private String[] primaryKeyColumnNames;
	private String versionColumnName;
	private String where;

	private String comment;
	public Delete setComment(String comment) {
		this.comment = comment;
		return this;
	}

	public Delete setTableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	public String toStatementString() {
		StringBuffer buf = new StringBuffer( tableName.length() + 10 );
		if ( comment!=null ) {
			buf.append( "/* " ).append(comment).append( " */ " );
		}
		buf.append( "delete from " ).append(tableName);
		if ( where != null || primaryKeyColumnNames != null || versionColumnName != null ) {
			buf.append( " where " );
		}
		boolean conditionsAppended = false;
		if ( primaryKeyColumnNames != null ) {
			buf.append( StringHelper.join( "=? and ", primaryKeyColumnNames ) ).append( "=?" );
			conditionsAppended = true;
		}
		if ( where!=null ) {
			if ( conditionsAppended ) {
				buf.append( " and " );
			}
			buf.append( where );
			conditionsAppended = true;
		}
		if ( versionColumnName!=null ) {
			if ( conditionsAppended ) {
				buf.append( " and " );
			}
			buf.append( versionColumnName ).append( "=?" );
		}
		return buf.toString();
	}

	public Delete setWhere(String where) {
		this.where=where;
		return this;
	}

	public Delete addWhereFragment(String fragment) {
		if ( where == null ) {
			where = fragment;
		}
		else {
			where += ( " and " + fragment );
		}
		return this;
	}

	public Delete setPrimaryKeyColumnNames(String[] primaryKeyColumnNames) {
		this.primaryKeyColumnNames = primaryKeyColumnNames;
		return this;
	}

	public Delete setVersionColumnName(String versionColumnName) {
		this.versionColumnName = versionColumnName;
		return this;
	}

}
