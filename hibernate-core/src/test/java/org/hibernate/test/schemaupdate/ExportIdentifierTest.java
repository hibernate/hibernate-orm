/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.NamedAuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SimpleAuxiliaryDatabaseObject;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@RequiresDialectFeature(DialectChecks.SupportsSequences.class)
public class ExportIdentifierTest extends BaseUnitTestCase {

	@Test
	@TestForIssue( jiraKey = "HHH-12935" )
	public void testUniqueExportableIdentifier() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();
		final MetadataBuildingOptions options = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( ssr );
		final Database database = new Database( options );

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

		final List<String> exportIdentifierList = new ArrayList<>();
		final Set<String> exportIdentifierSet = new HashSet<>();

		try {
			addTables( "aTable" , database.getNamespaces(), exportIdentifierList, exportIdentifierSet );
			addSimpleAuxiliaryDatabaseObject( database.getNamespaces(), exportIdentifierList, exportIdentifierSet );
			addNamedAuxiliaryDatabaseObjects(
					"aNamedAuxiliaryDatabaseObject", database.getNamespaces(), exportIdentifierList, exportIdentifierSet
			);
			addSequences( "aSequence", database.getNamespaces(), exportIdentifierList, exportIdentifierSet );

			assertEquals( exportIdentifierList.size(), exportIdentifierSet.size() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	private void addTables(
			String name,
			Iterable<Namespace> namespaces,
			List<String> exportIdentifierList,
			Set<String> exportIdentifierSet) {
		for ( Namespace namespace : namespaces ) {

			final Table table = new Table( namespace, Identifier.toIdentifier( name ), false );
			addExportIdentifier( table, exportIdentifierList, exportIdentifierSet );

			final ForeignKey foreignKey = new ForeignKey();
			foreignKey.setName( name );
			foreignKey.setTable( table );
			addExportIdentifier( foreignKey, exportIdentifierList, exportIdentifierSet );

			final Index index = new Index();
			index.setName( name );
			index.setTable( table );
			addExportIdentifier( index, exportIdentifierList, exportIdentifierSet );

			final PrimaryKey primaryKey = new PrimaryKey( table );
			primaryKey.setName( name );
			addExportIdentifier( primaryKey, exportIdentifierList, exportIdentifierSet );

			final UniqueKey uniqueKey = new UniqueKey();
			uniqueKey.setName( name );
			uniqueKey.setTable( table );
			addExportIdentifier( uniqueKey, exportIdentifierList, exportIdentifierSet );
		}
	}

	private void addSequences(
			String name,
			Iterable<Namespace> namespaces,
			List<String> exportIdentifierList,
			Set<String> exportIdentifierSet) {

		for ( Namespace namespace : namespaces ) {
			addExportIdentifier(
					new Sequence(
							namespace.getName().getCatalog(),
							namespace.getName().getSchema(),
							Identifier.toIdentifier( name )
					),
					exportIdentifierList,
					exportIdentifierSet
			);
		}
	}

	private void addSimpleAuxiliaryDatabaseObject(
			Iterable<Namespace> namespaces,
			List<String> exportIdentifierList,
			Set<String> exportIdentifierSet) {
		for ( Namespace namespace : namespaces ) {
			addExportIdentifier(
					new SimpleAuxiliaryDatabaseObject(
							namespace,
							"create",
							"drop",
							Collections.<String>emptySet()
					),
					exportIdentifierList,
					exportIdentifierSet
			);
		}
	}

	private void addNamedAuxiliaryDatabaseObjects(
			String name,
			Iterable<Namespace> namespaces,
			List<String> exportIdentifierList,
			Set<String> exportIdentifierSet) {
		for ( Namespace namespace : namespaces ) {
			addExportIdentifier(
					new NamedAuxiliaryDatabaseObject(
							name,
							namespace,
							"create",
							"drop",
							Collections.<String>emptySet()
					),
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
		assertEquals( exportIdentifierList.size(), exportIdentifierSet.size() );
	}
}
