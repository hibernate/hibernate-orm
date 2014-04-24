/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi.binding.basiccollections;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.internal.MetadataImpl;
import org.hibernate.metamodel.spi.PluralAttributeElementNature;
import org.hibernate.metamodel.spi.PluralAttributeNature;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityIdentifier;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeKeyBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.type.BagType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.SetType;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractBasicCollectionBindingTests extends BaseUnitTestCase {
	private StandardServiceRegistryImpl serviceRegistry;
	private MetadataImpl metadata;

	@Before
	public void setUp() {
		serviceRegistry = ( StandardServiceRegistryImpl ) new StandardServiceRegistryBuilder().build();
		MetadataSources metadataSources = new MetadataSources( serviceRegistry );
		addSources( metadataSources );
		metadata = ( MetadataImpl ) metadataSources.getMetadataBuilder().build();
	}

	@After
	public void tearDown() {
		serviceRegistry.destroy();
	}

	public abstract void addSources(MetadataSources sources);

	@Test
	public void testBasicCollections() {
		final EntityBinding entityBinding = metadata.getEntityBinding( EntityWithBasicCollections.class.getName() );
		final EntityIdentifier entityIdentifier = entityBinding.getHierarchyDetails().getEntityIdentifier();
		assertNotNull( entityBinding );

		// TODO: this will fail until HHH-7121 is fixed
		//assertTrue( entityBinding.getPrimaryTable().locateColumn( "`name`" ).isUnique() );

		assertBasicCollectionBinding(
				entityBinding,
				metadata.getCollection( EntityWithBasicCollections.class.getName() + "#theBag" ),
				BagType.class,
				Collection.class,
				String.class,
				entityBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().getAttributeBinding(),
				Identifier.toIdentifier( "EntityWithBasicCollections_theBag" ),
				Identifier.toIdentifier( "owner_id" ),
				FetchTiming.IMMEDIATE,
				true
		);

		assertBasicCollectionBinding(
				entityBinding,
				metadata.getCollection( EntityWithBasicCollections.class.getName() + "#theSet" ),
				SetType.class,
				Set.class,
				String.class,
				entityBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().getAttributeBinding(),
				Identifier.toIdentifier( "EntityWithBasicCollections_theSet" ),
				Identifier.toIdentifier( "pid" ),
				FetchTiming.EXTRA_LAZY,
				true
		);

		assertBasicCollectionBinding(
				entityBinding,
				metadata.getCollection( EntityWithBasicCollections.class.getName() + "#thePropertyRefSet" ),
				SetType.class,
				Set.class,
				Integer.class,
				( SingularAttributeBinding ) entityBinding.locateAttributeBinding( "name" ),
				Identifier.toIdentifier( "EntityWithBasicCollections_thePropertyRefSet" ),
				Identifier.toIdentifier( "pid" ),
				FetchTiming.DELAYED,
				false
		);
	}

	private <X extends CollectionType> void assertBasicCollectionBinding(
			EntityBinding collectionOwnerBinding,
			PluralAttributeBinding collectionBinding,
			Class<X> expectedCollectionTypeClass,
			Class<?> expectedCollectionJavaClass,
			Class<?> expectedElementJavaClass,
			SingularAttributeBinding expectedKeyTargetAttributeBinding,
			Identifier expectedCollectionTableName,
			Identifier expectedKeySourceColumnName,
			FetchTiming expectedFetchTiming,
			boolean expectedElementNullable) {
		assertNotNull( collectionBinding );
		assertSame(
				collectionBinding,
				collectionOwnerBinding.locateAttributeBinding( collectionBinding.getAttribute().getName() )
		);
		assertSame( collectionOwnerBinding, collectionBinding.getContainer().seekEntityBinding() );

		TableSpecification collectionTable =  collectionBinding.getPluralAttributeKeyBinding().getCollectionTable();
		assertNotNull( collectionTable );
		assertEquals( expectedCollectionTableName, collectionTable.getLogicalName() );
		PluralAttributeKeyBinding keyBinding = collectionBinding.getPluralAttributeKeyBinding();
		assertSame( collectionBinding, keyBinding.getPluralAttributeBinding() );
		HibernateTypeDescriptor collectionHibernateTypeDescriptor = collectionBinding.getHibernateTypeDescriptor();
		assertNull( collectionHibernateTypeDescriptor.getExplicitTypeName() );
		assertEquals(
				expectedCollectionJavaClass.getName(),
				collectionHibernateTypeDescriptor.getJavaTypeDescriptor().getName().toString()
		);
		assertTrue( collectionHibernateTypeDescriptor.getTypeParameters().isEmpty() );
		assertTrue( expectedCollectionTypeClass.isInstance( collectionHibernateTypeDescriptor.getResolvedTypeMapping() ) );
		assertFalse( collectionHibernateTypeDescriptor.getResolvedTypeMapping().isComponentType() );
		final String role = collectionBinding.getAttribute().getRole();
		assertEquals(
				role,
				collectionOwnerBinding.getEntityName() + "." + collectionBinding.getAttribute().getName()
		);
		assertEquals(
				role,
				expectedCollectionTypeClass.cast( collectionHibernateTypeDescriptor.getResolvedTypeMapping() ).getRole()
		);

		assertEquals( expectedFetchTiming, collectionBinding.getFetchTiming() );
		assertEquals( expectedFetchTiming != FetchTiming.IMMEDIATE, collectionBinding.isLazy() );

		List<RelationalValueBinding> keyRelationalValueBindings = keyBinding.getRelationalValueBindings();
		assertNotNull( keyRelationalValueBindings );
		for( RelationalValueBinding keyRelationalValueBinding : keyRelationalValueBindings ) {
			assertSame( collectionTable, keyRelationalValueBinding.getTable() );
		}
		assertEquals( 1, keyRelationalValueBindings.size() );
		assertEquals( 1, expectedKeyTargetAttributeBinding.getRelationalValueBindings().size() );
		Value expectedFKTargetValue = expectedKeyTargetAttributeBinding.getRelationalValueBindings().get( 0 ).getValue();
		assertFalse( keyRelationalValueBindings.get( 0 ).isDerived() );
		assertEquals( expectedKeySourceColumnName, ( (Column) keyRelationalValueBindings.get( 0 ).getValue() ).getColumnName() );
		assertEquals( expectedFKTargetValue.getJdbcDataType(),  keyRelationalValueBindings.get( 0 ).getValue().getJdbcDataType() );

		assertFalse( keyBinding.isCascadeDeleteEnabled() );
		checkEquals(
				expectedKeyTargetAttributeBinding.getHibernateTypeDescriptor(),
				keyBinding.getHibernateTypeDescriptor()
		);
		assertFalse( keyBinding.isInverse() );
		Assert.assertEquals(
				PluralAttributeElementNature.BASIC,
				collectionBinding.getPluralAttributeElementBinding().getNature()
		);
		assertEquals(
				expectedElementJavaClass.getName(),
				collectionBinding.getPluralAttributeElementBinding().getHibernateTypeDescriptor().getJavaTypeDescriptor().getName().toString()
		);
		assertEquals(
				expectedElementJavaClass,
				collectionBinding.getPluralAttributeElementBinding().getHibernateTypeDescriptor().getResolvedTypeMapping().getReturnedClass()

		);
		assertEquals( 1,
					  collectionBinding.getPluralAttributeElementBinding()
							  .getRelationalValueContainer()
							  .relationalValueBindings()
							  .size()
		);
		RelationalValueBinding elementRelationalValueBinding = collectionBinding.getPluralAttributeElementBinding().getRelationalValueContainer().relationalValueBindings().get( 0 );
		assertEquals( expectedElementNullable, elementRelationalValueBinding.isNullable() );
		if ( collectionBinding.getAttribute().getPluralAttributeNature() == PluralAttributeNature.BAG ) {
			assertEquals( 0, collectionTable.getPrimaryKey().getColumnSpan() );
		}
		else if ( collectionBinding.getAttribute().getPluralAttributeNature() == PluralAttributeNature.SET ) {
			if ( expectedElementNullable ) {
				assertEquals( 0, collectionTable.getPrimaryKey().getColumnSpan() );
			}
			else {
				assertEquals( 2, collectionTable.getPrimaryKey().getColumnSpan() );
				assertSame( keyRelationalValueBindings.get( 0 ).getValue(), collectionTable.getPrimaryKey().getColumns().get( 0 ) );
				assertSame( elementRelationalValueBinding.getValue(),  collectionTable.getPrimaryKey().getColumns().get( 1 ) );
			}
		}
	}

	private void checkEquals(HibernateTypeDescriptor expected, HibernateTypeDescriptor actual) {
		assertEquals( expected.getExplicitTypeName(), actual.getExplicitTypeName() );
		assertEquals( expected.getJavaTypeDescriptor(), actual.getJavaTypeDescriptor() );
		assertEquals( expected.getTypeParameters(), actual.getTypeParameters() );
		assertEquals( expected.getResolvedTypeMapping(), actual.getResolvedTypeMapping() );
		assertEquals( expected.isToOne(), actual.isToOne() );
	}
}
