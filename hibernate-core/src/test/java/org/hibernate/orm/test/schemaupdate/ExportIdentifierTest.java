/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.internal.BootstrapContextImpl;
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
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@RequiresDialectFeature(DialectChecks.SupportsSequences.class)
public class ExportIdentifierTest extends BaseUnitTestCase {

	@Test
	@JiraKey( value = "HHH-12935" )
	public void testUniqueExportableIdentifier() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		final MetadataBuilderImpl.MetadataBuildingOptionsImpl options = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( ssr );
		options.setBootstrapContext( new BootstrapContextImpl( ssr, options ) );
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

			final Table table = new Table( "orm", namespace, Identifier.toIdentifier( name ), false );
			addExportIdentifier( table, exportIdentifierList, exportIdentifierSet );

			final ForeignKey foreignKey = new ForeignKey( table );
			foreignKey.setName( name );
			addExportIdentifier( foreignKey, exportIdentifierList, exportIdentifierSet );

			final Index index = new Index();
			index.setName( name );
			index.setTable( table );
			addExportIdentifier( index, exportIdentifierList, exportIdentifierSet );

			final PrimaryKey primaryKey = new PrimaryKey( table );
			primaryKey.setName( name );
			addExportIdentifier( primaryKey, exportIdentifierList, exportIdentifierSet );

			final UniqueKey uniqueKey = new UniqueKey( table );
			uniqueKey.setName( name );
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
							"orm",
							namespace.getName().catalog(),
							namespace.getName().schema(),
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
							Collections.emptySet()
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
							Collections.emptySet()
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
