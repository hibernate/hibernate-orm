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

import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.metamodel.MetadataSourceProcessingOrder;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.internal.MetadataImpl;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.internal.StandardServiceRegistryImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class BasicCollectionBindingTests extends BaseUnitTestCase {
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
		sources.addResource( "org/hibernate/metamodel/spi/binding/EntityWithBasicCollections.hbm.xml" );
		MetadataImpl metadata = (MetadataImpl) sources.getMetadataBuilder().with( processingOrder ).buildMetadata();

		final EntityBinding entityBinding = metadata.getEntityBinding( EntityWithBasicCollections.class.getName() );
		assertNotNull( entityBinding );

		PluralAttributeBinding bagBinding = metadata.getCollection( EntityWithBasicCollections.class.getName() + ".theBag" );
		assertNotNull( bagBinding );
		assertSame( bagBinding, entityBinding.locateAttributeBinding( "theBag" ) );
		assertNotNull( bagBinding.getCollectionTable() );
		assertEquals( Identifier.toIdentifier( "`EntityWithBasicCollections_theBag`" ), bagBinding.getCollectionTable().getLogicalName() );
		PluralAttributeKeyBinding bagKeyBinding = bagBinding.getPluralAttributeKeyBinding();
		assertSame( bagBinding, bagKeyBinding.getPluralAttributeBinding() );

		ForeignKey fkBag = bagKeyBinding.getForeignKey();
		assertNotNull( fkBag );
		assertSame( bagBinding.getCollectionTable(), fkBag.getSourceTable() );
		assertEquals( 1, fkBag.getColumnSpan() );
		Iterator<Column> fkBagColumnIterator = fkBag.getColumns().iterator();
		Iterator<Column> fkBagSourceColumnIterator = fkBag.getSourceColumns().iterator();
		assertNotNull( fkBagColumnIterator );
		assertNotNull( fkBagSourceColumnIterator );
		assertTrue( fkBagColumnIterator.hasNext() );
		assertTrue( fkBagSourceColumnIterator.hasNext() );
		assertEquals( Identifier.toIdentifier( "`owner_id`" ), fkBagColumnIterator.next().getColumnName() );
		assertEquals( Identifier.toIdentifier( "`owner_id`" ), fkBagSourceColumnIterator.next().getColumnName() );
		assertFalse( fkBagColumnIterator.hasNext() );
		assertFalse( fkBagSourceColumnIterator.hasNext() );
		assertSame( entityBinding.getPrimaryTable(), fkBag.getTargetTable() );
		assertEquals( entityBinding.getPrimaryTable().getPrimaryKey().getColumns(), fkBag.getTargetColumns() );
		assertSame( ForeignKey.ReferentialAction.NO_ACTION, fkBag.getDeleteRule() );
		assertSame( ForeignKey.ReferentialAction.NO_ACTION, fkBag.getUpdateRule() );
		// FK is null because no default FK name is generated until HHH-7092 is fixed
		assertNull( fkBag.getName() );
		assertFalse( bagKeyBinding.isInverse() );
		assertEquals( PluralAttributeElementNature.BASIC, bagBinding.getPluralAttributeElementBinding().getPluralAttributeElementNature() );
		assertEquals( String.class.getName(), bagBinding.getPluralAttributeElementBinding().getHibernateTypeDescriptor().getJavaTypeName() );

		PluralAttributeBinding setBinding = metadata.getCollection( EntityWithBasicCollections.class.getName() + ".theSet" );
		assertNotNull( setBinding );
		assertSame( setBinding, entityBinding.locateAttributeBinding( "theSet" ) );
		assertNotNull( setBinding.getCollectionTable() );
		assertEquals( Identifier.toIdentifier( "`EntityWithBasicCollections_theSet`" ), setBinding.getCollectionTable().getLogicalName() );
		PluralAttributeKeyBinding setKeyBinding = setBinding.getPluralAttributeKeyBinding();
		assertSame( setBinding, setKeyBinding.getPluralAttributeBinding() );

		ForeignKey fkSet = setKeyBinding.getForeignKey();
		assertNotNull( fkSet );
		assertSame( setBinding.getCollectionTable(), fkSet.getSourceTable() );
		assertEquals( 1, fkSet.getColumnSpan() );
		Iterator<Column> fkSetColumnIterator = fkSet.getColumns().iterator();
		Iterator<Column> fkSetSourceColumnIterator = fkSet.getSourceColumns().iterator();
		assertNotNull( fkSetColumnIterator );
		assertNotNull( fkSetSourceColumnIterator );
		assertTrue( fkSetColumnIterator.hasNext() );
		assertTrue( fkSetSourceColumnIterator.hasNext() );
		assertEquals( Identifier.toIdentifier( "`pid`" ), fkSetColumnIterator.next().getColumnName() );
		assertEquals( Identifier.toIdentifier( "`pid`" ), fkSetSourceColumnIterator.next().getColumnName() );
		assertFalse( fkSetColumnIterator.hasNext() );
		assertFalse( fkSetSourceColumnIterator.hasNext() );
		assertSame( entityBinding.getPrimaryTable(), fkSet.getTargetTable() );
		assertEquals( entityBinding.getPrimaryTable().getPrimaryKey().getColumns(), fkSet.getTargetColumns() );
		assertSame( ForeignKey.ReferentialAction.NO_ACTION, fkSet.getDeleteRule() );
		assertSame( ForeignKey.ReferentialAction.NO_ACTION, fkSet.getUpdateRule() );
		// FK is null because no default FK name is generated until HHH-7092 is fixed
		assertNull( fkSet.getName() );
		assertFalse( setKeyBinding.isInverse() );
		assertEquals( PluralAttributeElementNature.BASIC, setBinding.getPluralAttributeElementBinding().getPluralAttributeElementNature() );
		assertEquals( String.class.getName(), setBinding.getPluralAttributeElementBinding().getHibernateTypeDescriptor().getJavaTypeName() );
	}
}
