/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;
import java.util.Iterator;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;

import org.jboss.logging.Logger;

/**
 * A primary key constraint
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class PrimaryKey extends Constraint {
	private static final Logger log = Logger.getLogger( PrimaryKey.class );

	public PrimaryKey(Table table){
		setTable( table );
	}

	@Override
	public void addColumn(Column column) {
		final Iterator<Column> columnIterator = getTable().getColumnIterator();
		while ( columnIterator.hasNext() ) {
			final Column next = columnIterator.next();
			if ( next.getCanonicalName().equals( column.getCanonicalName() ) ) {
				next.setNullable( false );
				log.debugf(
						"Forcing column [%s] to be non-null as it is part of the primary key for table [%s]",
						column.getCanonicalName(),
						getTableNameForLogging( column )
				);
			}
		}
		super.addColumn( column );
	}

	protected String getTableNameForLogging(Column column) {
		if ( getTable() != null ) {
			if ( getTable().getNameIdentifier() != null ) {
				return getTable().getNameIdentifier().getCanonicalName();
			}
			else {
				return "<unknown>";
			}
		}
		else if ( column.getValue() != null && column.getValue().getTable() != null ) {
			return column.getValue().getTable().getNameIdentifier().getCanonicalName();
		}
		return "<unknown>";
	}

	public String sqlConstraintString(Dialect dialect) {
		StringBuilder buf = new StringBuilder("primary key (");
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			buf.append( ( (Column) iter.next() ).getQuotedName(dialect) );
			if ( iter.hasNext() ) {
				buf.append(", ");
			}
		}
		return buf.append(')').toString();
	}

	public String sqlConstraintString(Dialect dialect, String constraintName, String defaultCatalog, String defaultSchema) {
		StringBuilder buf = new StringBuilder(
			dialect.getAddPrimaryKeyConstraintString(constraintName)
		).append('(');
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			buf.append( ( (Column) iter.next() ).getQuotedName(dialect) );
			if ( iter.hasNext() ) {
				buf.append(", ");
			}
		}
		return buf.append(')').toString();
	}
	
	public String generatedConstraintNamePrefix() {
		return "PK_";
	}

	@Override
	public String getExportIdentifier() {
		return StringHelper.qualify( getTable().getName(), "PK-" + getName() );
	}
}
