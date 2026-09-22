/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import jakarta.annotation.Nonnull;

import org.hibernate.boot.model.naming.spi.AssociationTableNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.boot.model.source.spi.AttributePath;

import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.spi.JoinColumnNamingInput;
import org.hibernate.boot.model.naming.spi.CollectionKeyNamingInput;
import org.hibernate.boot.model.naming.spi.AssociationKeyNamingInput;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BaseUnitTest
public class FullyQualifiedEntityNameNamingStrategyTest {
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

	@BeforeAll
	public void setUp() {
		ssr = ServiceRegistryUtil.serviceRegistry();
		metadata = (MetadataImplementor) MetadataBuildingTestHelper.buildMetadataWithImplicitNaming(
				ssr,
				new MappingSources()
						.addManagedClass( Category.class )
						.addManagedClass( Item.class )
						.addManagedClass( Workflow.class ),
				new MyNamingStrategy()
		);
	}

	@AfterAll
	public void tearDown() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@JiraKey(value = "HHH-4312")
	public void testEntityTable() throws Exception {
		final PersistentClass classMapping = metadata.getEntityBinding( Workflow.class.getName() );
		final String expectedTableName = transformEntityName( Workflow.class.getName() );
		assertEquals( expectedTableName, classMapping.getTable().getName() );
	}

	@Test
	@JiraKey(value = "HHH-9327")
	public void testElementCollectionTable() {
		final Collection collectionMapping = metadata.getCollectionBinding(
				Workflow.class.getName() + ".localized"
		);
		final String expectedTableName = transformEntityName( Workflow.class.getName() ) + "_localized";
		assertEquals( expectedTableName, collectionMapping.getCollectionTable().getName() );
	}

	@Test
	@JiraKey(value = "HHH-9327")
	public void testManyToManyCollectionTable() {
		final Collection collectionMapping = metadata.getCollectionBinding(
				Category.class.getName() + "." + "items"
		);
		final String expectedTableName = transformEntityName( Category.class.getName() ) + "_" + transformEntityName( Item.class.getName() );
		assertEquals( expectedTableName, collectionMapping.getCollectionTable().getName() );
	}

	@Test
	@JiraKey( value = "HHH-9327")
	public void testManyToManyForeignKeys() {
		final Collection ownerCollectionMapping = metadata.getCollectionBinding(
				Category.class.getName() + "." + "items"
		);
		final String expectedOwnerFK = transformEntityName( Category.class.getName() ) + "_id";
		final String expectedInverseFK = transformEntityName( Item.class.getName() ) + "_items_id";

		boolean ownerFKFound = false;
		boolean inverseFKFound = false;
		for ( ForeignKey foreignKey : ownerCollectionMapping.getCollectionTable().getForeignKeyCollection() ) {
			final String fkColumnName = foreignKey.getColumn( 0 ).getName();
			if ( expectedOwnerFK.equals( fkColumnName ) ) {
				ownerFKFound = true;
			}
			else if ( expectedInverseFK.equals( fkColumnName ) ) {
				inverseFKFound = true;
			}
		}
		assertTrue( ownerFKFound );
		assertTrue( inverseFKFound );
	}

	static String transformEntityName(String entityName) {
		return entityName.replaceAll( "\\.", "_" );
	}

	public static class MyNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {

		private static final long serialVersionUID = -5713413771290957530L;

		@Override
		protected String transformEntityName(EntityNaming entityNaming) {
			if ( entityNaming.getClassName() != null ) {
				return FullyQualifiedEntityNameNamingStrategyTest.transformEntityName( entityNaming.getClassName() );
			}
			return super.transformEntityName( entityNaming );
		}

		@Override
		@Nonnull
		public LogicalName determineAssociationTableName(@Nonnull AssociationTableNamingInput source, @Nonnull ImplicitNamingContext context) {
			final String ownerPortion = transformEntityName( source.owner() );
			final String ownedPortion;
			if ( source.target() != null ) {
				ownedPortion = transformEntityName( source.target() );
			}
			else {
				ownedPortion = transformAttributePath( AttributePath.parse( source.attributePath() ) );
			}

			return context.implicitName( ownerPortion + "_" + ownedPortion );
		}

			@Override
		@Nonnull
		public LogicalName determineJoinColumnName(@Nonnull JoinColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
			return joinName( transformEntityName( input.owner() ) + "_" + transformAttributePath( AttributePath.parse( input.attributePath() ) ), input.reference(), context );
		}

		@Override
		@Nonnull
		public LogicalName determineCollectionKeyColumnName(@Nonnull CollectionKeyNamingInput input, @Nonnull ImplicitNamingContext context) {
			return joinName( transformEntityName( input.owner() ) + input.inverseAttributePath().map( path -> "_" + transformAttributePath( AttributePath.parse( path ) ) ).orElse( "" ), input.reference(), context );
		}

		@Override
		@Nonnull
		public LogicalName determineAssociationKeyColumnName(@Nonnull AssociationKeyNamingInput input, @Nonnull ImplicitNamingContext context) {
			return joinName( transformEntityName( input.target() ) + "_" + transformAttributePath( AttributePath.parse( input.attributePath() ) ), input.reference(), context );
		}

	}
}
