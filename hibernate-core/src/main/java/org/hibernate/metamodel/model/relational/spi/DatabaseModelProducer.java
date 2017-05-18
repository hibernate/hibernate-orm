/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedNamespace;
import org.hibernate.boot.model.relational.MappedSequence;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.mapping.MappedPrimaryKey;
import org.hibernate.metamodel.model.relational.internal.DatabaseModelImpl;
import org.hibernate.metamodel.model.relational.internal.InflightTable;
import org.hibernate.metamodel.model.relational.internal.NamespaceImpl;

/**
 * Component used to produce the fully-populated runtime view of the relational
 * model given the boot-time view.
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public class DatabaseModelProducer {
	private final PhysicalNamingStrategy namingStrategy;
	private final JdbcEnvironment jdbcEnvironment;

	public DatabaseModelProducer(BootstrapContext bootstrapContext) {
		this.namingStrategy = bootstrapContext.getMetadataBuildingOptions().getPhysicalNamingStrategy();
		this.jdbcEnvironment = bootstrapContext.getServiceRegistry()
				.getService( JdbcServices.class )
				.getJdbcEnvironment();
	}

	public DatabaseModel produceDatabaseModel(Database database, Callback callback) {
		return new Process( callback, database ).execute();
	}

	class Process {
		private final Callback callback;
		private final Database database;

		public Process(Callback callback, Database database) {
			this.callback = callback;
			this.database = database;
		}

		private DatabaseModel execute() {
			final DatabaseModelImpl databaseModel = new DatabaseModelImpl();

			for ( MappedNamespace bootModelNamespace : database.getNamespaces() ) {
				final NamespaceImpl runtimeModelNamespace = generateNamespace(
						databaseModel,
						bootModelNamespace
				);

				processTables( bootModelNamespace, runtimeModelNamespace );
				processSequences( bootModelNamespace, runtimeModelNamespace );
			}

			return databaseModel;
		}

		private NamespaceImpl generateNamespace(
				DatabaseModelImpl databaseModel,
				MappedNamespace bootModelNamespace) {
			final NamespaceImpl runtimeModelNamespace = new NamespaceImpl(
					namingStrategy.toPhysicalCatalogName( bootModelNamespace.getName().getCatalog(), jdbcEnvironment ),
					namingStrategy.toPhysicalSchemaName( bootModelNamespace.getName().getSchema(), jdbcEnvironment )
			);
			databaseModel.addNamespace( runtimeModelNamespace );
			callback.namespaceBuilt( bootModelNamespace, runtimeModelNamespace );
			return runtimeModelNamespace;
		}

		private void processTables(
				MappedNamespace bootModelNamespace,
				NamespaceImpl runtimeModelNamespace) {
			for ( MappedTable mappedTable : bootModelNamespace.getTables() ) {
				final InflightTable runtimeTable = mappedTable.generateRuntimeTable(
						namingStrategy,
						jdbcEnvironment
				);
				runtimeModelNamespace.addTable( runtimeTable );

				final Map<MappedColumn,Column> tableColumnXref = new HashMap<>();

				for ( MappedColumn mappedColumn : mappedTable.getMappedColumns() ) {
					final Column column = mappedColumn.generateRuntimeColumn(
							runtimeTable,
							namingStrategy,
							jdbcEnvironment
					);
					runtimeTable.addColumn( column );
					callback.columnBuilt( mappedColumn, column );
					tableColumnXref.put( mappedColumn, column );
				}

				final MappedPrimaryKey bootPk = mappedTable.getPrimaryKey();
				for ( org.hibernate.mapping.Column mappedColumn : bootPk.getColumns() ) {
					final Column column = tableColumnXref.get( mappedColumn );
					runtimeTable.getPrimaryKey().addColumn( column );
				}
				callback.primaryKeyBuilt( bootPk, runtimeTable.getPrimaryKey() );

				// todo - uk

				callback.tableBuilt( mappedTable, runtimeTable );
			}

			// todo - fk

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

		default void foreignKeyBuilt(ForeignKey fk) {
		}

		default void sequenceBuilt(Sequence sequence) {
		}
	}
}
