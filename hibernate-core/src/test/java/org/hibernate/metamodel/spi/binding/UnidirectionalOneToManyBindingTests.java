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
package org.hibernate.metamodel.spi.binding;

import java.util.Collection;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.MetadataSourceProcessingOrder;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.internal.MetadataImpl;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.internal.StandardServiceRegistryImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.type.BagType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.SetType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class UnidirectionalOneToManyBindingTests extends BaseUnitTestCase {
	private StandardServiceRegistryImpl serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = (StandardServiceRegistryImpl) new ServiceRegistryBuilder().buildServiceRegistry();
	}

	@After
	public void tearDown() {
		serviceRegistry.destroy();
	}

//	@Test
//	public void testAnnotations() {
//		doTest( MetadataSourceProcessingOrder.ANNOTATIONS_FIRST );
//	}

	@Test
	public void testHbm() {
		doTest( MetadataSourceProcessingOrder.HBM_FIRST );
	}

	private void doTest(MetadataSourceProcessingOrder processingOrder) {
		MetadataSources sources = new MetadataSources( serviceRegistry );
//		sources.addAnnotatedClass( EntityWithBasicCollections.class );
		sources.addResource( "org/hibernate/metamodel/spi/binding/EntityWithUnidirectionalOneToManys.hbm.xml" );
		sources.addResource( "org/hibernate/metamodel/spi/binding/SimpleEntity.hbm.xml" );
		MetadataImpl metadata = (MetadataImpl) sources.getMetadataBuilder().with( processingOrder ).buildMetadata();

		final EntityBinding entityBinding = metadata.getEntityBinding( EntityWithUnidirectionalOneToManys.class.getName() );
		final EntityBinding simpleEntityBinding = metadata.getEntityBinding( SimpleEntity.class.getName() );
		assertNotNull( entityBinding );

		assertEquals( Identifier.toIdentifier( "SimpleEntity" ), simpleEntityBinding.getPrimaryTable().getLogicalName() );
		assertEquals( 1, simpleEntityBinding.getPrimaryTable().getPrimaryKey().getColumnSpan() );
		Column simpleEntityIdColumn = simpleEntityBinding.getPrimaryTable().getPrimaryKey().getColumns().get( 0 );
		assertEquals( Identifier.toIdentifier("id") , simpleEntityIdColumn.getColumnName() );

		checkResult(
				entityBinding,
				metadata.getCollection( EntityWithUnidirectionalOneToManys.class.getName() + ".theBag" ),
				BagType.class,
				Collection.class,
				simpleEntityBinding,
				entityBinding.getHierarchyDetails().getEntityIdentifier().getAttributeBinding(),
				Identifier.toIdentifier( "theBagOwner" ),
				FetchTiming.DELAYED
		);

		checkResult(
				entityBinding,
				metadata.getCollection( EntityWithUnidirectionalOneToManys.class.getName() + ".theSet" ),
				SetType.class,
				Set.class,
				simpleEntityBinding,
				entityBinding.getHierarchyDetails().getEntityIdentifier().getAttributeBinding(),
				Identifier.toIdentifier( "theSetOwner" ),
				FetchTiming.IMMEDIATE
		);

		checkResult(
				entityBinding,
				metadata.getCollection( EntityWithUnidirectionalOneToManys.class.getName() + ".thePropertyRefBag" ),
				BagType.class,
				Collection.class,
				simpleEntityBinding,
				(SingularAttributeBinding) entityBinding.locateAttributeBinding( "name" ),
				Identifier.toIdentifier( "ownerName" ),
				FetchTiming.EXTRA_DELAYED
		);
	}

	private <X extends CollectionType> void checkResult(
			EntityBinding collectionOwnerBinding,
			PluralAttributeBinding collectionBinding,
			Class<X> expectedCollectionTypeClass,
			Class<?> expectedCollectionJavaType,
			EntityBinding expectedElementEntityBinding,
			SingularAttributeBinding expectedKeyTargetAttributeBinding,
			Identifier expectedKeySourceColumnName,
			FetchTiming expectedFetchTiming) {
		assertEquals(
				PluralAttributeElementNature.ONE_TO_MANY,
				collectionBinding.getPluralAttributeElementBinding().getPluralAttributeElementNature()
		);
		assertSame( collectionBinding, collectionOwnerBinding.locateAttributeBinding( collectionBinding.getAttribute().getName() ) );
		assertEquals( expectedFetchTiming, collectionBinding.getFetchTiming()  );
		assertEquals( expectedFetchTiming != FetchTiming.IMMEDIATE, collectionBinding.isLazy() );

		final String role = collectionBinding.getAttribute().getRole();
		assertEquals(
				role,
				collectionOwnerBinding.getEntity().getName() + "." + collectionBinding.getAttribute().getName()
		);

		final PluralAttributeKeyBinding keyBinding = collectionBinding.getPluralAttributeKeyBinding();
		assertSame( expectedElementEntityBinding.getPrimaryTable(), keyBinding.getCollectionTable() );
		assertSame( collectionBinding, keyBinding.getPluralAttributeBinding() );
		assertFalse( keyBinding.isInverse() );

		final HibernateTypeDescriptor collectionHibernateTypeDescriptor = collectionBinding.getHibernateTypeDescriptor();
		assertNull( collectionHibernateTypeDescriptor.getExplicitTypeName() );
		assertEquals( expectedCollectionJavaType.getName(), collectionHibernateTypeDescriptor.getJavaTypeName() );
		assertTrue( collectionHibernateTypeDescriptor.getTypeParameters().isEmpty() );
		assertTrue( expectedCollectionTypeClass.isInstance( collectionHibernateTypeDescriptor.getResolvedTypeMapping() ) );
		assertFalse( collectionHibernateTypeDescriptor.getResolvedTypeMapping().isComponentType() );
		assertEquals(
				role,
				expectedCollectionTypeClass.cast( collectionHibernateTypeDescriptor.getResolvedTypeMapping() ).getRole()
		);

		SingularAttributeBinding keySourceAttributeBinding =
				( SingularAttributeBinding) expectedElementEntityBinding.locateAttributeBinding(
						"_" + role + "BackRef"
				);
		assertEquals( 1, keySourceAttributeBinding.getRelationalValueBindings().size() );
		Value keySourceValue = keySourceAttributeBinding.getRelationalValueBindings().get( 0 ).getValue();
		assertTrue( keySourceValue instanceof Column );
		Column keySourceColumn = (Column) keySourceValue;
		assertEquals( expectedKeySourceColumnName, keySourceColumn.getColumnName() );

		ForeignKey fk = keyBinding.getForeignKey();
		assertNotNull( fk );
		assertSame( ForeignKey.ReferentialAction.NO_ACTION, fk.getDeleteRule() );
		assertSame( ForeignKey.ReferentialAction.NO_ACTION, fk.getUpdateRule() );
		// FK name is null because no default FK name is generated until HHH-7092 is fixed
		assertNull( fk.getName() );

		assertSame( expectedElementEntityBinding.getPrimaryTable(), fk.getSourceTable() );
		assertEquals( 1, fk.getColumnSpan() );
		assertEquals( fk.getColumns(), fk.getSourceColumns() );
		assertEquals( 1, fk.getSourceColumns().size() );
		assertEquals( 1, fk.getTargetColumns().size() );
		assertSame( keySourceColumn, fk.getColumns().get( 0 ) );
		assertSame( keySourceColumn, fk.getSourceColumns().get( 0 ) );

		assertSame( collectionOwnerBinding.getPrimaryTable(), fk.getTargetTable() );
		assertEquals( 1, expectedKeyTargetAttributeBinding.getRelationalValueBindings().size() );
		assertSame(
				expectedKeyTargetAttributeBinding.getRelationalValueBindings().get( 0 ).getValue(),
				fk.getTargetColumns().get( 0 )
		);
		assertEquals(
				expectedKeyTargetAttributeBinding.getRelationalValueBindings().get( 0 ).getValue().getJdbcDataType(),
				fk.getColumns().get( 0 ).getJdbcDataType()
		);

		checkEquals(
				expectedKeyTargetAttributeBinding.getHibernateTypeDescriptor(),
				keyBinding.getHibernateTypeDescriptor()
		);

		assertEquals(
				expectedElementEntityBinding.getEntity().getName(),
				collectionBinding.getPluralAttributeElementBinding().getHibernateTypeDescriptor().getJavaTypeName()
		);
	}

	private void checkEquals(HibernateTypeDescriptor expected, HibernateTypeDescriptor actual) {
		assertEquals( expected.getExplicitTypeName(), actual.getExplicitTypeName() );
		assertEquals( expected.getJavaTypeName(), actual.getJavaTypeName() );
		assertEquals( expected.getTypeParameters(), actual.getTypeParameters() );
		assertEquals( expected.getResolvedTypeMapping(), actual.getResolvedTypeMapping() );
		assertEquals( expected.isToOne(), actual.isToOne() );
	}
}
