/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.InitCommand;
import org.hibernate.boot.model.relational.MappedAuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedNamespace;
import org.hibernate.boot.model.relational.MappedSequence;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.mapping.MappedPrimaryKey;
import org.hibernate.metamodel.model.creation.spi.DatabaseObjectResolver;
import org.hibernate.metamodel.model.relational.internal.DatabaseModelImpl;
import org.hibernate.metamodel.model.relational.internal.InflightTable;
import org.hibernate.metamodel.model.relational.internal.NamespaceImpl;

import org.jboss.logging.Logger;

/**
 * Component used to produce the fully-populated runtime view of the relational
 * model given the boot-time view.
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public class RuntimeDatabaseModelProducer {
	private static final Logger log = Logger.getLogger( RuntimeDatabaseModelProducer.class );

	private final PhysicalNamingStrategy namingStrategy;
	private final JdbcEnvironment jdbcEnvironment;
	private final IdentifierGeneratorFactory identifierGeneratorFactory;

	public RuntimeDatabaseModelProducer(BootstrapContext bootstrapContext) {
		this.namingStrategy = bootstrapContext.getMetadataBuildingOptions().getPhysicalNamingStrategy();

		final StandardServiceRegistry serviceRegistry = bootstrapContext.getServiceRegistry();
		this.jdbcEnvironment = serviceRegistry.getService( JdbcServices.class ).getJdbcEnvironment();
		this.identifierGeneratorFactory = serviceRegistry.getService( MutableIdentifierGeneratorFactory.class );
	}

	public DatabaseModel produceDatabaseModel(Database database, DatabaseObjectResolver dbObjectResolver, Callback callback) {
		return new Process( callback, dbObjectResolver, database ).execute();
	}

	class Process {
		private final Callback callback;
		private final DatabaseObjectResolver dbObjectResolver;
		private final Database bootDatabaseModel;

		public Process(
				Callback callback,
				DatabaseObjectResolver dbObjectResolver,
				Database bootDatabaseModel) {
			this.callback = callback;
			this.dbObjectResolver = dbObjectResolver;
			this.bootDatabaseModel = bootDatabaseModel;
		}

		private DatabaseModel execute() {
			final DatabaseModelImpl runtimeDatabaseModel = new DatabaseModelImpl( bootDatabaseModel.getJdbcEnvironment() );
			generateDefaultNamespace(runtimeDatabaseModel);
			for ( MappedNamespace bootModelNamespace : bootDatabaseModel.getNamespaces() ) {
				final NamespaceImpl runtimeModelNamespace = generateNamespace(
						runtimeDatabaseModel,
						bootModelNamespace
				);

				processTables( bootModelNamespace, runtimeModelNamespace );
				processSequences( bootModelNamespace, runtimeModelNamespace );
			}

			processForeignKeys( bootDatabaseModel, runtimeDatabaseModel );
			for ( MappedAuxiliaryDatabaseObject mappedAuxiliaryDatabaseObject : bootDatabaseModel.getAuxiliaryDatabaseObjects() ) {
				runtimeDatabaseModel.addAuxiliaryDatabaseObject( mappedAuxiliaryDatabaseObject.generateRuntimeAuxiliaryDatabaseObject(
						bootDatabaseModel.getJdbcEnvironment().getDialect() ) );
			}

			for ( InitCommand command : bootDatabaseModel.getInitCommands() ) {
				runtimeDatabaseModel.addInitCommand( command );
			}

			return runtimeDatabaseModel;
		}

		private NamespaceImpl generateNamespace(
				DatabaseModelImpl databaseModel,
				MappedNamespace bootModelNamespace) {
			final NamespaceImpl runtimeModelNamespace = generateRuntimeNamespace(bootModelNamespace);
			databaseModel.addNamespace( runtimeModelNamespace );
			callback.namespaceBuilt( bootModelNamespace, runtimeModelNamespace );
			return runtimeModelNamespace;
		}

		private void generateDefaultNamespace(
				DatabaseModelImpl databaseModel) {
			final MappedNamespace bootModelDefaultNamespace = bootDatabaseModel.getDefaultNamespace();
			final NamespaceImpl runtimeModelDefaultNamespace = generateRuntimeNamespace( bootModelDefaultNamespace );
			databaseModel.setDefaultNamespace( runtimeModelDefaultNamespace );
			callback.namespaceBuilt( bootModelDefaultNamespace, runtimeModelDefaultNamespace );
		}

		private NamespaceImpl generateRuntimeNamespace(MappedNamespace bootModelNamespace) {
			return new NamespaceImpl(
					namingStrategy.toPhysicalCatalogName( bootModelNamespace.getName().getCatalog(), jdbcEnvironment ),
					namingStrategy.toPhysicalSchemaName( bootModelNamespace.getName().getSchema(), jdbcEnvironment )
			);
		}

		private void processTables(
				MappedNamespace bootModelNamespace,
				NamespaceImpl runtimeModelNamespace) {
			for ( MappedTable mappedTable : bootModelNamespace.getTables() ) {
				if ( mappedTable.isExportable() ) {
					final InflightTable runtimeTable = mappedTable.generateRuntimeTable(
							namingStrategy,
							jdbcEnvironment,
							identifierGeneratorFactory,
							callback
					);
					runtimeModelNamespace.addTable( runtimeTable );
				}
			}
		}

		private void processSequences(
				MappedNamespace bootModelNamespace,
				NamespaceImpl runtimeModelNamespace) {
			for ( MappedSequence mappedSequence : bootModelNamespace.getSequences() ) {
				final Sequence runtimeSequence = mappedSequence.generateRuntimeSequence(
						namingStrategy,
						jdbcEnvironment
				);
				runtimeModelNamespace.addSequence( runtimeSequence );
				callback.sequenceBuilt( runtimeSequence );
			}
		}

		private void processForeignKeys(Database bootDatabaseModel, DatabaseModelImpl runtimeDatabaseModel) {
			for ( MappedNamespace bootModelNamespace : bootDatabaseModel.getNamespaces() ) {
				for ( MappedTable bootTable : bootModelNamespace.getTables() ) {
					log.tracef( "Processing FKs for table : %s", bootTable );

					for ( org.hibernate.mapping.ForeignKey bootFk : bootTable.getForeignKeys() ) {
						log.tracef( "Processing mapped FK to runtime FK : %s", bootFk );

						bootFk.alignColumns();

						// find the referring table (the source of the FK)
						Table runtimeReferringTable = dbObjectResolver.resolveTable( bootTable );

						// find the target table
						final Table runtimeTargetTable = dbObjectResolver.resolveTable( bootFk.getReferencedTable() );


						final List<org.hibernate.mapping.Column> bootReferringColumns = bootFk.getColumns();
						final List<org.hibernate.mapping.Column> bootTargetColumns = bootFk.getReferencedColumns();

						final List<ForeignKey.ColumnMapping> columnMappingList = new ArrayList<>();

						if ( bootTargetColumns == null || bootTargetColumns.isEmpty() ) {
							assert bootFk.isReferenceToPrimaryKey();

							// use PK
							if ( runtimeTargetTable.hasPrimaryKey() ) {
								final List<PhysicalColumn> runtimeTargetPkColumns = runtimeTargetTable.getPrimaryKey()
										.getColumns();

								assertSameNumberOfFkColumns( bootReferringColumns, runtimeTargetPkColumns );

								for ( int i = 0; i < runtimeTargetPkColumns.size(); i++ ) {
									columnMappingList.add(
											new ColumnMappingImpl(
													(PhysicalColumn) dbObjectResolver.resolveColumn(
															bootReferringColumns.get( i ) ),
													runtimeTargetPkColumns.get( i )
											)
									);
								}
							}
						}
						else {
							assert !bootFk.isReferenceToPrimaryKey();
							assertSameNumberOfFkColumns( bootReferringColumns, bootTargetColumns );

							for ( int i = 0; i < bootReferringColumns.size(); i++ ) {
								columnMappingList.add(
										new ColumnMappingImpl(
												(PhysicalColumn) dbObjectResolver.resolveColumn( bootReferringColumns.get( i ) ),
												(PhysicalColumn) dbObjectResolver.resolveColumn( bootTargetColumns.get( i ) )
										)
								);
							}
						}

						// todo (6.0) : handle implicit fk names
						final ForeignKey runtimeFk = ( (InflightTable) runtimeReferringTable ).createForeignKey(
								bootFk.getName(),
								bootFk.isCreationEnabled() && bootFk.isPhysicalConstraint(),
								bootFk.getKeyDefinition(),
								bootFk.isCascadeDeleteEnabled(),
								runtimeTargetTable,
								new ColumnMappingsImpl(
										runtimeReferringTable,
										runtimeTargetTable,
										columnMappingList
								)
						);

						callback.foreignKeyBuilt( bootFk, runtimeFk );
					}
				}
			}
		}

		private void assertSameNumberOfFkColumns(List referringColumns, List targetColumns) {
			assert referringColumns != null;
			assert targetColumns != null;

			if ( referringColumns.size() != targetColumns.size() ) {
				throw new MappingException( "FK column counts did not match" );
			}
		}

		private class ColumnMappingsImpl implements ForeignKey.ColumnMappings {
			private final Table referringTable;
			private final Table targetTable;
			private final List<ForeignKey.ColumnMapping> columnMappings;

			public ColumnMappingsImpl(
					Table referringTable,
					Table targetTable,
					List<ForeignKey.ColumnMapping> columnMappings) {
				this.referringTable = referringTable;
				this.targetTable = targetTable;
				this.columnMappings = columnMappings;
			}

			@Override
			public Table getReferringTable() {
				return referringTable;
			}

			@Override
			public Table getTargetTable() {
				return targetTable;
			}

			@Override
			public List<ForeignKey.ColumnMapping> getColumnMappings() {
				return columnMappings;
			}

			@Override
			public List<PhysicalColumn> getTargetColumns() {
				return getColumns( columnMapping -> columnMapping.getTargetColumn() );
			}

			@Override
			public List<PhysicalColumn> getReferringColumns() {
				return getColumns( columnMapping -> columnMapping.getReferringColumn() );
			}

			private List<PhysicalColumn> getColumns(Function<ForeignKey.ColumnMapping, PhysicalColumn> mapper) {
				return columnMappings
						.stream()
						.map( mapper )
						.collect( Collectors.toList() );
			}
		}

		private class ColumnMappingImpl implements ForeignKey.ColumnMapping {
			private final PhysicalColumn referringColumn;
			private final PhysicalColumn targetColumn;

			public ColumnMappingImpl(PhysicalColumn referringColumn, PhysicalColumn targetColumn) {
				this.referringColumn = referringColumn;
				this.targetColumn = targetColumn;
			}

			@Override
			public PhysicalColumn getReferringColumn() {
				return referringColumn;
			}

			@Override
			public PhysicalColumn getTargetColumn() {
				return targetColumn;
			}
		}
	}

	public interface Callback {
		default void namespaceBuilt(MappedNamespace mappedNamespace, Namespace namespace) {
		}

		default void tableBuilt(MappedTable mappedTable, Table table) {
		}

		default void columnBuilt(MappedColumn mappedColumn, Column column) {
		}

		default void primaryKeyBuilt(MappedPrimaryKey bootPk, PrimaryKey runtimePk) {
		}

		default void foreignKeyBuilt(org.hibernate.mapping.ForeignKey mappedFk, ForeignKey runtimeFk) {
		}

		default void uniqueKeyBuilt(org.hibernate.mapping.UniqueKey mappedUk, UniqueKey runtimeUk) {
		}

		default void sequenceBuilt(Sequence sequence) {
		}
	}
}
