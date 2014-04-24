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
package org.hibernate.metamodel.spi.binding.onetomany;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.internal.MetadataImpl;
import org.hibernate.metamodel.spi.PluralAttributeElementNature;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeKeyBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.type.BagType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ListType;
import org.hibernate.type.MapType;
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
 * @author Gail Badner
 */
public abstract class AbstractUnidirectionalOneToManyBindingTests extends BaseUnitTestCase {
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
	public void testOneToMany() {
		final EntityBinding entityBinding = metadata.getEntityBinding( EntityWithUnidirectionalOneToMany.class.getName() );
		final EntityBinding simpleEntityBinding = metadata.getEntityBinding( ReferencedEntity.class.getName() );
		assertNotNull( entityBinding );

		assertEquals(
				Identifier.toIdentifier( "ReferencedEntity" ),
				simpleEntityBinding.getPrimaryTable().getLogicalName()
		);
		assertEquals( 1, simpleEntityBinding.getPrimaryTable().getPrimaryKey().getColumnSpan() );
		Column simpleEntityIdColumn = simpleEntityBinding.getPrimaryTable().getPrimaryKey().getColumns().get( 0 );
		assertEquals( Identifier.toIdentifier( "id" ), simpleEntityIdColumn.getColumnName() );

		checkResult(
				entityBinding,
				metadata.getCollection( EntityWithUnidirectionalOneToMany.class.getName() + "#theBag" ),
				BagType.class,
				Collection.class,
				simpleEntityBinding,
				entityBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().getAttributeBinding(),
				Identifier.toIdentifier( "theBagOwner" ),
				FetchTiming.DELAYED,
				true
		);

		checkResult(
				entityBinding,
				metadata.getCollection( EntityWithUnidirectionalOneToMany.class.getName() + "#theSet" ),
				SetType.class,
				Set.class,
				simpleEntityBinding,
				entityBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().getAttributeBinding(),
				Identifier.toIdentifier( "theSetOwner" ),
				FetchTiming.IMMEDIATE,
				false
		);

		checkResult(
				entityBinding,
				metadata.getCollection( EntityWithUnidirectionalOneToMany.class.getName() + "#theList" ),
				ListType.class,
				List.class,
				simpleEntityBinding,
				entityBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().getAttributeBinding(),
				Identifier.toIdentifier( "theListOwner" ),
				FetchTiming.IMMEDIATE,
				false
		);

		checkResult(
				entityBinding,
				metadata.getCollection( EntityWithUnidirectionalOneToMany.class.getName() + "#theMap" ),
				MapType.class,
				Map.class,
				simpleEntityBinding,
				entityBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().getAttributeBinding(),
				Identifier.toIdentifier( "theMapOwner" ),
				FetchTiming.DELAYED,
				false
		);

		checkResult(
				entityBinding,
				metadata.getCollection( EntityWithUnidirectionalOneToMany.class.getName() + "#thePropertyRefSet" ),
				BagType.class,
				Collection.class,
				simpleEntityBinding,
				( SingularAttributeBinding ) entityBinding.locateAttributeBinding( "name" ),
				Identifier.toIdentifier( "ownerName" ),
				FetchTiming.EXTRA_LAZY,
				false
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
			FetchTiming expectedFetchTiming,
			boolean expectedNullableCollectionKey) {
		Assert.assertEquals(
				PluralAttributeElementNature.ONE_TO_MANY,
				collectionBinding.getPluralAttributeElementBinding().getNature()
		);
		assertSame(
				collectionBinding,
				collectionOwnerBinding.locateAttributeBinding( collectionBinding.getAttribute().getName() )
		);
		assertEquals( expectedFetchTiming, collectionBinding.getFetchTiming() );
		assertEquals( expectedFetchTiming != FetchTiming.IMMEDIATE, collectionBinding.isLazy() );

		final String role = collectionBinding.getAttribute().getRole();
		assertEquals(
				role,
				collectionOwnerBinding.getEntityName() + "." + collectionBinding.getAttribute().getName()
		);

		final PluralAttributeKeyBinding keyBinding = collectionBinding.getPluralAttributeKeyBinding();
		assertSame( expectedElementEntityBinding.getPrimaryTable(), keyBinding.getCollectionTable() );
		assertSame( collectionBinding, keyBinding.getPluralAttributeBinding() );
		assertFalse( keyBinding.isInverse() );

		final HibernateTypeDescriptor collectionHibernateTypeDescriptor = collectionBinding.getHibernateTypeDescriptor();
		assertNull( collectionHibernateTypeDescriptor.getExplicitTypeName() );
		assertEquals(				expectedCollectionJavaType.getName(),
				collectionHibernateTypeDescriptor.getJavaTypeDescriptor().getName().toString()
		);
		assertEquals(
				expectedCollectionJavaType.getName(),
				collectionHibernateTypeDescriptor.getJavaTypeDescriptor().getName().toString()
		);
		assertTrue( collectionHibernateTypeDescriptor.getTypeParameters().isEmpty() );
		assertTrue( expectedCollectionTypeClass.isInstance( collectionHibernateTypeDescriptor.getResolvedTypeMapping() ) );
		assertFalse( collectionHibernateTypeDescriptor.getResolvedTypeMapping().isComponentType() );
		assertEquals(
				role,
				expectedCollectionTypeClass.cast( collectionHibernateTypeDescriptor.getResolvedTypeMapping() ).getRole()
		);

		List<RelationalValueBinding> keyRelationalValueBinding = keyBinding.getRelationalValueBindings();
		assertNotNull( keyRelationalValueBinding );
		assertFalse( keyBinding.isCascadeDeleteEnabled() );

		assertSame( expectedElementEntityBinding.getPrimaryTable(), keyBinding.getCollectionTable() );
		assertEquals( 1, keyRelationalValueBinding.size() );

		SingularAttributeBinding keySourceAttributeBinding =
				( SingularAttributeBinding ) expectedElementEntityBinding.locateAttributeBinding(
						"_" + role + "BackRef"
				);
		assertEquals( "expected "+keyBinding.getPluralAttributeBinding() +" has a " + expectedNullableCollectionKey +" collection key",expectedNullableCollectionKey, keyBinding.isNullable() );
		if ( keyBinding.isNullable() ) {
			assertNull( keySourceAttributeBinding );
		}
		else {
			assertEquals( 1, keySourceAttributeBinding.getRelationalValueBindings().size() );
			Value keySourceValue = keySourceAttributeBinding.getRelationalValueBindings().get( 0 ).getValue();
			assertTrue( keySourceValue instanceof Column );
			Column keySourceColumn = ( Column ) keySourceValue;
			assertEquals( expectedKeySourceColumnName, keySourceColumn.getColumnName() );
		}

		assertEquals( 1, expectedKeyTargetAttributeBinding.getRelationalValueBindings().size() );
		assertEquals(
				expectedKeyTargetAttributeBinding.getRelationalValueBindings().get( 0 ).getValue().getJdbcDataType(),
				keyRelationalValueBinding.get( 0 ).getValue().getJdbcDataType()
		);

		checkEquals(
				expectedKeyTargetAttributeBinding.getHibernateTypeDescriptor(),
				keyBinding.getHibernateTypeDescriptor()
		);

		assertEquals(
				expectedElementEntityBinding.getEntityName(),
				collectionBinding.getPluralAttributeElementBinding().getHibernateTypeDescriptor().getJavaTypeDescriptor().getName().toString()
		);
	}

	private void checkEquals(HibernateTypeDescriptor expected, HibernateTypeDescriptor actual) {
		assertEquals( expected.getExplicitTypeName(), actual.getExplicitTypeName() );
		assertEquals(
				expected.getJavaTypeDescriptor().getName(),
				actual.getJavaTypeDescriptor().getName()
		);
		assertEquals( expected.getTypeParameters(), actual.getTypeParameters() );
		assertEquals( expected.getResolvedTypeMapping(), actual.getResolvedTypeMapping() );
		assertEquals( expected.isToOne(), actual.isToOne() );
	}
}
