/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitJoinTableNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;

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
		metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( Category.class )
				.addAnnotatedClass( Item.class )
				.addAnnotatedClass( Workflow.class )
				.getMetadataBuilder()
				.applyImplicitNamingStrategy( new MyNamingStrategy() )
				.build();
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
		public Identifier determineJoinTableName(ImplicitJoinTableNameSource source) {
			final String ownerPortion = transformEntityName( source.getOwningEntityNaming() );
			final String ownedPortion;
			if ( source.getNonOwningEntityNaming() != null ) {
				ownedPortion = transformEntityName( source.getNonOwningEntityNaming() );
			}
			else {
				ownedPortion = transformAttributePath( source.getAssociationOwningAttributePath() );
			}

			return toIdentifier( ownerPortion + "_" + ownedPortion, source.getBuildingContext() );
		}

		@Override
		public Identifier determineJoinColumnName(ImplicitJoinColumnNameSource source) {
			final String entityPortion = transformEntityName( source.getEntityNaming() );
			final String name;
			if ( source.getAttributePath() == null ) {
				name = entityPortion + "_" + source.getReferencedColumnName();
			}
			else {
				name = entityPortion + "_"
						+ transformAttributePath( source.getAttributePath() )
						+ "_" + source.getReferencedColumnName();
			}
			return toIdentifier( name, source.getBuildingContext() );
		}
	}
}
