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
