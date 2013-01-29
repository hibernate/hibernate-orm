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
package org.hibernate.metamodel.binding;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.metamodel.MetadataSourceProcessingOrder;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * @author Steve Ebersole
 */
public class BasicCollectionBindingTests extends BaseUnitTestCase {
	private StandardServiceRegistryImpl serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder().build();
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
		sources.addResource( "org/hibernate/metamodel/binding/EntityWithBasicCollections.hbm.xml" );
		MetadataImpl metadata = (MetadataImpl) sources.getMetadataBuilder().with( processingOrder ).build();

		final EntityBinding entityBinding = metadata.getEntityBinding( EntityWithBasicCollections.class.getName() );
		assertNotNull( entityBinding );

		PluralAttributeBinding bagBinding = metadata.getCollection( EntityWithBasicCollections.class.getName() + ".theBag" );
		assertNotNull( bagBinding );
		assertSame( bagBinding, entityBinding.locateAttributeBinding( "theBag" ) );
		assertNotNull( bagBinding.getCollectionTable() );
		assertEquals( CollectionElementNature.BASIC, bagBinding.getCollectionElement().getCollectionElementNature() );
		assertEquals( String.class.getName(), ( (BasicCollectionElement) bagBinding.getCollectionElement() ).getHibernateTypeDescriptor().getJavaTypeName() );

		PluralAttributeBinding setBinding = metadata.getCollection( EntityWithBasicCollections.class.getName() + ".theSet" );
		assertNotNull( setBinding );
		assertSame( setBinding, entityBinding.locateAttributeBinding( "theSet" ) );
		assertNotNull( setBinding.getCollectionTable() );
		assertEquals( CollectionElementNature.BASIC, setBinding.getCollectionElement().getCollectionElementNature() );
		assertEquals( String.class.getName(), ( (BasicCollectionElement) setBinding.getCollectionElement() ).getHibernateTypeDescriptor().getJavaTypeName() );
	}
}
