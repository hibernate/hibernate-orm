/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.Schema;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
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

	/**
	 * Intended for use from JPA schema export code.
	 *
	 * @param metadata The metadata for which to generate drop commands
	 * @param dropSchemas Should {@code DROP SCHEMA} command be generated?
	 * @param dialect Allow explicitly specifying the dialect.
	 *
	 * @return The commands
	 */
	public Iterable<String> generateDropCommands(MetadataImplementor metadata, boolean dropSchemas, Dialect dialect) {
		final ArrayList<String> commands = new ArrayList<String>();
		doDrop(
				metadata,
				dropSchemas,
				dialect,
				new Target() {

					@Override
					public boolean acceptsImportScriptActions() {
						return true;
					}

					@Override
					public void prepare() {
					}

					@Override
					public void accept(String action) {
						commands.add( action );
					}

					@Override
					public void release() {
					}
				}
		);
		return commands;
	}

	@Override
	public void doDrop(Metadata metadata, boolean dropSchemas, List<Target> targets) throws SchemaManagementException {
		doDrop( metadata, dropSchemas, targets.toArray( new Target[ targets.size() ] ) );
	}

	@Override
	public void doDrop(Metadata metadata, boolean dropSchemas, Dialect dialect, List<Target> targets) throws SchemaManagementException {
		doDrop( metadata, dropSchemas, dialect, targets.toArray( new Target[ targets.size() ] ) );
	}

	@Override
	public void doDrop(Metadata metadata, boolean dropSchemas, Target... targets) throws SchemaManagementException {
		doDrop(
				metadata,
				dropSchemas,
				metadata.getDatabase().getJdbcEnvironment().getDialect(),
				targets
		);
	}


	@Override
	public void doDrop(Metadata metadata, boolean dropSchemas, Dialect dialect, Target... targets) throws SchemaManagementException {
		final Database database = metadata.getDatabase();
		final JdbcEnvironment jdbcEnvironment = database.getJdbcEnvironment();

		for ( Target target : targets ) {
			target.prepare();
		}

		final Set<String> exportIdentifiers = new HashSet<String>( 50 );

		// NOTE : init commands are irrelevant for dropping...

		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : database.getAuxiliaryDatabaseObjects() ) {
			if ( !auxiliaryDatabaseObject.beforeTablesOnCreation() ) {
				continue;
			}
			if ( !auxiliaryDatabaseObject.appliesToDialect( dialect ) ) {
				continue;
			}

			applySqlStrings(
					targets,
					dialect.getAuxiliaryDatabaseObjectExporter().getSqlDropStrings( auxiliaryDatabaseObject, metadata )
			);
		}

		for ( Schema schema : database.getSchemas() ) {
			// we need to drop all constraints/indexes prior to dropping the tables
			applyConstraintDropping( targets, schema, metadata );

			// now it's safe to drop the tables
			for ( Table table : schema.getTables() ) {
				if ( !table.isPhysicalTable() ) {
					continue;
				}
				checkExportIdentifier( table, exportIdentifiers );
				applySqlStrings( targets, dialect.getTableExporter().getSqlDropStrings( table, metadata ) );
			}

			for ( Sequence sequence : schema.getSequences() ) {
				checkExportIdentifier( sequence, exportIdentifiers );
				applySqlStrings( targets, dialect.getSequenceExporter().getSqlDropStrings( sequence, metadata ) );
			}
		}

		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : database.getAuxiliaryDatabaseObjects() ) {
			if ( auxiliaryDatabaseObject.beforeTablesOnCreation() ) {
				continue;
			}
			if ( !auxiliaryDatabaseObject.appliesToDialect( dialect ) ) {
				continue;
			}

			applySqlStrings(
					targets,
					auxiliaryDatabaseObject.sqlDropStrings( jdbcEnvironment.getDialect() )
			);
		}

		for ( Schema schema : database.getSchemas() ) {
			if ( dropSchemas ) {
				if ( schema.getName().getSchema() == null ) {
					continue;
				}
				applySqlStrings( targets, dialect.getDropSchemaCommand( schema.getName().getSchema().render( dialect ) ) );
			}
		}

		for ( Target target : targets ) {
			target.release();
		}
	}

	private void applyConstraintDropping(Target[] targets, Schema schema, Metadata metadata) {
		final Dialect dialect = metadata.getDatabase().getJdbcEnvironment().getDialect();

		if ( !dialect.dropConstraints() ) {
			return;
		}

		for ( Table table : schema.getTables() ) {
			if ( !table.isPhysicalTable() ) {
				continue;
			}

			final Iterator fks = table.getForeignKeyIterator();
			while ( fks.hasNext() ) {
				final ForeignKey foreignKey = (ForeignKey) fks.next();
				applySqlStrings(
						targets,
						dialect.getForeignKeyExporter().getSqlDropStrings( foreignKey, metadata )
				);
			}
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
