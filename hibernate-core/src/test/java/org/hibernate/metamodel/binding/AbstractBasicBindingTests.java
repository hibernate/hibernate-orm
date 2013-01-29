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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.domain.BasicType;
import org.hibernate.metamodel.domain.SingularAttribute;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.Datatype;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
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

	private StandardServiceRegistryImpl serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder().build();
	}

	@After
	public void tearDown() {
		serviceRegistry.destroy();
	}

	protected ServiceRegistry basicServiceRegistry() {
		return serviceRegistry;
	}

	@Test
	public void testSimpleEntityMapping() {
		MetadataSources sources = new MetadataSources( serviceRegistry );
		addSourcesForSimpleEntityBinding( sources );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();
		EntityBinding entityBinding = metadata.getEntityBinding( SimpleEntity.class.getName() );
		assertRoot( metadata, entityBinding );
		assertIdAndSimpleProperty( entityBinding );

		assertNull( entityBinding.getHierarchyDetails().getVersioningAttributeBinding() );
	}

	@Test
	public void testSimpleVersionedEntityMapping() {
		MetadataSources sources = new MetadataSources( serviceRegistry );
		addSourcesForSimpleVersionedEntityBinding( sources );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();
		EntityBinding entityBinding = metadata.getEntityBinding( SimpleVersionedEntity.class.getName() );
		assertIdAndSimpleProperty( entityBinding );

		assertNotNull( entityBinding.getHierarchyDetails().getVersioningAttributeBinding() );
		assertNotNull( entityBinding.getHierarchyDetails().getVersioningAttributeBinding().getAttribute() );
	}

	@Test
	public void testEntityWithManyToOneMapping() {
		MetadataSources sources = new MetadataSources( serviceRegistry );
		addSourcesForSimpleEntityBinding( sources );
		addSourcesForManyToOne( sources );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();

		EntityBinding simpleEntityBinding = metadata.getEntityBinding( SimpleEntity.class.getName() );
		assertIdAndSimpleProperty( simpleEntityBinding );

		Set<SingularAssociationAttributeBinding> referenceBindings = simpleEntityBinding.locateAttributeBinding( "id" )
				.getEntityReferencingAttributeBindings();
		assertEquals( "There should be only one reference binding", 1, referenceBindings.size() );

		SingularAssociationAttributeBinding referenceBinding = referenceBindings.iterator().next();
		EntityBinding referencedEntityBinding = referenceBinding.getReferencedEntityBinding();
		// TODO - Is this assertion correct (HF)?
		assertEquals( "Should be the same entity binding", referencedEntityBinding, simpleEntityBinding );

		EntityBinding entityWithManyToOneBinding = metadata.getEntityBinding( ManyToOneEntity.class.getName() );
		Iterator<SingularAssociationAttributeBinding> it = entityWithManyToOneBinding.getEntityReferencingAttributeBindings()
				.iterator();
		assertTrue( it.hasNext() );
		assertSame( entityWithManyToOneBinding.locateAttributeBinding( "simpleEntity" ), it.next() );
		assertFalse( it.hasNext() );
	}

	@Test
	public void testSimpleEntityWithSimpleComponentMapping() {
		MetadataSources sources = new MetadataSources( serviceRegistry );
		addSourcesForComponentBinding( sources );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();
		EntityBinding entityBinding = metadata.getEntityBinding( SimpleEntityWithSimpleComponent.class.getName() );
		assertRoot( metadata, entityBinding );
		assertIdAndSimpleProperty( entityBinding );

		ComponentAttributeBinding componentAttributeBinding = (ComponentAttributeBinding) entityBinding.locateAttributeBinding( "simpleComponent" );
		assertNotNull( componentAttributeBinding );
		assertSame( componentAttributeBinding.getAttribute().getSingularAttributeType(), componentAttributeBinding.getAttributeContainer() );
		assertEquals( SimpleEntityWithSimpleComponent.class.getName() + ".simpleComponent", componentAttributeBinding.getPathBase() );
		assertSame( entityBinding, componentAttributeBinding.seekEntityBinding() );
		assertNotNull( componentAttributeBinding.getComponent() );
	}

	public abstract void addSourcesForSimpleVersionedEntityBinding(MetadataSources sources);

	public abstract void addSourcesForSimpleEntityBinding(MetadataSources sources);

	public abstract void addSourcesForManyToOne(MetadataSources sources);

	public abstract void addSourcesForComponentBinding(MetadataSources sources);

	protected void assertIdAndSimpleProperty(EntityBinding entityBinding) {
		assertNotNull( entityBinding );
		assertNotNull( entityBinding.getHierarchyDetails().getEntityIdentifier() );
		assertNotNull( entityBinding.getHierarchyDetails().getEntityIdentifier().getValueBinding() );

		AttributeBinding idAttributeBinding = entityBinding.locateAttributeBinding( "id" );
		assertNotNull( idAttributeBinding );
		assertSame( idAttributeBinding, entityBinding.getHierarchyDetails().getEntityIdentifier().getValueBinding() );
		assertSame( LongType.INSTANCE, idAttributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping() );

		assertTrue( idAttributeBinding.getAttribute().isSingular() );
		assertNotNull( idAttributeBinding.getAttribute() );
		SingularAttributeBinding singularIdAttributeBinding = (SingularAttributeBinding) idAttributeBinding;
		assertFalse( singularIdAttributeBinding.isNullable() );
		SingularAttribute singularIdAttribute =  ( SingularAttribute ) idAttributeBinding.getAttribute();
		BasicType basicIdAttributeType = ( BasicType ) singularIdAttribute.getSingularAttributeType();
		assertSame( Long.class, basicIdAttributeType.getClassReference() );

		assertNotNull( singularIdAttributeBinding.getValue() );
		assertTrue( singularIdAttributeBinding.getValue() instanceof Column );
		Datatype idDataType = ( (Column) singularIdAttributeBinding.getValue() ).getDatatype();
		assertSame( Long.class, idDataType.getJavaType() );
		assertSame( Types.BIGINT, idDataType.getTypeCode() );
		assertSame( LongType.INSTANCE.getName(), idDataType.getTypeName() );

		assertNotNull( entityBinding.locateAttributeBinding( "name" ) );
		assertNotNull( entityBinding.locateAttributeBinding( "name" ).getAttribute() );
		assertTrue( entityBinding.locateAttributeBinding( "name" ).getAttribute().isSingular() );

		SingularAttributeBinding nameBinding = (SingularAttributeBinding) entityBinding.locateAttributeBinding( "name" );
		assertTrue( nameBinding.isNullable() );
		assertSame( StringType.INSTANCE, nameBinding.getHibernateTypeDescriptor().getResolvedTypeMapping() );
		assertNotNull( nameBinding.getAttribute() );
		assertNotNull( nameBinding.getValue() );
		SingularAttribute singularNameAttribute =  ( SingularAttribute ) nameBinding.getAttribute();
		BasicType basicNameAttributeType = ( BasicType ) singularNameAttribute.getSingularAttributeType();
		assertSame( String.class, basicNameAttributeType.getClassReference() );

		assertNotNull( nameBinding.getValue() );
		SimpleValue nameValue = (SimpleValue) nameBinding.getValue();
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
