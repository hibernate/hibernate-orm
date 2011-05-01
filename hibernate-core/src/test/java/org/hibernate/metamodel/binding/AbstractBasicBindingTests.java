/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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

import java.util.Iterator;

import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.source.Metadata;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.metamodel.source.spi.MetadataImplementor;
import org.hibernate.service.BasicServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.internal.BasicServiceRegistryImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Basic tests of {@code hbm.xml} and annotation binding code
 *
 * @author Steve Ebersole
 */
public abstract class AbstractBasicBindingTests extends BaseUnitTestCase {

	private BasicServiceRegistryImpl serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = (BasicServiceRegistryImpl) new ServiceRegistryBuilder().buildServiceRegistry();
	}

	@After
	public void tearDown() {
		serviceRegistry.destroy();
	}

	protected BasicServiceRegistry basicServiceRegistry() {
		return serviceRegistry;
	}

	@Test
	public void testSimpleEntityMapping() {
		checkSimpleEntityMaping( buildSimpleEntityBinding() );
	}

	protected void checkSimpleEntityMaping(EntityBinding entityBinding) {
		assertNotNull( entityBinding );
		assertNotNull( entityBinding.getEntityIdentifier() );
		assertNotNull( entityBinding.getEntityIdentifier().getValueBinding() );
		assertNull( entityBinding.getVersioningValueBinding() );

		AttributeBinding idAttributeBinding = entityBinding.getAttributeBinding( "id" );
		assertNotNull( idAttributeBinding );
		assertSame( idAttributeBinding, entityBinding.getEntityIdentifier().getValueBinding() );
		assertNotNull( idAttributeBinding.getAttribute() );
		assertNotNull( idAttributeBinding.getValue() );
		assertTrue( idAttributeBinding.getValue() instanceof Column );

		AttributeBinding nameBinding = entityBinding.getAttributeBinding( "name" );
		assertNotNull( nameBinding );
		assertNotNull( nameBinding.getAttribute() );
		assertNotNull( nameBinding.getValue() );
	}

	@Test
	public void testSimpleVersionedEntityMapping() {
		EntityBinding entityBinding = buildSimpleVersionedEntityBinding();
		assertNotNull( entityBinding );
		assertNotNull( entityBinding.getEntityIdentifier() );
		assertNotNull( entityBinding.getEntityIdentifier().getValueBinding() );
		assertNotNull( entityBinding.getVersioningValueBinding() );
		assertNotNull( entityBinding.getVersioningValueBinding().getAttribute() );

		AttributeBinding idAttributeBinding = entityBinding.getAttributeBinding( "id" );
		assertNotNull( idAttributeBinding );
		assertSame( idAttributeBinding, entityBinding.getEntityIdentifier().getValueBinding() );
		assertNotNull( idAttributeBinding.getAttribute() );
		assertNotNull( idAttributeBinding.getValue() );
		assertTrue( idAttributeBinding.getValue() instanceof Column );

		AttributeBinding nameBinding = entityBinding.getAttributeBinding( "name" );
		assertNotNull( nameBinding );
		assertNotNull( nameBinding.getAttribute() );
		assertNotNull( nameBinding.getValue() );
	}

	@Test
	public void testEntityWithManyToOneMapping() {
		MetadataImplementor metadata = buildMetadataWithManyToOne();
		EntityBinding entityWithManyToOneBinding = metadata.getEntityBinding( EntityWithManyToOne.class.getName() );
		EntityBinding simpleEntityBinding = metadata.getEntityBinding( SimpleEntity.class.getName() );
		checkSimpleEntityMaping( simpleEntityBinding );

		assertTrue(
				1 == simpleEntityBinding.getAttributeBinding( "id" ).getEntityReferencingAttributeBindings().size()
		);
		Iterator<EntityReferencingAttributeBinding> it = entityWithManyToOneBinding.getEntityReferencingAttributeBindings().iterator();
		assertTrue( it.hasNext() );
		assertSame( entityWithManyToOneBinding.getAttributeBinding( "simpleEntity" ), it.next() );
		assertFalse( it.hasNext() );
	}
	/*
	@Test
	public void testEntityWithElementCollection() {
		EntityBinding entityBinding = buildEntityWithElementCollectionBinding();

		assertNotNull( entityBinding );
		assertNotNull( entityBinding.getEntityIdentifier() );
		assertNotNull( entityBinding.getEntityIdentifier().getValueBinding() );
		assertNull( entityBinding.getVersioningValueBinding() );

		AttributeBinding idAttributeBinding = entityBinding.getAttributeBinding( "id" );
		assertNotNull( idAttributeBinding );
		assertSame( idAttributeBinding, entityBinding.getEntityIdentifier().getValueBinding() );
		assertNotNull( idAttributeBinding.getAttribute() );
		assertNotNull( idAttributeBinding.getValue() );
		assertTrue( idAttributeBinding.getValue() instanceof Column );

		AttributeBinding nameBinding = entityBinding.getAttributeBinding( "name" );
		assertNotNull( nameBinding );
		assertNotNull( nameBinding.getAttribute() );
		assertNotNull( nameBinding.getValue() );
	}
	*/

	public abstract EntityBinding buildSimpleVersionedEntityBinding();

	public abstract EntityBinding buildSimpleEntityBinding();

	public abstract MetadataImplementor buildMetadataWithManyToOne();

	//public abstract EntityBinding buildEntityWithElementCollectionBinding();
}
