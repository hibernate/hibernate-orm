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
package org.hibernate.service.schema.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.spi.relational.AuxiliaryDatabaseObject;
import org.hibernate.metamodel.spi.relational.Database;
import org.hibernate.metamodel.spi.relational.Exportable;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.Index;
import org.hibernate.metamodel.spi.relational.InitCommand;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.metamodel.spi.relational.UniqueKey;
import org.hibernate.service.schema.spi.SchemaCreator;
import org.hibernate.service.schema.spi.SchemaManagementException;
import org.hibernate.service.schema.spi.Target;

/**
 * This is functionally nothing more than the creation script from the older SchemaExport class (plus some
 * additional stuff in the script).
 *
 * @author Steve Ebersole
 */
public class SchemaCreatorImpl implements SchemaCreator {
	@Override
	public void doCreation(Database database, List<Target> targets, boolean createSchemas) throws SchemaManagementException {
		final Dialect dialect = database.getJdbcEnvironment().getDialect();

		for ( Target target : targets ) {
			target.prepare();
		}

		final Set<String> exportIdentifiers = new HashSet<String>( 50 );

		for ( Schema schema : database.getSchemas() ) {
			if ( createSchemas ) {
				// todo : add dialect method for getting a CREATE SCHEMA command and use it here
			}

			for ( Table table : schema.getTables() ) {
				applySqlStrings( table, targets, dialect, exportIdentifiers );
			}

			// todo : allow stuff like user datatypes.
			//		maybe reusing AuxiliaryDatabaseObject as the general vehicle, but adding a notion of where
			// it needs to be in terms of creation?

			for ( Table table : schema.getTables() ) {
				if ( ! dialect.supportsUniqueConstraintInCreateAlterTable() ) {
					for  ( UniqueKey uniqueKey : table.getUniqueKeys() ) {
						applySqlStrings( uniqueKey, targets, dialect, exportIdentifiers );
					}
				}

				for ( Index index : table.getIndexes() ) {
					applySqlStrings( index, targets, dialect, exportIdentifiers );
				}

				if ( dialect.hasAlterTable() ) {
					for ( ForeignKey foreignKey : table.getForeignKeys() ) {
						// only add the foreign key if its target is a physical table
						if ( Table.class.isInstance( foreignKey.getTargetTable() ) ) {
							applySqlStrings( foreignKey, targets, dialect, exportIdentifiers );
						}
					}
				}
			}

			for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : database.getAuxiliaryDatabaseObjects() ) {
				if ( auxiliaryDatabaseObject.appliesToDialect( dialect ) ) {
					applySqlStrings( auxiliaryDatabaseObject, targets, dialect, exportIdentifiers );
				}
			}

			for ( InitCommand initCommand : database.getInitCommands() ) {
				applySqlStrings( initCommand.getInitCommands(), targets );
			}
		}

		for ( Target target : targets ) {
			target.release();
		}
	}

	private static void applySqlStrings(
			Exportable exportable,
			List<Target> targets,
			Dialect dialect,
			Set<String> exportIdentifiers) {
		final String exportIdentifier = exportable.getExportIdentifier();
		if ( exportIdentifiers.contains( exportIdentifier ) ) {
			throw new SchemaManagementException( "SQL strings added more than once for: " + exportIdentifier );
		}
		exportIdentifiers.add( exportIdentifier );

		applySqlStrings( exportable.sqlCreateStrings( dialect ), targets );
	}

	/*package*/ static void applySqlStrings(
			Exportable exportable,
			List<Target> targets,
			Dialect dialect) {
		applySqlStrings( exportable.sqlCreateStrings( dialect ), targets );
	}

	private static void applySqlStrings(String[] sqlStrings, List<Target> targets) {
		if ( sqlStrings == null ) {
			return;
		}

		for ( Target target : targets ) {
			for ( String sqlString : sqlStrings ) {
				target.accept( sqlString );
			}
		}
	}
}
