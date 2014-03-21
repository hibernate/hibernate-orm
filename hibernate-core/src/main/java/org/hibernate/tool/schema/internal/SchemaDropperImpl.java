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
package org.hibernate.tool.schema.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.spi.relational.AuxiliaryDatabaseObject;
import org.hibernate.metamodel.spi.relational.Database;
import org.hibernate.metamodel.spi.relational.Exportable;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.relational.Sequence;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.tool.schema.spi.SchemaDropper;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.Target;

/**
 * This is functionally nothing more than the creation script from the older SchemaExport class (plus some
 * additional stuff in the script).
 *
 * @author Steve Ebersole
 */
public class SchemaDropperImpl implements SchemaDropper {

	@Override
	public void doDrop(Database database, boolean dropSchemas, List<Target> targets) throws SchemaManagementException {
		doDrop( database, dropSchemas, targets.toArray( new Target[ targets.size() ] ) );
	}

	@Override
	public void doDrop(Database database, boolean dropSchemas, Target... targets) throws SchemaManagementException {
		final JdbcEnvironment jdbcEnvironment = database.getJdbcEnvironment();
		final Dialect dialect = jdbcEnvironment.getDialect();

		for ( Target target : targets ) {
			target.prepare();
		}

		final Set<String> exportIdentifiers = new HashSet<String>( 50 );

		// NOTE : init commands are irrelevant for dropping...

		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : database.getAuxiliaryDatabaseObjects() ) {
			if ( auxiliaryDatabaseObject.appliesToDialect( dialect ) && ! auxiliaryDatabaseObject.beforeTablesOnCreation() ) {
				checkExportIdentifier( auxiliaryDatabaseObject, exportIdentifiers );
				applySqlStrings(
						targets,
						dialect.getAuxiliaryDatabaseObjectExporter().getSqlDropStrings( auxiliaryDatabaseObject, jdbcEnvironment )
				);
			}
		}

		for ( Schema schema : database.getSchemas() ) {
			// we need to drop all constraints/indexes prior to dropping the tables
			
			applySqlStrings( targets, dialect.dropConstraints( schema.getTables(), jdbcEnvironment ) );
			for ( Table table : schema.getTables() ) {
				if( !table.isPhysicalTable() ){
					continue;
				}
				if ( dialect.dropConstraints() ) {
					for ( ForeignKey foreignKey : table.getForeignKeys() ) {
						// only add the foreign key if its source and target are both physical tables
						// and if the target table does not have any denormalized tables.
						if ( Table.class.isInstance( foreignKey.getTable() ) &&
								Table.class.isInstance( foreignKey.getTargetTable() ) ) {
							Table sourceTable = Table.class.cast( foreignKey.getTable() );
							Table targetTable = Table.class.cast( foreignKey.getTargetTable() );
							if ( sourceTable.isPhysicalTable() &&
									targetTable.isPhysicalTable() &&
									!targetTable.hasDenormalizedTables() ) {
								checkExportIdentifier( foreignKey, exportIdentifiers );
								applySqlStrings(
										targets,
										dialect.getForeignKeyExporter().getSqlDropStrings( foreignKey, jdbcEnvironment )
								);
							}
						}
					}
				}
			}
			
			// now it's safe to drop the tables
			for ( Table table : schema.getTables() ) {
				if( !table.isPhysicalTable() ){
					continue;
				}
				checkExportIdentifier( table, exportIdentifiers );
				applySqlStrings( targets, dialect.getTableExporter().getSqlDropStrings( table, jdbcEnvironment ) );
			}

			for ( Sequence sequence : schema.getSequences() ) {
				checkExportIdentifier( sequence, exportIdentifiers );
				applySqlStrings( targets, dialect.getSequenceExporter().getSqlDropStrings( sequence, jdbcEnvironment ) );
			}
		}

		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : database.getAuxiliaryDatabaseObjects() ) {
			if ( auxiliaryDatabaseObject.appliesToDialect( dialect ) && ! auxiliaryDatabaseObject.beforeTablesOnCreation() ) {
				checkExportIdentifier( auxiliaryDatabaseObject, exportIdentifiers );
				applySqlStrings(
						targets,
						dialect.getAuxiliaryDatabaseObjectExporter().getSqlDropStrings( auxiliaryDatabaseObject, jdbcEnvironment )
				);			}
		}

		for ( Schema schema : database.getSchemas() ) {
			if ( dropSchemas ) {
				applySqlStrings( targets, dialect.getDropSchemaCommand( schema.getName().getSchema().getText( dialect ) ) );
			}
		}

		for ( Target target : targets ) {
			target.release();
		}
	}

	private static void checkExportIdentifier(Exportable exportable, Set<String> exportIdentifiers) {
		final String exportIdentifier = exportable.getExportIdentifier();
		if ( exportIdentifiers.contains( exportIdentifier ) ) {
			throw new SchemaManagementException( "SQL strings added more than once for: " + exportIdentifier );
		}
		exportIdentifiers.add( exportIdentifier );
	}

	private static void applySqlStrings(Target[] targets, String... sqlStrings) {
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
