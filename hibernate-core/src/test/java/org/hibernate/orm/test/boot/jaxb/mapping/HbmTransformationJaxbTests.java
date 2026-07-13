/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.transform.HbmXmlTransformer;
import org.hibernate.boot.jaxb.hbm.transform.UnsupportedFeatureHandling;
import org.hibernate.boot.jaxb.internal.stax.HbmEventReader;
import org.hibernate.boot.jaxb.mapping.GenerationTiming;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributeOverrideImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCompositeUserTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToManyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToOneImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToManyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToOneImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTransientImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.xsd.MappingXsdSupport;
import org.hibernate.orm.test.boot.jaxb.JaxbHelper;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.type.descriptor.java.IntegerJavaType;
import org.junit.jupiter.api.Test;

import jakarta.persistence.InheritanceType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.jaxb.JaxbHelper.withStaxEventReader;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class HbmTransformationJaxbTests {
	@Test
	public void hbmTransformationTest(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/basic/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 1 );
			assertThat( transformed.getPackage() ).isEqualTo( "org.hibernate.orm.test.boot.jaxb.mapping" );

			final JaxbEntityImpl ormEntity = transformed.getEntities().get( 0 );
			assertThat( ormEntity.getName() ).isNull();
			assertThat( ormEntity.getClazz() ).isEqualTo( "SimpleEntity" );

			assertThat( ormEntity.getAttributes().getIdAttributes() ).hasSize( 1 );
			assertThat( ormEntity.getAttributes().getBasicAttributes() ).hasSize( 1 );
			assertThat( ormEntity.getAttributes().getEmbeddedAttributes() ).isEmpty();
			assertThat( ormEntity.getAttributes().getOneToOneAttributes() ).isEmpty();
			assertThat( ormEntity.getAttributes().getManyToOneAttributes() ).isEmpty();
			assertThat( ormEntity.getAttributes().getAnyMappingAttributes() ).isEmpty();
			assertThat( ormEntity.getAttributes().getOneToManyAttributes() ).isEmpty();
			assertThat( ormEntity.getAttributes().getManyToManyAttributes() ).isEmpty();
			assertThat( ormEntity.getAttributes().getPluralAnyMappingAttributes() ).isEmpty();
		} );
	}

	@Test
	@JiraKey( "HHH-20451" )
	public void mapKeyManyToManyTransformationTest(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/ternary/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 2 );

			final JaxbEntityImpl mapKeyManyToManyEntity = transformed.getEntities().stream()
					.filter( e -> "MapKeyManyToManyEntity".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			assertThat( mapKeyManyToManyEntity.getAttributes().getManyToManyAttributes() ).hasSize( 1 );

			final JaxbManyToManyImpl managersAttr = mapKeyManyToManyEntity.getAttributes()
					.getManyToManyAttributes()
					.get( 0 );
			assertThat( managersAttr.getName() ).isEqualTo( "managers" );
			assertThat( managersAttr.getMapKeyJoinColumns() ).hasSize( 1 );
			assertThat( managersAttr.getMapKeyJoinColumns().get( 0 ).getName() ).isEqualTo( "siteId" );
		} );
	}

	@Test
	public void manyToOnePropertyRefTransformationTest(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/many-to-one-property-ref/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 2 );

			final JaxbEntityImpl sourceEntity = transformed.getEntities().stream()
					.filter( e -> "PropertyRefSourceEntity".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			assertThat( sourceEntity.getAttributes().getManyToOneAttributes() ).hasSize( 1 );

			final JaxbManyToOneImpl manyToOne = sourceEntity.getAttributes().getManyToOneAttributes().get( 0 );
			assertThat( manyToOne.getName() ).isEqualTo( "target" );
			assertThat( manyToOne.getTargetEntity() ).isEqualTo( "PropertyRefTargetEntity" );
			assertThat( manyToOne.getPropertyRef() ).isNotNull();
			assertThat( manyToOne.getPropertyRef().getName() ).isEqualTo( "name" );
			assertThat( manyToOne.getJoinColumnOrJoinFormula() ).isEmpty();
		} );
	}

	@Test
	public void testManyToManyOrphanRemovalHbmTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/manytomany/UserGroup.hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 2 );

			final JaxbEntityImpl userEntity = transformed.getEntities().stream()
					.filter( e -> "User".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			assertThat( userEntity.getAttributes().getManyToManyAttributes() ).isEmpty();
			assertThat( userEntity.getAttributes().getOneToManyAttributes() ).hasSize( 1 );
			final JaxbOneToManyImpl oneToMany = userEntity.getAttributes().getOneToManyAttributes().get( 0 );

					assertThat( oneToMany.getJoinTable() ).isNotNull();
					assertThat( oneToMany.getJoinTable().getName() ).isEqualTo( "UserGroup" );
					assertThat( oneToMany.getJoinTable().getJoinColumn() ).hasSize( 1 );
					assertThat( oneToMany.getJoinTable().getJoinColumn().get( 0 ).getName() ).isEqualTo( "name" );
					assertThat( oneToMany.getJoinTable().getInverseJoinColumn() ).hasSize( 1 );
					assertThat( oneToMany.getJoinTable().getInverseJoinColumn().get( 0 ).getName() ).isEqualTo( "groupName" );

			assertThat( oneToMany.getCascade() ).isNotNull();
			assertThat( oneToMany.getCascade().getCascadeAll() ).isNotNull();

			assertThat( oneToMany.isOrphanRemoval() ).isTrue();

			final var mapKeyJavaType = oneToMany.getMapKeyJavaType();
			assertThat( mapKeyJavaType ).isNotNull();
			assertThat( mapKeyJavaType ).isEqualTo( IntegerJavaType.class.getName() );
		} );
	}

	@Test
	@JiraKey( "HHH-20483" )
	public void testQuotedTableName(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/manytomany/UserGroup.hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 2 );

			final JaxbEntityImpl userEntity = transformed.getEntities().stream()
					.filter( e -> "User".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			assertThat( userEntity.getTable() ).isNotNull();
			assertThat( userEntity.getTable().getName() ).isEqualTo( "`User`" );

			final JaxbEntityImpl groupEntity = transformed.getEntities().stream()
					.filter( e -> "Group".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			assertThat( groupEntity.getTable() ).isNotNull();
			assertThat( groupEntity.getTable().getName() ).isEqualTo( "`Group`" );
		} );
	}

	@Test
	@JiraKey( "HHH-20484" )
	public void testNativeIdGeneratorTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/basic/nativeId.hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 1 );

			final JaxbEntityImpl entity = transformed.getEntities().get( 0 );
			assertThat( entity.getAttributes().getIdAttributes() ).hasSize( 1 );
			final JaxbIdImpl id = entity.getAttributes().getIdAttributes().get( 0 );

			assertThat( id.getName() ).isEqualTo( "id" );
			assertThat( id.getGeneratedValue() ).isNotNull();
			assertThat( id.getGenericGenerator() ).isNotNull();
			assertThat( id.getGenericGenerator().getClazz() ).isEqualTo( "native" );
			assertThat( id.getGenericGenerator().getName() ).isNotNull();
			assertThat( id.getGenericGenerator().getName() )
					.isEqualTo( id.getGeneratedValue().getGenerator() );
		} );
	}

	@Test
	@JiraKey( "HHH-20564" )
	public void testComponentGeneratedPropertyTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/component-generated/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 1 );
			assertThat( transformed.getEmbeddables() ).hasSize( 1 );

			final JaxbEmbeddableImpl embeddable = transformed.getEmbeddables().get( 0 );
			assertThat( embeddable.getAttributes().getBasicAttributes() ).hasSize( 1 );

			final JaxbBasicImpl basicAttr = embeddable.getAttributes().getBasicAttributes().get( 0 );
			assertThat( basicAttr.getName() ).isEqualTo( "generated" );
			assertThat( basicAttr.getGenerated() ).isEqualTo( GenerationTiming.ALWAYS );
		} );
	}

	@Test
	@JiraKey( "HHH-20566" )
	public void testJoinedSubclassInheritanceStrategy(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/joined-subclass/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 2 );

			final JaxbEntityImpl baseEntity = transformed.getEntities().stream()
					.filter( e -> "JoinedSubclassBase".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();
			assertThat( baseEntity.getInheritance() ).isNotNull();
			assertThat( baseEntity.getInheritance().getStrategy() ).isEqualTo( InheritanceType.JOINED );

			final JaxbEntityImpl childEntity = transformed.getEntities().stream()
					.filter( e -> "JoinedSubclassChild".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();
			assertThat( childEntity.getPrimaryKeyJoinColumns() ).isNotEmpty();
			assertThat( childEntity.getPrimaryKeyJoinColumns().get( 0 ).getName() ).isEqualTo( "base_id" );
		} );
	}

	@Test
	@JiraKey( "HHH-20566" )
	public void testUnionSubclassInheritanceStrategy(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/union-subclass/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 2 );

			final JaxbEntityImpl baseEntity = transformed.getEntities().stream()
					.filter( e -> "UnionSubclassBase".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();
			assertThat( baseEntity.getInheritance() ).isNotNull();
			assertThat( baseEntity.getInheritance().getStrategy() ).isEqualTo( InheritanceType.TABLE_PER_CLASS );

			final JaxbEntityImpl childEntity = transformed.getEntities().stream()
					.filter( e -> "UnionSubclassChild".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();
			assertThat( childEntity.getTable() ).isNotNull();
			assertThat( childEntity.getTable().getName() ).isEqualTo( "us_child" );
		} );
	}

	@Test
	public void testUnmappedPropertiesAreTransient(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/unmapped-property/hbm.xml", scope, transformed -> {
			final JaxbEntityImpl entity = transformed.getEntities().stream()
					.filter( e -> e.getClazz() != null && e.getClazz().endsWith( "UnmappedPropEntity" ) )
					.findFirst()
					.orElseThrow();

			final var attributes = entity.getAttributes();

			assertThat( attributes.getIdAttributes() )
					.extracting( JaxbIdImpl::getName )
					.contains( "id" );

			assertThat( attributes.getBasicAttributes() )
					.extracting( JaxbBasicImpl::getName )
					.contains( "name", "anotherCompositeName" )
					.doesNotContain( "compositeName" );

			final var transients = attributes.getTransients();

			assertThat( transients )
					.extracting( JaxbTransientImpl::getName )
					.as( "Unmapped field 'unmappedRef' should be marked as transient" )
					.contains( "unmappedRef" )
					.as( "compositeName has no backing field and access is field — should not be transient" )
					.doesNotContain( "compositeName" )
					.as( "Mapped properties should not be marked as transient" )
					.doesNotContain( "id", "name", "anotherCompositeName" );
		} );
	}

	@Test
	public void testSubselectEntityTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/subselect-entity/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 2 );

			final JaxbEntityImpl viewEntity = transformed.getEntities().stream()
					.filter( e -> "SubselectView".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			assertThat( viewEntity.getTableExpression() )
					.as( "Subselect entity should have a table-expression" )
					.isNotNull();
			assertThat( viewEntity.getTableExpression() ).contains( "select id, name, value from base_table" );
			assertThat( viewEntity.getTable() )
					.as( "Subselect entity should not have a regular table" )
					.isNull();
			assertThat( viewEntity.getSynchronizeTables() ).hasSize( 1 );
			assertThat( viewEntity.getSynchronizeTables().get( 0 ).getTable() ).isEqualTo( "base_table" );
			assertThat( viewEntity.isMutable() ).isFalse();

			for ( JaxbBasicImpl basic : viewEntity.getAttributes().getBasicAttributes() ) {
				if ( basic.getColumn() != null ) {
					assertThat( basic.getColumn().getTable() )
							.as( "Column for property '%s' should not have a table attribute on a subselect entity", basic.getName() )
							.isNull();
				}
			}
		} );
	}

	@Test
	@JiraKey( "HHH-20600" )
	public void testRecursiveComponentTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/recursive-component/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 1 );
			assertThat( transformed.getEmbeddables() ).hasSize( 1 );

			final JaxbEmbeddableImpl embeddable = transformed.getEmbeddables().get( 0 );
			assertThat( embeddable.getAttributes().getBasicAttributes() )
					.as( "Only the mapped 'name' property should be present" )
					.hasSize( 1 );
			assertThat( embeddable.getAttributes().getBasicAttributes().get( 0 ).getName() )
					.isEqualTo( "name" );
		} );
	}

	@Test
	@JiraKey( "HHH-20600" )
	public void testComponentPropertyAccessTransientDetection(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/component-access-transient/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEmbeddables() ).hasSize( 1 );

			final JaxbEmbeddableImpl embeddable = transformed.getEmbeddables().get( 0 );

			assertThat( embeddable.getAttributes().getBasicAttributes() )
					.extracting( JaxbBasicImpl::getName )
					.containsExactlyInAnyOrder( "street", "city" );

			assertThat( embeddable.getAttributes().getTransients() )
					.extracting( JaxbTransientImpl::getName )
					.as( "getFullAddress() is an unmapped getter — with property access it must be marked transient" )
					.contains( "fullAddress" )
					.as( "Mapped properties should not be marked transient" )
					.doesNotContain( "street", "city" );
		} );
	}

	@Test
	public void testCompositeElementParentTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/parent/composite-element-parent.hbm.xml", scope, transformed -> {
			assertThat( transformed.getEmbeddables() ).hasSize( 1 );

			final JaxbEmbeddableImpl embeddable = transformed.getEmbeddables().get( 0 );
			assertThat( embeddable.getAttributes().getParent() )
					.as( "<parent> in <composite-element> should produce <parent> in the embeddable" )
					.isEqualTo( "parent" );
		} );
	}

	@Test
	public void testComponentParentTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/parent/component-parent.hbm.xml", scope, transformed -> {
			assertThat( transformed.getEmbeddables() ).hasSize( 1 );

			final JaxbEmbeddableImpl embeddable = transformed.getEmbeddables().get( 0 );
			assertThat( embeddable.getAttributes().getParent() )
					.as( "<parent> in <component> should produce <parent> in the embeddable" )
					.isEqualTo( "owner" );
		} );
	}

	@Test
	@JiraKey( "HHH-20628" )
	public void testCompositeElementAccessTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/composite-element/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEmbeddables() ).hasSize( 1 );

			final JaxbEmbeddableImpl embeddable = transformed.getEmbeddables().get( 0 );
			assertThat( embeddable.getAccess() )
					.as( "Composite-element embeddable should have access=PROPERTY (HBM default)" )
					.isEqualTo( jakarta.persistence.AccessType.PROPERTY );
		} );
	}

	@Test
	@JiraKey( "HHH-20599" )
	public void testCompositeElementColumnTableTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/composite-element/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEmbeddables() ).hasSize( 1 );

			final JaxbEmbeddableImpl embeddable = transformed.getEmbeddables().get( 0 );
			for ( JaxbBasicImpl basic : embeddable.getAttributes().getBasicAttributes() ) {
				if ( basic.getColumn() != null ) {
					assertThat( basic.getColumn().getTable() )
							.as( "Column '%s' should not have a table attribute — it belongs to the collection table, not a secondary table",
									basic.getName() )
							.isNull();
				}
			}
		} );
	}

	@Test
	@JiraKey( "HHH-20598" )
	public void testSortNaturalTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/sort-natural/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 1 );

			final JaxbEntityImpl personEntity = transformed.getEntities().get( 0 );
			assertThat( personEntity.getAttributes().getElementCollectionAttributes() ).hasSize( 1 );

			final var nickNames = personEntity.getAttributes().getElementCollectionAttributes().get( 0 );
			assertThat( nickNames.getName() ).isEqualTo( "nickNames" );
			assertThat( nickNames.getSortNatural() )
					.as( "sort='natural' should become <sort-natural/>, not sort='natural'" )
					.isNotNull();
			assertThat( nickNames.getSort() )
					.as( "sort attribute should be null when sort-natural is used" )
					.isNull();
		} );
	}

	@Test
	@JiraKey( "HHH-20596" )
	public void testNonAggregatedCompositeIdKeyManyToOneTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/non-aggregate-key-many-to-one/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 2 );

			final JaxbEntityImpl detailEntity = transformed.getEntities().stream()
					.filter( e -> "Detail".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			assertThat( detailEntity.getAttributes().getManyToOneAttributes() )
					.as( "key-many-to-one should be transformed to a many-to-one with id=true" )
					.hasSize( 1 );

			final JaxbManyToOneImpl manyToOne = detailEntity.getAttributes().getManyToOneAttributes().get( 0 );
			assertThat( manyToOne.getName() ).isEqualTo( "master" );
			assertThat( manyToOne.isId() )
					.as( "many-to-one should have id=true for non-aggregated composite-id key-many-to-one" )
					.isTrue();
		} );
	}

	@Test
	@JiraKey( "HHH-20593" )
	public void testCompositePkPropertyRefOneToOneTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/composite-pk-property-ref/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 2 );

			final JaxbEntityImpl entityA = transformed.getEntities().stream()
					.filter( e -> "EntityA".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			assertThat( entityA.getAttributes().getOneToOneAttributes() ).hasSize( 1 );

			final JaxbOneToOneImpl oneToOne = entityA.getAttributes().getOneToOneAttributes().get( 0 );
			assertThat( oneToOne.getName() ).isEqualTo( "entityB" );
			assertThat( oneToOne.getMappedBy() )
					.as( "mapped-by should resolve to 'entityA' via property-ref on composite-PK entity" )
					.isEqualTo( "entityA" );
		} );
	}

	@Test
	@JiraKey( "HHH-20591" )
	public void testCollectionOptimisticLockTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/collection-optimistic-lock/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 3 );

			final JaxbEntityImpl ownerEntity = transformed.getEntities().stream()
					.filter( e -> "Owner".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			assertThat( ownerEntity.getAttributes().getOneToManyAttributes() ).hasSize( 2 );

			final JaxbOneToManyImpl lockedItems = ownerEntity.getAttributes().getOneToManyAttributes().stream()
					.filter( a -> "lockedItems".equals( a.getName() ) )
					.findFirst()
					.orElseThrow();
			assertThat( lockedItems.isOptimisticLock() )
					.as( "lockedItems should have optimistic-lock=true (default)" )
					.isTrue();

			final JaxbOneToManyImpl unlockedItems = ownerEntity.getAttributes().getOneToManyAttributes().stream()
					.filter( a -> "unlockedItems".equals( a.getName() ) )
					.findFirst()
					.orElseThrow();
			assertThat( unlockedItems.isOptimisticLock() )
					.as( "unlockedItems should have optimistic-lock=false" )
					.isFalse();
		} );
	}

	@Test
	@JiraKey( "HHH-20591" )
	public void testCompositeIdKeyManyToOneMappedByTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/composite-key-many-to-one/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 2 );

			final JaxbEntityImpl parentEntity = transformed.getEntities().stream()
					.filter( e -> "Parent".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			assertThat( parentEntity.getAttributes().getOneToManyAttributes() ).hasSize( 1 );

			final JaxbOneToManyImpl children = parentEntity.getAttributes().getOneToManyAttributes().get( 0 );
			assertThat( children.getName() ).isEqualTo( "children" );
			assertThat( children.getMappedBy() )
					.as( "mapped-by should resolve to 'id.parent' for key-many-to-one inside composite-id" )
					.isEqualTo( "id.parent" );
		} );
	}

	@Test
	@JiraKey( "HHH-19424" )
	public void testCompositeUserTypeComponentTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/composite-user-type/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 2 );

			assertThat( transformed.getCompositeUserTypeRegistrations() )
					.as( "CompositeUserType component should generate a <composite-user-type> registration" )
					.hasSize( 1 );

			final JaxbCompositeUserTypeRegistrationImpl registration =
					transformed.getCompositeUserTypeRegistrations().get( 0 );
			assertThat( registration.getClazz() )
					.isEqualTo( "org.hibernate.orm.test.cut.MonetoryAmount" );
			assertThat( registration.getDescriptor() )
					.isEqualTo( "org.hibernate.orm.test.cut.MonetoryAmountUserType" );

			assertThat( transformed.getEmbeddables() ).hasSize( 1 );

			final JaxbEmbeddableImpl embeddable = transformed.getEmbeddables().get( 0 );
			assertThat( embeddable.getClazz() )
					.isEqualTo( "org.hibernate.orm.test.cut.MonetoryAmount" );

			final JaxbBasicImpl amountAttr = embeddable.getAttributes().getBasicAttributes().stream()
					.filter( b -> "amount".equals( b.getName() ) )
					.findFirst()
					.orElseThrow();

			final JaxbEntityImpl mutualFundEntity = transformed.getEntities().stream()
					.filter( e -> "MutualFund".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();
			assertThat( mutualFundEntity.getAttributes().getEmbeddedAttributes() ).hasSize( 1 );
			assertThat( mutualFundEntity.getAttributes().getEmbeddedAttributes().get( 0 ).getName() )
					.isEqualTo( "holdings" );
		} );
	}

	@Test
	@JiraKey( "HHH-20627" )
	public void testSharedEmbeddableAttributeOverrideTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/composite-user-type/hbm.xml", scope, transformed -> {
			final JaxbEntityImpl mutualFundEntity = transformed.getEntities().stream()
					.filter( e -> "MutualFund".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			final JaxbEmbeddedImpl holdings = mutualFundEntity.getAttributes().getEmbeddedAttributes().get( 0 );
			assertThat( holdings.getName() ).isEqualTo( "holdings" );

			assertThat( holdings.getAttributeOverrides() )
					.as( "MutualFund.holdings should have an attribute-override for 'amount'" )
					.hasSize( 1 );

			final JaxbAttributeOverrideImpl override = holdings.getAttributeOverrides().get( 0 );
			assertThat( override.getName() ).isEqualTo( "amount" );
			assertThat( override.getColumn() ).isNotNull();
			assertThat( override.getColumn().getName() ).isEqualTo( "amount_millions" );
			assertThat( override.getColumn().getRead() ).isEqualTo( "amount_millions * 1000000.0" );
			assertThat( override.getColumn().getWrite() ).isEqualTo( "? / 1000000.0" );

			final JaxbEntityImpl transactionEntity = transformed.getEntities().stream()
					.filter( e -> "Transaction".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			final JaxbEmbeddedImpl value = transactionEntity.getAttributes().getEmbeddedAttributes().get( 0 );
			assertThat( value.getAttributeOverrides() )
					.as( "Transaction.value should not have attribute overrides (it matches the embeddable)" )
					.isEmpty();
		} );
	}

	@Test
	public void testComponentUpdateFalseTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/component-update-false/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEmbeddables() ).hasSize( 1 );

			final JaxbEmbeddableImpl embeddable = transformed.getEmbeddables().get( 0 );
			final JaxbBasicImpl nameAttr = embeddable.getAttributes().getBasicAttributes().stream()
					.filter( b -> "name".equals( b.getName() ) )
					.findFirst()
					.orElseThrow();
			assertThat( nameAttr.getColumn() )
					.as( "Property with update=\"false\" should generate a column element" )
					.isNotNull();
			assertThat( nameAttr.getColumn().isUpdatable() )
					.as( "Column should have updatable=false" )
					.isFalse();

			final JaxbBasicImpl descAttr = embeddable.getAttributes().getBasicAttributes().stream()
					.filter( b -> "description".equals( b.getName() ) )
					.findFirst()
					.orElseThrow();
			assertThat( descAttr.getColumn() )
					.as( "Property with insert=\"false\" should generate a column element" )
					.isNotNull();
			assertThat( descAttr.getColumn().isInsertable() )
					.as( "Column should have insertable=false" )
					.isFalse();
		} );
	}

	@Test
	public void testUnionSubclassNoInheritedTransients(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/union-subclass-transient/hbm.xml", scope, transformed -> {
			final JaxbEntityImpl employeeEntity = transformed.getEntities().stream()
					.filter( e -> "Employee".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			for ( JaxbTransientImpl transientAttr : employeeEntity.getAttributes().getTransients() ) {
				assertThat( transientAttr.getName() )
						.as( "Subclass should not have transient for inherited property '%s'", transientAttr.getName() )
						.isNotIn( "sex", "name", "id" );
			}

			final JaxbEntityImpl customerEntity = transformed.getEntities().stream()
					.filter( e -> "Customer".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			for ( JaxbTransientImpl transientAttr : customerEntity.getAttributes().getTransients() ) {
				assertThat( transientAttr.getName() )
						.as( "Subclass should not have transient for inherited property '%s'", transientAttr.getName() )
						.isNotIn( "sex", "name", "id" );
			}
		} );
	}

	@Test
	public void testPropertyIndexTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/property-index/hbm.xml", scope, transformed -> {
			final JaxbEntityImpl personEntity = transformed.getEntities().stream()
					.filter( e -> "Person".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			assertThat( personEntity.getTable() ).isNotNull();
			assertThat( personEntity.getTable().getIndexes() )
					.as( "Entity table should have indexes from property and many-to-one index attributes" )
					.hasSizeGreaterThanOrEqualTo( 2 );

			assertThat( personEntity.getTable().getIndexes() )
					.anySatisfy( index -> {
						assertThat( index.getName() ).isEqualTo( "person_name_index" );
						assertThat( index.getColumnList() ).isEqualTo( "name" );
					} );

			assertThat( personEntity.getTable().getIndexes() )
					.anySatisfy( index -> {
						assertThat( index.getName() ).isEqualTo( "person_persongroup_index" );
						assertThat( index.getColumnList() ).isEqualTo( "personGroup" );
					} );
		} );
	}

	@Test
	public void testImmutableTypeTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/immutable-type/hbm.xml", scope, (transformed) -> {
			assertThat( transformed.getEntities() ).hasSize( 1 );

			final JaxbEntityImpl entity = transformed.getEntities().get( 0 );
			final JaxbBasicImpl createdAttr = entity.getAttributes().getBasicAttributes().stream()
					.filter( b -> "created".equals( b.getName() ) )
					.findFirst()
					.orElseThrow();

			// imm_date type should generate <mutable>false</mutable>
			// to avoid false dirty-checks during merge
			assertThat( createdAttr.isMutable() )
					.as( "Property with imm_date type should have mutable=false" )
					.isFalse();
		} );
	}

	@Test
	public void testOneToOnePropertyRefTransformation(ServiceRegistryScope scope) {
		transformAndVerifyMultiple(
				new String[] { "xml/jaxb/mapping/one-to-one-property-ref/hbm.xml" },
				scope,
				(transformedRoots) -> {
					final JaxbEntityMappingsImpl transformed = transformedRoots.get( 0 );
					assertThat( transformed.getEntities() ).hasSize( 2 );

					final JaxbEntityImpl personEntity = transformed.getEntities().stream()
							.filter( e -> "Person".equals( e.getClazz() ) )
							.findFirst()
							.orElseThrow();

					// Person.address one-to-one with property-ref should become mapped-by
					assertThat( personEntity.getAttributes().getOneToOneAttributes() ).hasSize( 1 );
					final JaxbOneToOneImpl address = personEntity.getAttributes().getOneToOneAttributes().get( 0 );
					assertThat( address.getName() ).isEqualTo( "address" );
					assertThat( address.getMappedBy() )
							.as( "One-to-one with property-ref should generate mapped-by" )
							.isEqualTo( "resident" );
				}
		);
	}

	@Test
	public void testElementCollectionNotNullTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/element-not-null/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 1 );

			final JaxbEntityImpl entity = transformed.getEntities().get( 0 );
			assertThat( entity.getAttributes().getElementCollectionAttributes() ).hasSize( 1 );

			final var elementCollection = entity.getAttributes().getElementCollectionAttributes().get( 0 );
			assertThat( elementCollection.getName() ).isEqualTo( "persons" );
			assertThat( elementCollection.getColumn() )
					.as( "Element collection should have a column element" )
					.isNotNull();
			assertThat( elementCollection.getColumn().isNullable() )
					.as( "Element column with not-null='true' should have nullable=false" )
					.isFalse();
		} );
	}

	@Test
	public void testSharedEmbeddableFormulaPropertyTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/shared-embeddable-formula/hbm.xml", scope, transformed -> {
			final JaxbEntityImpl formulaUserEntity = transformed.getEntities().stream()
					.filter( e -> "FormulaUser".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			final JaxbEmbeddedImpl userPerson = formulaUserEntity.getAttributes().getEmbeddedAttributes().get( 0 );
			assertThat( userPerson.getName() ).isEqualTo( "person" );

			final JaxbEntityImpl formulaEmployeeEntity = transformed.getEntities().stream()
					.filter( e -> "FormulaEmployee".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			final JaxbEmbeddedImpl empPerson = formulaEmployeeEntity.getAttributes().getEmbeddedAttributes().get( 0 );
			assertThat( empPerson.getName() ).isEqualTo( "person" );

			assertThat( empPerson.getAttributeOverrides() )
					.as( "FormulaEmployee.person should have attribute override for 'heightInches'" )
					.anySatisfy( override ->
							assertThat( override.getName() ).isEqualTo( "heightInches" )
					);
		} );
	}

	private void transformAndVerify(
			String resourceName,
			ServiceRegistryScope scope,
			Consumer<JaxbEntityMappingsImpl> assertions) {
		scope.withService( ClassLoaderService.class, (cls) -> {
			try (final InputStream inputStream = cls.locateResourceStream( resourceName )) {
				withStaxEventReader( inputStream, cls, (staxEventReader) -> {
					final XMLEventReader reader = new HbmEventReader( staxEventReader, XMLEventFactory.newInstance() );
					try {
						final JAXBContext jaxbCtx = JAXBContext.newInstance( JaxbHbmHibernateMapping.class );
						final JaxbHbmHibernateMapping hbmMapping = JaxbHelper.VALIDATING.jaxb(
								reader,
								MappingXsdSupport.hbmXml.getSchema(),
								jaxbCtx
						);
						assertThat( hbmMapping ).isNotNull();

						final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources(
								scope.getRegistry()
						).addHbmXmlBinding( new Binding<>(
								hbmMapping,
								new Origin( SourceType.RESOURCE, resourceName )
						) ).buildMetadata();
						final List<Binding<JaxbEntityMappingsImpl>> transformedBindingList = HbmXmlTransformer.transform(
								singletonList( new Binding<>(
										hbmMapping,
										new Origin( SourceType.RESOURCE, resourceName )
								) ),
								metadata,
								UnsupportedFeatureHandling.ERROR
						);
						final JaxbEntityMappingsImpl transformed = transformedBindingList.get( 0 ).getRoot();
						assertThat( transformed ).isNotNull();

						assertions.accept( transformed );
					}
					catch (JAXBException e) {
						throw new RuntimeException( "Error during JAXB processing", e );
					}
				} );
			}
			catch (IOException e) {
				throw new RuntimeException( "Error accessing mapping file", e );
			}
		} );
	}

	@Test
	public void testCustomSqlCallableTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/custom-sql/hbm.xml", scope, (transformed) -> {
			assertThat( transformed.getEntities() ).hasSize( 1 );

			final JaxbEntityImpl entity = transformed.getEntities().get( 0 );

			assertThat( entity.getSqlInsert() )
					.as( "sql-insert should be present" )
					.isNotNull();
			assertThat( entity.getSqlInsert().isCallable() )
					.as( "sql-insert should have callable=true" )
					.isTrue();

			assertThat( entity.getSqlUpdate() )
					.as( "sql-update should be present" )
					.isNotNull();
			assertThat( entity.getSqlUpdate().isCallable() )
					.as( "sql-update should have callable=true" )
					.isTrue();

			assertThat( entity.getSqlDelete() )
					.as( "sql-delete should be present" )
					.isNotNull();
			assertThat( entity.getSqlDelete().isCallable() )
					.as( "sql-delete should have callable=true" )
					.isTrue();
		} );
	}

	@Test
	public void testPropertyUniqueTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/property-unique/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 1 );

			final JaxbEntityImpl entity = transformed.getEntities().get( 0 );
			final JaxbBasicImpl nameAttr = entity.getAttributes().getBasicAttributes().stream()
					.filter( b -> "name".equals( b.getName() ) )
					.findFirst()
					.orElseThrow();

			assertThat( nameAttr.getColumn() )
					.as( "Property with unique='true' should generate a column element" )
					.isNotNull();
			assertThat( nameAttr.getColumn().isUnique() )
					.as( "Column should have unique=true" )
					.isTrue();
		} );
	}

	private void transformAndVerifyMultiple(
			String[] resourceNames,
			ServiceRegistryScope scope,
			Consumer<List<JaxbEntityMappingsImpl>> assertions) {
		scope.withService( ClassLoaderService.class, (cls) -> {
			try {
				final JAXBContext jaxbCtx = JAXBContext.newInstance( JaxbHbmHibernateMapping.class );
				final List<Binding<JaxbHbmHibernateMapping>> hbmBindings = new ArrayList<>();

				for ( String resourceName : resourceNames ) {
					try (final InputStream inputStream = cls.locateResourceStream( resourceName )) {
						withStaxEventReader( inputStream, cls, (staxEventReader) -> {
							final XMLEventReader reader = new HbmEventReader( staxEventReader, XMLEventFactory.newInstance() );
							try {
								final JaxbHbmHibernateMapping hbmMapping = JaxbHelper.VALIDATING.jaxb(
										reader,
										MappingXsdSupport.hbmXml.getSchema(),
										jaxbCtx
								);
								hbmBindings.add( new Binding<>( hbmMapping, new Origin( SourceType.RESOURCE, resourceName ) ) );
							}
							catch (JAXBException e) {
								throw new RuntimeException( "Error during JAXB processing of " + resourceName, e );
							}
						} );
					}
				}

				final MetadataSources metadataSources = new MetadataSources( scope.getRegistry() );
				hbmBindings.forEach( metadataSources::addHbmXmlBinding );
				final MetadataImplementor metadata = (MetadataImplementor) metadataSources.buildMetadata();

				final List<Binding<JaxbEntityMappingsImpl>> transformedBindings = HbmXmlTransformer.transform(
						hbmBindings,
						metadata,
						UnsupportedFeatureHandling.ERROR
				);

				final List<JaxbEntityMappingsImpl> transformedRoots = transformedBindings.stream()
						.map( Binding::getRoot )
						.toList();
				assertions.accept( transformedRoots );
			}
			catch (JAXBException | IOException e) {
				throw new RuntimeException( "Error during transformation", e );
			}
		} );
	}

	@Test
	@JiraKey( "HHH-20638" )
	public void testFilterDefParameterTypeResolution(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/filter-def-types/hbm.xml", scope, transformed -> {
			assertThat( transformed.getFilterDefinitions() ).hasSize( 3 );

			final var stringFilter = transformed.getFilterDefinitions().stream()
					.filter( f -> "stringFilter".equals( f.getName() ) )
					.findFirst()
					.orElseThrow();
			assertThat( stringFilter.getFilterParams() ).hasSize( 1 );
			assertThat( stringFilter.getFilterParams().get( 0 ).getType() )
					.as( "HBM type 'string' should resolve to java.lang.String" )
					.isEqualTo( "java.lang.String" );

			final var timestampFilter = transformed.getFilterDefinitions().stream()
					.filter( f -> "timestampFilter".equals( f.getName() ) )
					.findFirst()
					.orElseThrow();
			assertThat( timestampFilter.getFilterParams() ).hasSize( 1 );
			assertThat( timestampFilter.getFilterParams().get( 0 ).getType() )
					.as( "HBM type 'timestamp' should resolve to java.util.Date" )
					.isEqualTo( "java.util.Date" );

			final var numericFilter = transformed.getFilterDefinitions().stream()
					.filter( f -> "numericFilter".equals( f.getName() ) )
					.findFirst()
					.orElseThrow();
			assertThat( numericFilter.getFilterParams() ).hasSize( 3 );

			final var doubleParam = numericFilter.getFilterParams().stream()
					.filter( p -> "amount".equals( p.getName() ) )
					.findFirst()
					.orElseThrow();
			assertThat( doubleParam.getType() )
					.as( "HBM type 'double' should resolve to double" )
					.isEqualTo( "double" );

			final var longParam = numericFilter.getFilterParams().stream()
					.filter( p -> "count".equals( p.getName() ) )
					.findFirst()
					.orElseThrow();
			assertThat( longParam.getType() )
					.as( "HBM type 'long' should resolve to long" )
					.isEqualTo( "long" );

			final var intParam = numericFilter.getFilterParams().stream()
					.filter( p -> "code".equals( p.getName() ) )
					.findFirst()
					.orElseThrow();
			assertThat( intParam.getType() )
					.as( "HBM type 'integer' should resolve to int" )
					.isEqualTo( "int" );
		} );
	}

	@Test
	@JiraKey( "HHH-20639" )
	public void testManyToManyElementFilterTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/many-to-many-element-filter/hbm.xml", scope, transformed -> {
			final JaxbEntityImpl productEntity = transformed.getEntities().stream()
					.filter( e -> "Product".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			assertThat( productEntity.getAttributes().getManyToManyAttributes() ).hasSize( 1 );

			final JaxbManyToManyImpl categories = productEntity.getAttributes().getManyToManyAttributes().get( 0 );
			assertThat( categories.getName() ).isEqualTo( "categories" );

			assertThat( categories.getFilters() )
					.as( "Filters from <many-to-many> element should be transferred" )
					.hasSizeGreaterThanOrEqualTo( 2 );

			assertThat( categories.getFilters() )
					.extracting( f -> f.getName() )
					.contains( "effectiveDate", "cat" );
		} );
	}

	@Test
	public void testConverterPropertyNoJavaTypeTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/converter/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 1 );

			final JaxbEntityImpl entity = transformed.getEntities().get( 0 );
			final JaxbBasicImpl balanceAttr = entity.getAttributes().getBasicAttributes().stream()
					.filter( b -> "balance".equals( b.getName() ) )
					.findFirst()
					.orElseThrow();

			assertThat( balanceAttr.getConvert() )
					.as( "converted:: property should generate a <convert> element" )
					.isNotNull();
			assertThat( balanceAttr.getConvert().getConverter() )
					.isEqualTo( "org.hibernate.orm.test.boot.jaxb.mapping.MoneyConverter" );

			assertThat( balanceAttr.getJavaType() )
					.as( "converted:: property should not have a java-type — the converter provides type information" )
					.isNull();
			assertThat( balanceAttr.getJdbcType() )
					.as( "converted:: property should not have a jdbc-type" )
					.isNull();
			assertThat( balanceAttr.getJdbcTypeCode() )
					.as( "converted:: property should not have a jdbc-type-code" )
					.isNull();
		} );
	}

	@Test
	@JiraKey( "HHH-20640" )
	public void testInverseManyToManyMappedByTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/many-to-many-inverse/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 2 );

			final JaxbEntityImpl productEntity = transformed.getEntities().stream()
					.filter( e -> "Product".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			assertThat( productEntity.getAttributes().getManyToManyAttributes() ).hasSize( 1 );
			final JaxbManyToManyImpl categories = productEntity.getAttributes().getManyToManyAttributes().get( 0 );
			assertThat( categories.getName() ).isEqualTo( "categories" );
			assertThat( categories.getMappedBy() )
					.as( "Owning side should not have mapped-by" )
					.isNull();
			assertThat( categories.getJoinTable() )
					.as( "Owning side should have a join-table" )
					.isNotNull();
			assertThat( categories.getJoinTable().getName() ).isEqualTo( "PROD_CAT" );

			final JaxbEntityImpl categoryEntity = transformed.getEntities().stream()
					.filter( e -> "Category".equals( e.getClazz() ) )
					.findFirst()
					.orElseThrow();

			assertThat( categoryEntity.getAttributes().getManyToManyAttributes() ).hasSize( 1 );
			final JaxbManyToManyImpl products = categoryEntity.getAttributes().getManyToManyAttributes().get( 0 );
			assertThat( products.getName() ).isEqualTo( "products" );
			assertThat( products.getMappedBy() )
					.as( "Inverse side should have mapped-by pointing to the owning side" )
					.isEqualTo( "categories" );
			assertThat( products.getJoinTable() )
					.as( "Inverse side should not have a join-table" )
					.isNull();
		} );
	}

	@Test
	@JiraKey( "HHH-20682" )
	public void testNaturalIdNullablePropertyTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/natural-id-nullable/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 1 );

			final JaxbEntityImpl userEntity = transformed.getEntities().get( 0 );
			assertThat( userEntity.getAttributes().getNaturalId() ).isNotNull();
			assertThat( userEntity.getAttributes().getNaturalId().isMutable() ).isTrue();

			final var naturalIdBasics = userEntity.getAttributes().getNaturalId().getBasicAttributes();
			assertThat( naturalIdBasics ).hasSize( 3 );

			for ( JaxbBasicImpl basic : naturalIdBasics ) {
				assertThat( basic.getColumn() )
						.as( "Natural-id property '%s' with not-null='false' should generate a column element",
								basic.getName() )
						.isNotNull();
				assertThat( basic.getColumn().isNullable() )
						.as( "Natural-id property '%s' should have nullable=true", basic.getName() )
						.isTrue();
			}
		} );
	}

	@Test
	@JiraKey( "HHH-20683" )
	public void testDynamicEntityIdNameTransformation(ServiceRegistryScope scope) {
		transformAndVerify( "xml/jaxb/mapping/dynamic-entity/hbm.xml", scope, transformed -> {
			assertThat( transformed.getEntities() ).hasSize( 2 );

			final JaxbEntityImpl baseEntity = transformed.getEntities().stream()
					.filter( e -> "Base".equals( e.getName() ) )
					.findFirst()
					.orElseThrow();

			assertThat( baseEntity.getAttributes().getIdAttributes() ).hasSize( 1 );
			final JaxbIdImpl id = baseEntity.getAttributes().getIdAttributes().get( 0 );
			assertThat( id.getName() )
					.as( "Dynamic entity <id/> without name should get a default name from the boot model" )
					.isNotNull();
		} );
	}
}
