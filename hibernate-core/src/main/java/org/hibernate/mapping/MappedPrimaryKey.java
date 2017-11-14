/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.Collection;
import java.util.Iterator;

import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedTable;

import org.jboss.logging.Logger;

/**
 * A primary key constraint
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class MappedPrimaryKey extends Constraint {
	private static final Logger log = Logger.getLogger( MappedPrimaryKey.class );

	public MappedPrimaryKey(MappedTable table){
		setTable( table );
	}

	public void addColumn(MappedColumn column) {

	}

	@Override
	public void addColumn(Column column) {
		final Collection<Column> columns = getMappedTable().getMappedColumns();
		columns.stream().filter( c -> c.getCanonicalName().equals( column.getCanonicalName() ) )
				.forEach( c -> {
					c.setNullable( false );
					log.debugf(
							"Forcing column [%s] to be non-null as it is part of the primary key for table [%s]",
							column.getCanonicalName(),
							getTableNameForLogging( column )
					);
				} );
		super.addColumn( column );
	}

	protected String getTableNameForLogging(Column column) {
		if ( getMappedTable() != null ) {
			if ( getMappedTable().getNameIdentifier() != null ) {
				return getMappedTable().getNameIdentifier().getCanonicalName();
			}
			else {
				return "<unknown>";
			}
		}
		else if ( column.getTableName() != null ) {
			return column.getTableName().getCanonicalName();
		}
		return "<unknown>";
	}

	public String generatedConstraintNamePrefix() {
		return "PK_";
	}
}
