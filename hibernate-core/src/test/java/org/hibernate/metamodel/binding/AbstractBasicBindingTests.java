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

import java.sql.Types;
import java.util.Iterator;
import java.util.Set;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.binder.source.MetadataImplementor;
import org.hibernate.metamodel.binder.source.internal.MetadataImpl;
import org.hibernate.metamodel.domain.BasicType;
import org.hibernate.metamodel.domain.SingularAttribute;
import org.hibernate.metamodel.domain.TypeNature;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.Datatype;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.service.BasicServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.internal.BasicServiceRegistryImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;

import static junit.framework.Assert.assertEquals;
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
	private MetadataSources sources;

	@Before
	public void setUp() {
		serviceRegistry = (BasicServiceRegistryImpl) new ServiceRegistryBuilder().buildServiceRegistry();
		sources = new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() );
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
		MetadataImpl metadata = addSourcesForSimpleEntityBinding( sources );
		EntityBinding entityBinding = metadata.getEntityBinding( SimpleEntity.class.getName() );
		assertRoot( metadata, entityBinding );
		assertIdAndSimpleProperty( entityBinding );

		assertNull( entityBinding.getVersioningValueBinding() );
	}

	@Test
	public void testSimpleVersionedEntityMapping() {
		MetadataImpl metadata = addSourcesForSimpleVersionedEntityBinding( sources );
		EntityBinding entityBinding = metadata.getEntityBinding( SimpleVersionedEntity.class.getName() );
		assertIdAndSimpleProperty( entityBinding );

		assertNotNull( entityBinding.getVersioningValueBinding() );
		assertNotNull( entityBinding.getVersioningValueBinding().getAttribute() );
	}

	@Test
	public void testEntityWithManyToOneMapping() {
		MetadataImpl metadata = addSourcesForManyToOne( sources );

		EntityBinding simpleEntityBinding = metadata.getEntityBinding( SimpleEntity.class.getName() );
		assertIdAndSimpleProperty( simpleEntityBinding );

		Set<EntityReferencingAttributeBinding> referenceBindings = simpleEntityBinding.getAttributeBinding( "id" )
				.getEntityReferencingAttributeBindings();
		assertEquals( "There should be only one reference binding", 1, referenceBindings.size() );

		EntityReferencingAttributeBinding referenceBinding = referenceBindings.iterator().next();
		EntityBinding referencedEntityBinding = referenceBinding.getReferencedEntityBinding();
		// TODO - Is this assertion correct (HF)?
		assertEquals( "Should be the same entity binding", referencedEntityBinding, simpleEntityBinding );

		EntityBinding entityWithManyToOneBinding = metadata.getEntityBinding( ManyToOneEntity.class.getName() );
		Iterator<EntityReferencingAttributeBinding> it = entityWithManyToOneBinding.getEntityReferencingAttributeBindings()
				.iterator();
		assertTrue( it.hasNext() );
		assertSame( entityWithManyToOneBinding.getAttributeBinding( "simpleEntity" ), it.next() );
		assertFalse( it.hasNext() );
	}

	public abstract MetadataImpl addSourcesForSimpleVersionedEntityBinding(MetadataSources sources);

	public abstract MetadataImpl addSourcesForSimpleEntityBinding(MetadataSources sources);

	public abstract MetadataImpl addSourcesForManyToOne(MetadataSources sources);

	protected void assertIdAndSimpleProperty(EntityBinding entityBinding) {
		assertNotNull( entityBinding );
		assertNotNull( entityBinding.getEntityIdentifier() );
		assertNotNull( entityBinding.getEntityIdentifier().getValueBinding() );

		AttributeBinding idAttributeBinding = entityBinding.getAttributeBinding( "id" );
		assertNotNull( idAttributeBinding );
		assertSame( idAttributeBinding, entityBinding.getEntityIdentifier().getValueBinding() );
		assertSame( LongType.INSTANCE, idAttributeBinding.getHibernateTypeDescriptor().getExplicitType() );

		assertTrue( idAttributeBinding.getAttribute().isSingular() );
		assertNotNull( idAttributeBinding.getAttribute() );
		SingularAttribute singularIdAttribute =  ( SingularAttribute ) idAttributeBinding.getAttribute();
		assertSame( TypeNature.BASIC, singularIdAttribute.getSingularAttributeType().getNature() );
		BasicType basicIdAttributeType = ( BasicType ) singularIdAttribute.getSingularAttributeType();
		assertSame( Long.class, basicIdAttributeType.getJavaType().getClassReference() );

		assertNotNull( idAttributeBinding.getValue() );
		assertTrue( idAttributeBinding.getValue() instanceof Column );
		Datatype idDataType = ( (Column) idAttributeBinding.getValue() ).getDatatype();
		assertSame( Long.class, idDataType.getJavaType() );
		assertSame( Types.BIGINT, idDataType.getTypeCode() );
		assertSame( LongType.INSTANCE.getName(), idDataType.getTypeName() );

		AttributeBinding nameBinding = entityBinding.getAttributeBinding( "name" );
		assertNotNull( nameBinding );
		assertSame( StringType.INSTANCE, nameBinding.getHibernateTypeDescriptor().getExplicitType() );
		assertNotNull( nameBinding.getAttribute() );
		assertNotNull( nameBinding.getValue() );

		assertTrue( nameBinding.getAttribute().isSingular() );
		assertNotNull( nameBinding.getAttribute() );
		SingularAttribute singularNameAttribute =  ( SingularAttribute ) nameBinding.getAttribute();
		assertSame( TypeNature.BASIC, singularNameAttribute.getSingularAttributeType().getNature() );
		BasicType basicNameAttributeType = ( BasicType ) singularNameAttribute.getSingularAttributeType();
		assertSame( String.class, basicNameAttributeType.getJavaType().getClassReference() );

		assertNotNull( nameBinding.getValue() );
		// until HHH-6380 is fixed, need to call getValues()
		assertEquals( 1, nameBinding.getValuesSpan() );
		Iterator<SimpleValue> it = nameBinding.getValues().iterator();
		assertTrue( it.hasNext() );
		SimpleValue nameValue = it.next();
		assertTrue( nameValue instanceof Column );
		Datatype nameDataType = nameValue.getDatatype();
		assertSame( String.class, nameDataType.getJavaType() );
		assertSame( Types.VARCHAR, nameDataType.getTypeCode() );
		assertSame( StringType.INSTANCE.getName(), nameDataType.getTypeName() );
	}

	protected void assertRoot(MetadataImplementor metadata, EntityBinding entityBinding) {
		assertTrue( entityBinding.isRoot() );
		assertSame( entityBinding, metadata.getRootEntityBinding( entityBinding.getEntity().getName() ) );
	}
}
