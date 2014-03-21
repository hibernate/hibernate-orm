/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.dialect.constraint;

import java.util.Iterator;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Constraint;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.tool.schema.internal.StandardUniqueKeyExporter;

/**
 * DB2 does not allow unique constraints on nullable columns.  Rather than
 * forcing "not null", instead use unique indexes on nullables.
 * 
 * @author Brett Meyer
 */
public class DB2UniqueKeyExporter extends StandardUniqueKeyExporter {
	private final Dialect dialect;
	
	public DB2UniqueKeyExporter(Dialect dialect) {
		super( dialect );
		this.dialect = dialect;
	}
	
	@Override
	public String[] getSqlCreateStrings(Constraint constraint, JdbcEnvironment jdbcEnvironment) {
		if ( hasNullable( constraint ) ) {
			final String tableName = jdbcEnvironment.getQualifiedObjectNameSupport().formatName(
					( (Table) constraint.getTable() ).getTableName() );
			StringBuilder sb = new StringBuilder()
					.append( "create unique index " )
					.append( constraint.getName().getText( dialect ) )
					.append( " on " )
					.append( tableName )
					.append( " (" );
			final Iterator columnIterator = constraint.getColumns().iterator();
			while ( columnIterator.hasNext() ) {
				Column column = (Column) columnIterator.next();
				sb.append( column.getColumnName().getText( dialect ) );
				if ( columnIterator.hasNext() ) {
					sb.append( ", " );
				}
			}
			sb.append( ")" );
			return new String[] { sb.toString() };
		}
		else {
			return super.getSqlCreateStrings( constraint, jdbcEnvironment );
		}
	}

	@Override
	public String[] getSqlDropStrings(Constraint constraint, JdbcEnvironment jdbcEnvironment) {
		if ( hasNullable( constraint ) ) {
			return new String[] { "drop index " + constraint.getName().getText( dialect ) };
		}
		else {
			return super.getSqlDropStrings( constraint, jdbcEnvironment );
		}
	}

	private boolean hasNullable(Constraint constraint) {
		for ( Column column : constraint.getColumns() ) {
			if ( column.isNullable() ) {
				return true;
			}
		}
		return false;
	}
}
