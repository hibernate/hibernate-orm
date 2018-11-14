/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.MappedNamespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SimpleAuxiliaryDatabaseObject;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.model.relational.internal.ColumnMappingsImpl;
import org.hibernate.metamodel.model.relational.spi.Exportable;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.PhysicalNamingStrategy;
import org.hibernate.metamodel.model.relational.spi.PhysicalTable;
import org.hibernate.metamodel.model.relational.spi.RuntimeDatabaseModelProducer;
import org.hibernate.metamodel.model.relational.spi.UniqueKey;
import org.hibernate.naming.Identifier;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.DialectFeatureChecks;
import org.hibernate.testing.junit5.RequiresDialectFeature;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSequences.class)
public class ExportIdentifierTest extends BaseSchemaUnitTestCase {

	private PhysicalNamingStrategy namingStrategy;
	private JdbcEnvironment jdbcEnvironment;
	private MutableIdentifierGeneratorFactory identifierGeneratorFactory;
	private TypeConfiguration typeConfiguration;
	private Database database;

	@Override
	protected void beforeEach(SchemaScope scope) {
		namingStrategy = getMetadata().getMetadataBuildingOptions().getPhysicalNamingStrategy();
		jdbcEnvironment = getStandardServiceRegistry().getService( JdbcServices.class ).getJdbcEnvironment();
		identifierGeneratorFactory = getStandardServiceRegistry().getService( MutableIdentifierGeneratorFactory.class );
		typeConfiguration = getMetadata().getTypeConfiguration();

		database = new Database( getMetadata().getMetadataBuildingOptions() );

		database.locateNamespace( null, null );
		database.locateNamespace( Identifier.toIdentifier( "catalog1" ), null );
		database.locateNamespace( Identifier.toIdentifier( "catalog2" ), null );
		database.locateNamespace( null, Identifier.toIdentifier( "schema1" ) );
		database.locateNamespace( null, Identifier.toIdentifier( "schema2" ) );
		database.locateNamespace(
				Identifier.toIdentifier( "catalog_both_1" ),
				Identifier.toIdentifier( "schema_both_1" )
		);
		database.locateNamespace(
				Identifier.toIdentifier( "catalog_both_2" ),
				Identifier.toIdentifier( "schema_both_2" )
		);
	}

	@SchemaTest
	@TestForIssue(jiraKey = "HHH-12935")
	public void testUniqueExportableIdentifier(SchemaScope scope) {
		final List<String> exportIdentifierList = new ArrayList<>();
		final Set<String> exportIdentifierSet = new HashSet<>();

		addTables( "aTable", database.getNamespaces(), exportIdentifierList, exportIdentifierSet );
		addAuxiliaryDatabaseObjects(
				"aNamedAuxiliaryDatabaseObject",
				database.getNamespaces(),
				exportIdentifierList,
				exportIdentifierSet
		);
		addSequences( "aSequence", database.getNamespaces(), exportIdentifierList, exportIdentifierSet );

		assertEquals( exportIdentifierList.size(), exportIdentifierSet.size() );
	}

	private void addTables(
			String name,
			Iterable<MappedNamespace> namespaces,
			List<String> exportIdentifierList,
			Set<String> exportIdentifierSet) {
		for ( MappedNamespace namespace : namespaces ) {
			final Table table = new Table( namespace, Identifier.toIdentifier( name ), false );
			PhysicalTable runtimeTable = (PhysicalTable) table.generateRuntimeTable(
					namingStrategy,
					jdbcEnvironment,
					identifierGeneratorFactory,
					new TestCallback(),
					typeConfiguration
			);
			addExportIdentifier( runtimeTable, exportIdentifierList, exportIdentifierSet );

			ForeignKey runtimeForeignKey = runtimeTable.createForeignKey(
					"aForeignKey",
					true,
					null,
					true,
					true,
					null,
					new ColumnMappingsImpl( runtimeTable, null, Collections.emptyList(), Collections.emptyList() )
			);
			addExportIdentifier( runtimeForeignKey, exportIdentifierList, exportIdentifierSet );

			final Index index = new Index();
			index.setName( "anIndex" );
			index.setTable( table );
			addExportIdentifier(
					index.generateRuntimeIndex(
							runtimeTable,
							namingStrategy,
							jdbcEnvironment,
							typeConfiguration
					),
					exportIdentifierList,
					exportIdentifierSet
			);

			addExportIdentifier(
					new UniqueKey( Identifier.toIdentifier( "aUniqueKey" ), runtimeTable ),
					exportIdentifierList,
					exportIdentifierSet
			);
		}
	}

	private class TestCallback implements RuntimeDatabaseModelProducer.Callback {
	}

	private void addSequences(
			String name,
			Iterable<MappedNamespace> namespaces,
			List<String> exportIdentifierList,
			Set<String> exportIdentifierSet) {

		for ( MappedNamespace namespace : namespaces ) {
			Sequence sequence = new Sequence(
					namespace.getName().getCatalog(),
					namespace.getName().getSchema(),
					Identifier.toIdentifier( name )
			);
			addExportIdentifier(
					sequence.generateRuntimeSequence( namingStrategy, jdbcEnvironment ),
					exportIdentifierList,
					exportIdentifierSet
			);
		}
	}

	private void addAuxiliaryDatabaseObjects(
			String name,
			Iterable<MappedNamespace> namespaces,
			List<String> exportIdentifierList,
			Set<String> exportIdentifierSet) {
		for ( MappedNamespace namespace : namespaces ) {
			SimpleAuxiliaryDatabaseObject auxiliaryDatabaseObject = new SimpleAuxiliaryDatabaseObject(
					namespace,
					"create",
					"drop",
					Collections.emptySet()
			);
			addExportIdentifier(
					auxiliaryDatabaseObject.generateRuntimeAuxiliaryDatabaseObject( getDialect() ),
					exportIdentifierList,
					exportIdentifierSet
			);
		}
	}

	private void addExportIdentifier(
			Exportable exportable,
			List<String> exportIdentifierList,
			Set<String> exportIdentifierSet) {
		exportIdentifierList.add( exportable.getExportIdentifier() );
		exportIdentifierSet.add( exportable.getExportIdentifier() );
	}
}
