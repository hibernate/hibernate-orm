/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.Target;

/**
 * This is functionally nothing more than the creation script from the older SchemaExport class (plus some
 * additional stuff in the script).
 *
 * @author Steve Ebersole
 */
public class SchemaCreatorImpl implements SchemaCreator {

	@Override
	public void doCreation(Metadata metadata, boolean createNamespaces, List<Target> targets) throws SchemaManagementException {
		doCreation( metadata, createNamespaces, targets.toArray( new Target[ targets.size() ] ) );
	}

	@Override
	public void doCreation(Metadata metadata, boolean createNamespaces, Dialect dialect, List<Target> targets) throws SchemaManagementException {
		doCreation( metadata, createNamespaces, dialect, targets.toArray( new Target[ targets.size() ] ) );
	}

	/**
	 * For testing...
	 *
	 * @param metadata The metadata for which to generate the creation commands.
	 *
	 * @return The generation commands
	 */
	public List<String> generateCreationCommands(Metadata metadata, boolean createNamespaces) {
		final ArrayList<String> commands = new ArrayList<String>();
		doCreation(
				metadata,
				createNamespaces,
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

	/**
	 * For temporary use from JPA schema generation
	 *
	 * @param metadata The metadata for which to generate the creation commands.
	 * @param createNamespaces Should the schema(s)/catalog(s) actually be created as well ({@code CREATE SCHEMA})?
	 * @param dialect Allow explicitly passing the Dialect to use.
	 *
	 * @return The generation commands
	 */
	public List<String> generateCreationCommands(Metadata metadata, boolean createNamespaces, Dialect dialect) {
		final ArrayList<String> commands = new ArrayList<String>();
		doCreation(
				metadata,
				createNamespaces,
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
	public void doCreation(Metadata metadata, boolean createNamespaces, Target... targets)
			throws SchemaManagementException {
		doCreation(
				metadata,
				createNamespaces,
				metadata.getDatabase().getJdbcEnvironment().getDialect(),
				targets
		);
	}

	@Override
	public void doCreation(Metadata metadata, boolean createNamespaces, Dialect dialect, Target... targets)
			throws SchemaManagementException {
		boolean tryToCreateCatalogs = false;
		boolean tryToCreateSchemas = false;
		if ( createNamespaces ) {
			if ( dialect.canCreateSchema() ) {
				tryToCreateSchemas = true;
			}
			if ( dialect.canCreateCatalog() ) {
				tryToCreateCatalogs = true;
			}
		}

		final Database database = metadata.getDatabase();
		final JdbcEnvironment jdbcEnvironment = database.getJdbcEnvironment();

		for ( Target target : targets ) {
			target.prepare();
		}

		final Set<String> exportIdentifiers = new HashSet<String>( 50 );

		// first, create each catalog/schema
		if ( tryToCreateCatalogs || tryToCreateSchemas ) {
			Set<Identifier> exportedCatalogs = new HashSet<Identifier>();
			for ( Namespace namespace : database.getNamespaces() ) {

				if ( tryToCreateCatalogs ) {
					final Identifier catalogLogicalName = namespace.getName().getCatalog();
					final Identifier catalogPhysicalName = namespace.getPhysicalName().getCatalog();

					if ( catalogPhysicalName != null && !exportedCatalogs.contains( catalogLogicalName ) ) {
						applySqlStrings(
								targets,
								dialect.getCreateCatalogCommand( catalogPhysicalName.render( dialect ) )
						);
						exportedCatalogs.add( catalogLogicalName );
					}
				}

				if ( tryToCreateSchemas && namespace.getPhysicalName().getSchema() != null ) {
					applySqlStrings(
							targets,
							dialect.getCreateSchemaCommand(
									namespace.getPhysicalName().getSchema().render( dialect )
							)
					);
				}
			}
		}

		// next, create all "before table" auxiliary objects
		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : database.getAuxiliaryDatabaseObjects() ) {
			if ( !auxiliaryDatabaseObject.beforeTablesOnCreation() ) {
				continue;
			}

			if ( auxiliaryDatabaseObject.appliesToDialect( dialect ) ) {
				applySqlStrings(
						targets,
						dialect.getAuxiliaryDatabaseObjectExporter().getSqlCreateStrings(
								auxiliaryDatabaseObject,
								metadata
						)
				);
			}
		}

		// then, create all schema objects (tables, sequences, constraints, etc) in each schema
		for ( Namespace namespace : database.getNamespaces() ) {

			// sequences
			for ( Sequence sequence : namespace.getSequences() ) {
				checkExportIdentifier( sequence, exportIdentifiers );
				applySqlStrings(
						targets,
						dialect.getCreateSequenceStrings(
								jdbcEnvironment.getQualifiedObjectNameFormatter().format( sequence.getName(), dialect ),
								sequence.getInitialValue(),
								sequence.getIncrementSize()
						)
				);
			}

			// tables
			for ( Table table : namespace.getTables() ) {
				if( !table.isPhysicalTable() ){
					continue;
				}
				checkExportIdentifier( table, exportIdentifiers );
				applySqlStrings(
						targets,
						dialect.getTableExporter().getSqlCreateStrings( table, metadata )
				);

			}


			for ( Table table : namespace.getTables() ) {

				// NOTE : Foreign keys must be created *after* unique keys for numerous DBs.  See HHH-8390

				// indexes
				final Iterator indexItr = table.getIndexIterator();
				while ( indexItr.hasNext() ) {
					final Index index = (Index) indexItr.next();
					checkExportIdentifier( index, exportIdentifiers );
					applySqlStrings(
							targets,
							dialect.getIndexExporter().getSqlCreateStrings( index, metadata )
					);
				}

				// unique keys
				final Iterator ukItr = table.getUniqueKeyIterator();
				while ( ukItr.hasNext() ) {
					final UniqueKey uniqueKey = (UniqueKey) ukItr.next();
					checkExportIdentifier( uniqueKey, exportIdentifiers );
					applySqlStrings(
							targets,
							dialect.getUniqueKeyExporter().getSqlCreateStrings( uniqueKey, metadata )
					);
				}

				// foreign keys
				final Iterator fkItr = table.getForeignKeyIterator();
				while ( fkItr.hasNext() ) {
					final ForeignKey foreignKey = (ForeignKey) fkItr.next();
//					checkExportIdentifier( foreignKey, exportIdentifiers );
					applySqlStrings(
							targets,
							dialect.getForeignKeyExporter().getSqlCreateStrings( foreignKey, metadata )
					);
				}
			}
		}

		// next, create all "after table" auxiliary objects
		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : database.getAuxiliaryDatabaseObjects() ) {
			if ( auxiliaryDatabaseObject.appliesToDialect( dialect ) && !auxiliaryDatabaseObject.beforeTablesOnCreation() ) {
				checkExportIdentifier( auxiliaryDatabaseObject, exportIdentifiers );
				applySqlStrings(
						targets,
						dialect.getAuxiliaryDatabaseObjectExporter().getSqlCreateStrings( auxiliaryDatabaseObject, metadata )
				);
			}
		}

		// and finally add all init commands
		for ( InitCommand initCommand : database.getInitCommands() ) {
			applySqlStrings( targets, initCommand.getInitCommands() );
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
