/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.dialect.constraint;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.spi.relational.Index;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.tool.schema.internal.StandardIndexExporter;

/**
 * MySQL requires "ON table" at the end of a "DROP INDEX".
 * 
 * TODO: If other Dialects need that as well, consider adding a "requiresOnTable" type of method on Dialect and
 * work it into StandardIndexExporter itself.
 * 
 * @author Brett Meyer
 */
public class MySQLIndexExporter extends StandardIndexExporter {
	private final Dialect dialect;

	public MySQLIndexExporter(Dialect dialect) {
		super(dialect);
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlDropStrings(Index index, JdbcEnvironment jdbcEnvironment) {
		if ( ! dialect.dropConstraints() ) {
			return NO_COMMANDS;
		}

		final String tableName = jdbcEnvironment.getQualifiedObjectNameSupport().formatName(
				( (Table) index.getTable() ).getTableName()
		);
		
		StringBuilder sb = new StringBuilder();
		sb.append( "drop index " );
		sb.append( ( dialect.qualifyIndexName()
				? index.getName().getQualifiedText( tableName, dialect ) : index.getName() ) );
		sb.append( " on " + tableName );
		
		return new String[] { sb.toString() };
	}
}
