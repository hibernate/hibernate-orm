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
package org.hibernate.metamodel.spi.binding;

import java.sql.Types;
import java.util.List;

import org.hibernate.FetchMode;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.internal.MetadataImpl;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.domain.Aggregate;
import org.hibernate.metamodel.spi.domain.Attribute;
import org.hibernate.metamodel.spi.domain.BasicType;
import org.hibernate.metamodel.spi.domain.Entity;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.JdbcDataType;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;

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
 * Basic tests of {@code hbm.xml} and annotation binding code
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
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

	@Test
	public void testSimpleEntityMapping() {
		MetadataSources sources = new MetadataSources( serviceRegistry );
		addSourcesForSimpleEntityBinding( sources );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();
		EntityBinding entityBinding = metadata.getEntityBinding( SimpleEntity.class.getName() );
		assertRoot( metadata, entityBinding );
		assertIdAndSimpleProperty( entityBinding );

		assertNull( entityBinding.getHierarchyDetails().getEntityVersion() );
	}

	@Test
	public void testSimpleVersionedEntityMapping() {
		MetadataSources sources = new MetadataSources( serviceRegistry );
		addSourcesForSimpleVersionedEntityBinding( sources );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();
		EntityBinding entityBinding = metadata.getEntityBinding( SimpleVersionedEntity.class.getName() );
		assertIdAndSimpleProperty( entityBinding );

		assertNotNull( entityBinding.getHierarchyDetails().getEntityVersion().getVersioningAttributeBinding() );
		assertNotNull( entityBinding.getHierarchyDetails().getEntityVersion().getVersioningAttributeBinding().getAttribute() );

		final BasicAttributeBinding versionAttributeBinding =
				entityBinding.getHierarchyDetails().getEntityVersion().getVersioningAttributeBinding();
		assertEquals( "does not have 1 relational binding", 1, versionAttributeBinding.getRelationalValueBindings().size() );
		assertEquals( "version is nullable", false, versionAttributeBinding.getRelationalValueBindings().get( 0 ).isNullable() );
	}

	protected void testEntityWithManyToOneMapping(String defaultManyToOneColumnReferencingId) {
		MetadataSources sources = new MetadataSources( serviceRegistry );
		addSourcesForSimpleEntityBinding( sources );
		addSourcesForManyToOne( sources );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();

		final String simpleEntityClassName = SimpleEntity.class.getName();
		EntityBinding simpleEntityBinding = metadata.getEntityBinding( simpleEntityClassName );
		assertIdAndSimpleProperty( simpleEntityBinding );

		EntityBinding entityWithManyToOneBinding = metadata.getEntityBinding( EntityWithManyToOnes.class.getName() );
		AttributeBinding attributeBinding = entityWithManyToOneBinding.locateAttributeBinding( "simpleEntity" );
		checkManyToOneAttributeBinding(
				metadata,
				entityWithManyToOneBinding,
				attributeBinding,
				SingularAttributeBinding.class.cast( 
						simpleEntityBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().getAttributeBinding()
				),
				defaultManyToOneColumnReferencingId,
				false
		);

		checkManyToOneAttributeBinding(
				metadata,
				entityWithManyToOneBinding,
				entityWithManyToOneBinding.locateAttributeBinding( "simpleEntityFromPropertyRef" ),
				SingularAttributeBinding.class.cast( simpleEntityBinding.locateAttributeBinding( "name" ) ),
				"simplename",
				true
		);
	}

	private void checkManyToOneAttributeBinding(
			MetadataImplementor metadata,
			EntityBinding entityWithManyToOneBinding,
			AttributeBinding attributeBinding, 
			SingularAttributeBinding referencedAttributeBinding,
			String manyToOneColumnName,
			boolean expectedNullable) {
		final EntityBinding referencedEntityBinding = referencedAttributeBinding.getContainer().seekEntityBinding();
		final String referencedEntityName = referencedEntityBinding.getEntityName();
		assertTrue( SingularAttributeBinding.class.isInstance( referencedAttributeBinding ) );
		assertEquals( 1, SingularAttributeBinding.class.cast( referencedAttributeBinding ).getRelationalValueBindings().size() );
		final Value referencedValue =
				SingularAttributeBinding.class.cast( referencedAttributeBinding )
						.getRelationalValueBindings().get( 0 ).getValue();
		assertTrue( Column.class.isInstance( referencedValue ) );
		final JdbcDataType referencedJdbcDataType = Column.class.cast( referencedValue ).getJdbcDataType();
				
		// binding model
		assertTrue( attributeBinding.isAssociation() );
		assertTrue( ManyToOneAttributeBinding.class.isInstance(  attributeBinding ) );
		ManyToOneAttributeBinding manyToOneAttributeBinding = (ManyToOneAttributeBinding) attributeBinding;
		assertEquals( referencedEntityName, manyToOneAttributeBinding.getReferencedEntityName() );
		assertSame( referencedEntityBinding, manyToOneAttributeBinding.getReferencedEntityBinding() );
		assertSame( CascadeStyles.NONE, manyToOneAttributeBinding.getCascadeStyle() );
		assertTrue( manyToOneAttributeBinding.isLazy() );
		assertSame( FetchMode.SELECT, manyToOneAttributeBinding.getFetchMode() );
		assertSame( FetchTiming.DELAYED, manyToOneAttributeBinding.getFetchTiming() );
		assertSame( entityWithManyToOneBinding, manyToOneAttributeBinding.getContainer() );
		Assert.assertEquals( "property", manyToOneAttributeBinding.getPropertyAccessorName() );
		assertTrue( manyToOneAttributeBinding.isIncludedInOptimisticLocking() );
		assertFalse( manyToOneAttributeBinding.isAlternateUniqueKey() );
		assertEquals( expectedNullable, manyToOneAttributeBinding.isNullable() );
		HibernateTypeDescriptor hibernateTypeDescriptor = manyToOneAttributeBinding.getHibernateTypeDescriptor();
		Assert.assertNull( hibernateTypeDescriptor.getExplicitTypeName() );
		Assert.assertEquals(
				referencedEntityName,
				hibernateTypeDescriptor.getJavaTypeDescriptor().getName().toString()
		);
		assertTrue( hibernateTypeDescriptor.isToOne() );
		assertTrue( hibernateTypeDescriptor.getTypeParameters().isEmpty() );		

		// domain model
		Attribute simpleEntityAttribute= entityWithManyToOneBinding.getEntity().locateAttribute( "simpleEntity" );
		assertEquals( "simpleEntity", simpleEntityAttribute.getName() );
		Assert.assertSame( entityWithManyToOneBinding.getEntity(), simpleEntityAttribute.getAttributeContainer() ) ;
		Assert.assertTrue( simpleEntityAttribute.isSingular() );
		SingularAttribute simpleEntitySingularAttribute = ( SingularAttribute ) simpleEntityAttribute;
		assertTrue( simpleEntitySingularAttribute.isTypeResolved() );
		assertSame(
				metadata.getEntityBinding( referencedEntityName ).getEntity(), 
				simpleEntitySingularAttribute.getSingularAttributeType() 
		);
		Entity simpleEntityAttributeType = (Entity) simpleEntitySingularAttribute.getSingularAttributeType();
		assertEquals( referencedEntityName, simpleEntityAttributeType.getName() );
		Assert.assertEquals(
				referencedEntityName,
				simpleEntityAttributeType.getDescriptor().getName().toString()
		);
		Assert.assertTrue( simpleEntityAttributeType.isAssociation() );
		assertFalse( simpleEntityAttributeType.isAggregate() );

		// relational
		List<RelationalValueBinding> relationalValueBindings = manyToOneAttributeBinding.getRelationalValueBindings();
		Assert.assertEquals( 1, relationalValueBindings.size() );
		RelationalValueBinding relationalValueBinding = relationalValueBindings.get( 0 );
		assertFalse( relationalValueBinding.isDerived() );
		assertTrue( relationalValueBinding.isIncludeInInsert() );
		assertTrue( relationalValueBinding.isIncludeInUpdate() );
		assertEquals( expectedNullable, relationalValueBinding.isNullable() );
		assertTrue( relationalValueBinding.getValue() instanceof Column );
		Column column = ( Column ) relationalValueBinding.getValue();
		Assert.assertEquals( Identifier.toIdentifier( manyToOneColumnName ), column.getColumnName() );
		JdbcDataType jdbcDataType = column.getJdbcDataType();
		assertEquals( referencedJdbcDataType.getTypeCode(), jdbcDataType.getTypeCode() );
		assertEquals( referencedJdbcDataType.getJavaType(), jdbcDataType.getJavaType() );
		assertEquals( referencedJdbcDataType.getTypeName(), jdbcDataType.getTypeName() );
		
		// locate the foreignKey
		boolean sourceColumnFound = false;
		for ( ForeignKey fk : relationalValueBinding.getTable().getForeignKeys() ) {
			for ( Column sourceColumn : fk.getSourceColumns() ) {
				if ( sourceColumn == column ) {
					assertFalse( "source column not found in more than one foreign key", sourceColumnFound );
					sourceColumnFound = true;
					assertEquals( 1, fk.getTargetColumns().size() );
					assertSame(
							referencedAttributeBinding.getRelationalValueBindings().get( 0 ).getValue(),
							fk.getTargetColumns().get( 0 )
					);
				}
			}
		}
		assertTrue( "foreign key with specified source column found", sourceColumnFound );
	}

	@Test
	public void testSimpleEntityWithSimpleComponentMapping() {
		MetadataSources sources = new MetadataSources( serviceRegistry );
		addSourcesForComponentBinding( sources );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();
		EntityBinding entityBinding = metadata.getEntityBinding( SimpleEntityWithSimpleComponent.class.getName() );
		assertRoot( metadata, entityBinding );
		assertIdAndSimpleProperty( entityBinding );

		EmbeddedAttributeBinding embeddedAttributeBinding =
				(EmbeddedAttributeBinding) entityBinding.locateAttributeBinding( "simpleComponent" );
		assertNotNull( embeddedAttributeBinding );
		assertSame( embeddedAttributeBinding.getAttribute().getSingularAttributeType(), embeddedAttributeBinding.getEmbeddableBinding().getAttributeContainer() );
		assertEquals( "simpleComponent", embeddedAttributeBinding.getAttributePath().getFullPath() );
		assertEquals( "simpleComponent", embeddedAttributeBinding.getEmbeddableBinding().getPathBase().getFullPath() );
		assertSame( entityBinding, embeddedAttributeBinding.getEmbeddableBinding().seekEntityBinding() );
		assertTrue( embeddedAttributeBinding.getAttribute().getSingularAttributeType() instanceof Aggregate );
	}

	public abstract void addSourcesForSimpleVersionedEntityBinding(MetadataSources sources);

	public abstract void addSourcesForSimpleEntityBinding(MetadataSources sources);

	public abstract void addSourcesForManyToOne(MetadataSources sources);

	public abstract void addSourcesForComponentBinding(MetadataSources sources);

	protected void assertIdAndSimpleProperty(EntityBinding entityBinding) {
		assertNotNull( entityBinding );
		assertNotNull( entityBinding.getHierarchyDetails().getEntityIdentifier() );
		assertNotNull( entityBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().getAttributeBinding() );

		AttributeBinding idAttributeBinding = entityBinding.locateAttributeBinding( "id" );
		assertNotNull( idAttributeBinding );
		assertSame( idAttributeBinding, entityBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().getAttributeBinding() );
		assertSame( LongType.INSTANCE, idAttributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping() );

		assertTrue( idAttributeBinding.getAttribute().isSingular() );
		assertNotNull( idAttributeBinding.getAttribute() );
		SingularAttributeBinding singularIdAttributeBinding = (SingularAttributeBinding) idAttributeBinding;
		assertFalse( singularIdAttributeBinding.isNullable() );
		SingularAttribute singularIdAttribute =  ( SingularAttribute ) idAttributeBinding.getAttribute();
		BasicType basicIdAttributeType = ( BasicType ) singularIdAttribute.getSingularAttributeType();
		assertEquals( Long.class.getName(), basicIdAttributeType.getDescriptor().getName().toString() );

		assertTrue( singularIdAttributeBinding.getRelationalValueBindings().size() == 1 );
		Value value = singularIdAttributeBinding.getRelationalValueBindings().get( 0 ).getValue();
		assertTrue( value instanceof Column );
		JdbcDataType idDataType = value.getJdbcDataType();
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
		assertNotNull( nameBinding.getRelationalValueBindings().size() );
		SingularAttribute singularNameAttribute =  nameBinding.getAttribute();
		BasicType basicNameAttributeType = (BasicType) singularNameAttribute.getSingularAttributeType();
		assertEquals( String.class.getName(), basicNameAttributeType.getDescriptor().getName().toString() );
		Assert.assertEquals( 1, nameBinding.getRelationalValueBindings().size() );
		Value nameValue = nameBinding.getRelationalValueBindings().get( 0 ).getValue();
		assertTrue( nameValue instanceof Column );
		JdbcDataType nameDataType = nameValue.getJdbcDataType();
		assertSame( String.class, nameDataType.getJavaType() );
		assertSame( Types.VARCHAR, nameDataType.getTypeCode() );
		assertSame( StringType.INSTANCE.getName(), nameDataType.getTypeName() );
	}

	protected void assertRoot(MetadataImplementor metadata, EntityBinding entityBinding) {
		assertTrue( entityBinding.isRoot() );
		assertSame( entityBinding, metadata.getRootEntityBinding( entityBinding.getEntityName() ) );
	}
}
