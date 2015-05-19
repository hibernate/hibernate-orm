/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.LiteralType;

/**
 * An SQL <tt>INSERT</tt> statement
 *
 * @author Gavin King
 */
public class Insert {
	private Dialect dialect;
	private String tableName;
	private String comment;
	private Map columns = new LinkedHashMap();

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

	public Insert addColumn(String columnName) {
		return addColumn(columnName, "?");
	}

	public Insert addColumns(String[] columnNames) {
		for ( int i=0; i<columnNames.length; i++ ) {
			addColumn( columnNames[i] );
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
		columns.put(columnName, valueExpression);
		return this;
	}

	public Insert addColumn(String columnName, Object value, LiteralType type) throws Exception {
		return addColumn( columnName, type.objectToSQLString(value, dialect) );
	}

	public Insert addIdentityColumn(String columnName) {
		String value = dialect.getIdentityInsertString();
		if ( value != null ) {
			addColumn( columnName, value );
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
			buf.append( "/* " ).append( comment ).append( " */ " );
		}
		buf.append("insert into ")
			.append(tableName);
		if ( columns.size()==0 ) {
			buf.append(' ').append( dialect.getNoColumnsInsertString() );
		}
		else {
			buf.append(" (");
			Iterator iter = columns.keySet().iterator();
			while ( iter.hasNext() ) {
				buf.append( iter.next() );
				if ( iter.hasNext() ) {
					buf.append( ", " );
				}
			}
			buf.append(") values (");
			iter = columns.values().iterator();
			while ( iter.hasNext() ) {
				buf.append( iter.next() );
				if ( iter.hasNext() ) {
					buf.append( ", " );
				}
			}
			buf.append(')');
		}
		return buf.toString();
	}
}
