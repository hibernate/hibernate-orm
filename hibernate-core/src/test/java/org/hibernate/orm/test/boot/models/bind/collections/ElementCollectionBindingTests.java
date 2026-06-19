/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.collections;

import java.io.Serializable;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Date;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.SortedSet;

import org.hibernate.annotations.Array;
import org.hibernate.annotations.Bag;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionIdJavaClass;
import org.hibernate.annotations.CollectionIdJavaType;
import org.hibernate.annotations.CollectionIdJdbcType;
import org.hibernate.annotations.CollectionIdJdbcTypeCode;
import org.hibernate.annotations.CollectionIdMutability;
import org.hibernate.annotations.CollectionIdType;
import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.ListIndexBase;
import org.hibernate.annotations.ListIndexJavaType;
import org.hibernate.annotations.ListIndexJdbcType;
import org.hibernate.annotations.ListIndexJdbcTypeCode;
import org.hibernate.annotations.MapKeyCompositeType;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.SQLOrder;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.SortNatural;

import org.hibernate.HibernateException;
import org.hibernate.SharedSessionContract;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.boot.mapping.internal.model.BasicValueIntent;
import org.hibernate.boot.mapping.internal.model.BootBindingModel;
import org.hibernate.boot.mapping.internal.model.CollectionValueIntent;
import org.hibernate.boot.mapping.internal.model.EmbeddedValueIntent;
import org.hibernate.boot.mapping.internal.model.ManagedTypeBinding;
import org.hibernate.boot.mapping.internal.view.AttributeBindingView;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.CustomCollectionType;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.java.LongJavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserCollectionType;
import org.hibernate.usertype.UserType;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Fetch;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.MapKeyJoinColumns;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
public class ElementCollectionBindingTests {
	@Test
	@ServiceRegistry
	void testBasicSetElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( SetOwner.class.getName() );
					final Property property = entityBinding.getProperty( "labels" );
					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.Set.class );
					final Collection collection = (Collection) property.getValue();
					final BasicValue element = (BasicValue) collection.getElement();
					final CollectionValueIntent collectionIntent = collectionIntent(
							context.getBindingState().getBootBindingModel(),
							SetOwner.class,
							"labels"
					);

					assertThat( collectionIntent.elementIntent() ).isInstanceOf( BasicValueIntent.class );
					assertThat( collectionIntent.indexIntent() ).isNull();
					assertThat( collectionIntent.collectionIdIntent() ).isNull();
					assertThat( collection.getRole() ).isEqualTo( SetOwner.class.getName() + ".labels" );
					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "set_owner_labels" );
					assertThat( collection.getCollectionTable().getOptions() ).isEqualTo( "collection table options" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label" );
					assertThat( collection.getCollectionTable().getForeignKeyCollection() ).hasSize( 1 );
					final org.hibernate.mapping.ForeignKey foreignKey = collection.getCollectionTable()
							.getForeignKeyCollection()
							.iterator()
							.next();
					assertThat( foreignKey.getName() ).isEqualTo( "fk_set_owner_labels_owner" );
					final org.hibernate.mapping.UniqueKey uniqueKey = collection.getCollectionTable()
							.getUniqueKey( "uk_set_owner_labels_owner_label" );
					assertThat( uniqueKey ).isNotNull();
					assertThat( uniqueKey.getOptions() ).isEqualTo( "unique options" );
					assertThat( uniqueKey.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id", "label" );
					final org.hibernate.mapping.Index index = collection.getCollectionTable()
							.getIndex( "idx_set_owner_labels_label" );
					assertThat( index ).isNotNull();
					assertThat( index.getOptions() ).isEqualTo( "index options" );
					assertThat( index.getSelectables() )
							.extracting( selectable -> ( (org.hibernate.mapping.Column) selectable ).getName() )
							.containsExactly( "label" );
					assertThat( context.getMetadataCollector().getCollectionBinding( collection.getRole() ) ).isSameAs( collection );
				},
				scope.getRegistry(),
				SetOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testJpaFetchOverridesElementCollectionFetchTypeAndBatchSize(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( JpaFetchElementCollectionOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "labels" ).getValue();

					assertThat( collection.isLazy() ).isFalse();
					assertThat( collection.getFetchStyle() ).isEqualTo( FetchStyle.JOIN );
					assertThat( collection.getBatchSize() ).isEqualTo( 11 );
				},
				scope.getRegistry(),
				JpaFetchElementCollectionOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testImplicitCollectionTableName(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ImplicitCollectionTableOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "labels" ).getValue();

					assertThat( collection.getCollectionTable().getName() )
							.isEqualTo( "implicitcollectiontableowner_labels" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id" );
				},
				scope.getRegistry(),
				ImplicitCollectionTableOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testBasicListElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ListOwner.class.getName() );
					final Property property = entityBinding.getProperty( "labels" );
					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.List.class );
					final org.hibernate.mapping.List collection = (org.hibernate.mapping.List) property.getValue();
					final BasicValue element = (BasicValue) collection.getElement();
					final BasicValue index = (BasicValue) collection.getIndex();
					final CollectionValueIntent collectionIntent = collectionIntent(
							context.getBindingState().getBootBindingModel(),
							ListOwner.class,
							"labels"
					);

					assertThat( collectionIntent.elementIntent() ).isInstanceOf( BasicValueIntent.class );
					assertThat( collectionIntent.indexIntent() ).isInstanceOf( BasicValueIntent.class );
					assertThat( collection.getRole() ).isEqualTo( ListOwner.class.getName() + ".labels" );
					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "list_owner_labels" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( index.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "position" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label" );
				},
				scope.getRegistry(),
				ListOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testListIndexBaseElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ListIndexBaseOwner.class.getName() );
					final org.hibernate.mapping.List collection = (org.hibernate.mapping.List) entityBinding
							.getProperty( "labels" )
							.getValue();

					assertThat( collection.getBaseIndex() ).isEqualTo( 5 );
				},
				scope.getRegistry(),
				ListIndexBaseOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testListIndexJavaTypeElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ListIndexJavaTypeOwner.class.getName() );
					final org.hibernate.mapping.List collection = (org.hibernate.mapping.List) entityBinding
							.getProperty( "labels" )
							.getValue();
					final BasicValue index = (BasicValue) collection.getIndex();

					assertThat( index.resolve().getDomainJavaType().getJavaType() ).isEqualTo( Long.class );
				},
				scope.getRegistry(),
				ListIndexJavaTypeOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testListIndexJdbcTypeElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ListIndexJdbcTypeOwner.class.getName() );
					final org.hibernate.mapping.List collection = (org.hibernate.mapping.List) entityBinding
							.getProperty( "labels" )
							.getValue();
					final BasicValue index = (BasicValue) collection.getIndex();

					assertThat( index.resolve().getJdbcType().getJdbcTypeCode() ).isEqualTo( Types.INTEGER );
				},
				scope.getRegistry(),
				ListIndexJdbcTypeOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testListIndexJdbcTypeCodeElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ListIndexJdbcTypeCodeOwner.class.getName() );
					final org.hibernate.mapping.List collection = (org.hibernate.mapping.List) entityBinding
							.getProperty( "labels" )
							.getValue();
					final BasicValue index = (BasicValue) collection.getIndex();

					assertThat( index.resolve().getJdbcType().getJdbcTypeCode() ).isEqualTo( Types.SMALLINT );
				},
				scope.getRegistry(),
				ListIndexJdbcTypeCodeOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testImplicitOrderColumnListElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ImplicitListOwner.class.getName() );
					final Property property = entityBinding.getProperty( "labels" );
					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.List.class );
					final org.hibernate.mapping.List collection = (org.hibernate.mapping.List) property.getValue();
					final BasicValue index = (BasicValue) collection.getIndex();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "implicit_list_owner_labels" );
					assertThat( index.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "idx" );
				},
				scope.getRegistry(),
				ImplicitListOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testBagListElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( BagListOwner.class.getName() );
					final Property property = entityBinding.getProperty( "labels" );

					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.Bag.class );
				},
				scope.getRegistry(),
				BagListOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testCollectionTypeElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CustomCollectionTypeOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "labels" ).getValue();
					final CustomCollectionType collectionType = (CustomCollectionType) collection.getCollectionType();

					assertThat( collection.getTypeName() ).isEqualTo( LocalCollectionType.class.getName() );
					assertThat( collectionType.getUserType() ).isInstanceOf( LocalCollectionType.class );
					assertThat( ( (LocalCollectionType) collectionType.getUserType() ).strategy ).isEqualTo( "local" );
				},
				scope.getRegistry(),
				CustomCollectionTypeOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testIdBagElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( IdBagOwner.class.getName() );
					final Property property = entityBinding.getProperty( "labels" );

					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.IdentifierBag.class );
					final org.hibernate.mapping.IdentifierBag collection = (org.hibernate.mapping.IdentifierBag) property.getValue();
					final BasicValue identifier = (BasicValue) collection.getIdentifier();
					final BasicValue element = (BasicValue) collection.getElement();
					final CollectionValueIntent collectionIntent = collectionIntent(
							context.getBindingState().getBootBindingModel(),
							IdBagOwner.class,
							"labels"
					);

					assertThat( collectionIntent.elementIntent() ).isInstanceOf( BasicValueIntent.class );
					assertThat( collectionIntent.collectionIdIntent() ).isInstanceOf( BasicValueIntent.class );
					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "idbag_owner_labels" );
					assertThat( identifier.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label_row_id" );
					assertThat( identifier.getColumns().get( 0 ).isNullable() ).isFalse();
					assertThat( identifier.resolve().getDomainJavaType().getJavaType() ).isEqualTo( Integer.class );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label" );
				},
				scope.getRegistry(),
				IdBagOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testIdBagElementCollectionWithGeneratorImplementation(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( IdBagGeneratorImplementationOwner.class.getName() );
					final org.hibernate.mapping.IdentifierBag collection = (org.hibernate.mapping.IdentifierBag) entityBinding
							.getProperty( "labels" )
							.getValue();
					final BasicValue identifier = (BasicValue) collection.getIdentifier();

					assertThat( identifier.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label_row_id" );
					assertThat( identifier.getCustomIdGeneratorCreator() ).isNotNull();
				},
				scope.getRegistry(),
				IdBagGeneratorImplementationOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testIdBagElementCollectionWithLocalSequenceGenerator(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( IdBagLocalSequenceGeneratorOwner.class.getName() );
					final org.hibernate.mapping.IdentifierBag collection = (org.hibernate.mapping.IdentifierBag) entityBinding
							.getProperty( "labels" )
							.getValue();
					final BasicValue identifier = (BasicValue) collection.getIdentifier();

					assertThat( identifier.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label_row_id" );
					assertThat( identifier.getCustomIdGeneratorCreator() ).isNotNull();
				},
				scope.getRegistry(),
				IdBagLocalSequenceGeneratorOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testIdBagElementCollectionWithCollectionIdJavaType(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( IdBagCollectionIdJavaTypeOwner.class.getName() );
					final org.hibernate.mapping.IdentifierBag collection = (org.hibernate.mapping.IdentifierBag) entityBinding
							.getProperty( "labels" )
							.getValue();
					final BasicValue identifier = (BasicValue) collection.getIdentifier();

					assertThat( identifier.resolve().getDomainJavaType().getJavaType() ).isEqualTo( Long.class );
				},
				scope.getRegistry(),
				IdBagCollectionIdJavaTypeOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testIdBagElementCollectionWithCollectionIdJdbcType(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( IdBagCollectionIdJdbcTypeOwner.class.getName() );
					final org.hibernate.mapping.IdentifierBag collection = (org.hibernate.mapping.IdentifierBag) entityBinding
							.getProperty( "labels" )
							.getValue();
					final BasicValue identifier = (BasicValue) collection.getIdentifier();

					assertThat( identifier.resolve().getJdbcType().getJdbcTypeCode() ).isEqualTo( Types.INTEGER );
				},
				scope.getRegistry(),
				IdBagCollectionIdJdbcTypeOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testIdBagElementCollectionWithCollectionIdJdbcTypeCode(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( IdBagCollectionIdJdbcTypeCodeOwner.class.getName() );
					final org.hibernate.mapping.IdentifierBag collection = (org.hibernate.mapping.IdentifierBag) entityBinding
							.getProperty( "labels" )
							.getValue();
					final BasicValue identifier = (BasicValue) collection.getIdentifier();

					assertThat( identifier.resolve().getJdbcType().getJdbcTypeCode() ).isEqualTo( Types.SMALLINT );
				},
				scope.getRegistry(),
				IdBagCollectionIdJdbcTypeCodeOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testIdBagElementCollectionWithCollectionIdMutability(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( IdBagCollectionIdMutabilityOwner.class.getName() );
					final org.hibernate.mapping.IdentifierBag collection = (org.hibernate.mapping.IdentifierBag) entityBinding
							.getProperty( "labels" )
							.getValue();
					final BasicValue identifier = (BasicValue) collection.getIdentifier();

					assertThat( identifier.resolve().getMutabilityPlan() )
							.isInstanceOf( MutableIntegerMutabilityPlan.class );
					assertThat( identifier.resolve().getMutabilityPlan().isMutable() ).isTrue();
				},
				scope.getRegistry(),
				IdBagCollectionIdMutabilityOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testIdBagElementCollectionWithCollectionIdType(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( IdBagCollectionIdTypeOwner.class.getName() );
					final org.hibernate.mapping.IdentifierBag collection = (org.hibernate.mapping.IdentifierBag) entityBinding
							.getProperty( "labels" )
							.getValue();
					final BasicValue identifier = (BasicValue) collection.getIdentifier();
					final CustomType<?> customType = (CustomType<?>) identifier.resolve().getLegacyResolvedBasicType();

					assertThat( customType.getUserType() ).isInstanceOf( IntegerCollectionIdUserType.class );
					assertThat( ( (IntegerCollectionIdUserType) customType.getUserType() ).strategy )
							.isEqualTo( "collection-id" );
					assertThat( identifier.resolve().getDomainJavaType().getJavaType() ).isEqualTo( Integer.class );
				},
				scope.getRegistry(),
				IdBagCollectionIdTypeOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testArrayElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ArrayOwner.class.getName() );
					final Property property = entityBinding.getProperty( "labels" );

					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.Array.class );
					final org.hibernate.mapping.Array collection = (org.hibernate.mapping.Array) property.getValue();
					final BasicValue index = (BasicValue) collection.getIndex();

					assertThat( collection.getElementClassName() ).isEqualTo( String.class.getName() );
					assertThat( index.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "position" );
				},
				scope.getRegistry(),
				ArrayOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testSqlArrayLength(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( SqlArrayOwner.class.getName() );
					final BasicValue tags = (BasicValue) entityBinding.getProperty( "tags" ).getValue();
					final org.hibernate.mapping.Column column = (org.hibernate.mapping.Column) tags.getColumn();

					assertThat( column.getArrayLength() ).isEqualTo( 7 );
				},
				scope.getRegistry(),
				SqlArrayOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testSqlOrderedSetElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( SqlOrderedSetOwner.class.getName() );
					final Property property = entityBinding.getProperty( "labels" );

					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.Set.class );
					final Collection collection = (Collection) property.getValue();
					assertThat( collection.getOrderBy() ).isEqualTo( "label desc" );
					assertThat( collection.isSorted() ).isFalse();
				},
				scope.getRegistry(),
				SqlOrderedSetOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testJpaOrderedEmbeddableElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( JpaOrderedEmbeddableOwner.class.getName() );
					final Property property = entityBinding.getProperty( "labels" );

					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.Set.class );
					final Collection collection = (Collection) property.getValue();
					assertThat( collection.getOrderBy() ).isEqualTo( "label desc" );
					assertThat( collection.isSorted() ).isFalse();
				},
				scope.getRegistry(),
				JpaOrderedEmbeddableOwner.class,
				OrderedLabel.class
		);
	}

	@Test
	@ServiceRegistry
	void testNaturalSortedSetElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( NaturalSortedSetOwner.class.getName() );
					final Property property = entityBinding.getProperty( "labels" );

					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.Set.class );
					final Collection collection = (Collection) property.getValue();
					assertThat( collection.isSorted() ).isTrue();
					assertThat( collection.getComparatorClassName() ).isNull();
				},
				scope.getRegistry(),
				NaturalSortedSetOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testComparatorSortedMapElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ComparatorSortedMapOwner.class.getName() );
					final Property property = entityBinding.getProperty( "labels" );

					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.Map.class );
					final Collection collection = (Collection) property.getValue();
					assertThat( collection.isSorted() ).isTrue();
					assertThat( collection.getComparatorClassName() ).isEqualTo( ReverseStringComparator.class.getName() );
				},
				scope.getRegistry(),
				ComparatorSortedMapOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testBasicMapElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( MapOwner.class.getName() );
					final Property property = entityBinding.getProperty( "labels" );
					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.Map.class );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) property.getValue();
					final BasicValue key = (BasicValue) collection.getIndex();
					final BasicValue element = (BasicValue) collection.getElement();
					final CollectionValueIntent collectionIntent = collectionIntent(
							context.getBindingState().getBootBindingModel(),
							MapOwner.class,
							"labels"
					);

					assertThat( collectionIntent.elementIntent() ).isInstanceOf( BasicValueIntent.class );
					assertThat( collectionIntent.indexIntent() ).isInstanceOf( BasicValueIntent.class );
					assertThat( collection.getRole() ).isEqualTo( MapOwner.class.getName() + ".labels" );
					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "map_owner_labels" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( key.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label_key" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label" );
				},
				scope.getRegistry(),
				MapOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testImplicitMapKeyColumnElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ImplicitMapOwner.class.getName() );
					final Property property = entityBinding.getProperty( "labels" );
					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.Map.class );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) property.getValue();
					final BasicValue key = (BasicValue) collection.getIndex();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "implicit_map_owner_labels" );
					assertThat( key.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id" );
				},
				scope.getRegistry(),
				ImplicitMapOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testEntityMapKeyElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( EntityMapKeyOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "labels" )
							.getValue();
					final ManyToOne key = (ManyToOne) collection.getIndex();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "entity_map_key_owner_labels" );
					assertThat( key.getReferencedEntityName() ).isEqualTo( LabelKey.class.getName() );
					assertThat( key.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label_key_id" );
				},
				scope.getRegistry(),
				EntityMapKeyOwner.class,
				LabelKey.class
		);
	}

	@Test
	@ServiceRegistry
	void testEntityMapKeyElementCollectionNonPrimaryKeyReference(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( EntityMapKeyNonPkOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "labels" )
							.getValue();
					final ManyToOne key = (ManyToOne) collection.getIndex();

					assertThat( key.isReferenceToPrimaryKey() ).isFalse();
					assertThat( key.getReferencedPropertyName() ).isEqualTo( "code" );
					assertThat( key.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label_key_code" );
					assertThat( collection.getCollectionTable().getForeignKeyCollection() ).hasSize( 2 );
				},
				scope.getRegistry(),
				EntityMapKeyNonPkOwner.class,
				LabelKeyNonPk.class
		);
	}

	@Test
	@ServiceRegistry
	void testCompositeEntityMapKeyElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CompositeEntityMapKeyOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "labels" )
							.getValue();
					final ManyToOne key = (ManyToOne) collection.getIndex();

					assertThat( key.getReferencedEntityName() ).isEqualTo( CompositeLabelKey.class.getName() );
					assertThat( key.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label_key_fk1", "label_key_fk2" );
				},
				scope.getRegistry(),
				CompositeEntityMapKeyOwner.class,
				CompositeLabelKey.class
		);
	}

	@Test
	@ServiceRegistry
	void testPropertyMapKeyEmbeddableElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( PropertyMapKeyEmbeddableOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "addresses" )
							.getValue();

					assertThat( collection.hasMapKeyProperty() ).isTrue();
					assertThat( collection.getMapKeyPropertyName() ).isEqualTo( "zipCode" );
					assertThat( collection.getIndex().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "zipCode" );
					assertThat( collection.getElement() ).isInstanceOf( Component.class );
				},
				scope.getRegistry(),
				PropertyMapKeyEmbeddableOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testEnumMapKeyElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( EnumMapKeyOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "labels" )
							.getValue();
					final BasicValue key = (BasicValue) collection.getIndex();

					assertThat( key.getEnumerationStyle() ).isEqualTo( EnumType.STRING );
					assertThat( key.resolve().getDomainJavaType().getJavaType() ).isEqualTo( LabelKind.class );
				},
				scope.getRegistry(),
				EnumMapKeyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testMapKeyClassElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( MapKeyClassOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "labels" )
							.getValue();
					final BasicValue key = (BasicValue) collection.getIndex();

					assertThat( key.getEnumerationStyle() ).isEqualTo( EnumType.STRING );
					assertThat( key.resolve().getDomainJavaType().getJavaType() ).isEqualTo( LabelKind.class );
				},
				scope.getRegistry(),
				MapKeyClassOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testMapKeyCompositeTypeElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CompositeTypeMapKeyOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "labels" )
							.getValue();
					final Component key = (Component) collection.getIndex();

					assertThat( key.getTypeName() ).isEqualTo( CompositeLabelKeyUserType.class.getName() );
					assertThat( key.getComponentClassName() ).isEqualTo( CompositeLabelKeyEmbeddable.class.getName() );
					assertThat( key.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "key_code", "key_region" );
				},
				scope.getRegistry(),
				CompositeTypeMapKeyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testTemporalMapKeyElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( TemporalMapKeyOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "labels" )
							.getValue();
					final BasicValue key = (BasicValue) collection.getIndex();

					assertThat( key.getTemporalPrecision() ).isEqualTo( TemporalType.DATE );
				},
				scope.getRegistry(),
				TemporalMapKeyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testConvertedMapKeyElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ConvertedMapKeyOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "labels" )
							.getValue();
					final BasicValue key = (BasicValue) collection.getIndex();

					assertThat( key.getJpaAttributeConverterDescriptor() ).isNotNull();
				},
				scope.getRegistry(),
				ConvertedMapKeyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testConvertedBasicElementCollectionValue(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ConvertedElementOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "labels" ).getValue();
					final BasicValue element = (BasicValue) collection.getElement();

					assertThat( element.getJpaAttributeConverterDescriptor() ).isNotNull();
				},
				scope.getRegistry(),
				ConvertedElementOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testCompositeOwnerElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CompositeSetOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "labels" ).getValue();
					final BasicValue element = (BasicValue) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "composite_set_owner_labels" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id1", "owner_id2" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label" );
				},
				scope.getRegistry(),
				CompositeSetOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testEmbeddableElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( EmbeddableElementOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "addresses" ).getValue();
					final Component element = (Component) collection.getElement();
					final CollectionValueIntent collectionIntent = collectionIntent(
							context.getBindingState().getBootBindingModel(),
							EmbeddableElementOwner.class,
							"addresses"
					);

					assertThat( collectionIntent.elementIntent() ).isInstanceOf( EmbeddedValueIntent.class );
					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_addresses" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( element.getComponentClassName() ).isEqualTo( Address.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "line1", "zipCode" );
				},
				scope.getRegistry(),
				EmbeddableElementOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testEmbeddableListElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( EmbeddableListOwner.class.getName() );
					final Property property = entityBinding.getProperty( "addresses" );
					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.List.class );
					final org.hibernate.mapping.List collection = (org.hibernate.mapping.List) property.getValue();
					final Component element = (Component) collection.getElement();
					final BasicValue index = (BasicValue) collection.getIndex();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "list_owner_addresses" );
					assertThat( index.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "address_position" );
					assertThat( element.getComponentClassName() ).isEqualTo( Address.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "line1", "zipCode" );
				},
				scope.getRegistry(),
				EmbeddableListOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testEmbeddableMapElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( EmbeddableMapOwner.class.getName() );
					final Property property = entityBinding.getProperty( "addresses" );
					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.Map.class );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) property.getValue();
					final BasicValue key = (BasicValue) collection.getIndex();
					final Component element = (Component) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "map_owner_addresses" );
					assertThat( key.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "address_key" );
					assertThat( element.getComponentClassName() ).isEqualTo( Address.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "line1", "zipCode" );
				},
				scope.getRegistry(),
				EmbeddableMapOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testEmbeddableMapValueElementCollectionAttributeOverride(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( OverrideEmbeddableMapValueOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "addresses" )
							.getValue();
					final Component element = (Component) collection.getElement();

					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "value_street", "value_postal_code" );
				},
				scope.getRegistry(),
				OverrideEmbeddableMapValueOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testEmbeddableElementCollectionAttributeOverride(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( OverrideEmbeddableElementOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "addresses" ).getValue();
					final Component element = (Component) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "override_owner_addresses" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "street", "postal_code" );
				},
				scope.getRegistry(),
				OverrideEmbeddableElementOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testEmbeddedElementCollectionIntent(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( EmbeddedIntentElementOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "addresses" ).getValue();
					final Component element = (Component) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "embedded_intent_owner_addresses" );
					assertThat( element.getComponentClassName() ).isEqualTo( Address.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "line1", "zipCode" );
				},
				scope.getRegistry(),
				EmbeddedIntentElementOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testNestedEmbeddableElementCollectionAttributeOverride(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( NestedEmbeddableElementOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "addresses" ).getValue();
					final Component element = (Component) collection.getElement();
					final Component location = (Component) element.getProperty( "location" ).getValue();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "nested_owner_addresses" );
					assertThat( location.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "home_city", "home_country" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "line1", "home_city", "home_country", "zipCode" );
				},
				scope.getRegistry(),
				NestedEmbeddableElementOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testNestedEmbeddableElementCollectionConvert(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( NestedConvertedEmbeddableElementOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "addresses" ).getValue();
					final Component element = (Component) collection.getElement();
					final Component location = (Component) element.getProperty( "location" ).getValue();
					final BasicValue city = (BasicValue) location.getProperty( "city" ).getValue();
					final BasicValue country = (BasicValue) location.getProperty( "country" ).getValue();

					assertThat( city.getJpaAttributeConverterDescriptor() ).isNotNull();
					assertThat( country.getJpaAttributeConverterDescriptor() ).isNotNull();
				},
				scope.getRegistry(),
				NestedConvertedEmbeddableElementOwner.class
		);
	}

	@Entity(name="SetOwner")
	@Table(name="set_owners")
	public static class SetOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "set_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				foreignKey = @ForeignKey(name = "fk_set_owner_labels_owner"),
				uniqueConstraints = @UniqueConstraint(
						name = "uk_set_owner_labels_owner_label",
						columnNames = { "owner_id", "label" },
						options = "unique options"
				),
				indexes = @Index(
						name = "idx_set_owner_labels_label",
						columnList = "label",
						options = "index options"
				),
				options = "collection table options"
		)
		@Column(name = "label")
		private Set<String> labels;
	}

	@Entity(name="JpaFetchElementCollectionOwner")
	@Table(name="jpa_fetch_element_collection_owners")
	public static class JpaFetchElementCollectionOwner {
		@Id
		private Integer id;
		@ElementCollection(fetch = FetchType.LAZY)
		@CollectionTable(name = "jpa_fetch_element_collection_labels")
		@Fetch(type = FetchType.EAGER, batchSize = 11)
		private Set<String> labels;
	}

	@Entity(name="CompositeSetOwner")
	@Table(name="composite_set_owners")
	public static class CompositeSetOwner {
		@EmbeddedId
		private Pk id;
		@ElementCollection
		@CollectionTable(
				name = "composite_set_owner_labels",
				joinColumns = {
						@JoinColumn(name = "owner_id2", referencedColumnName = "id2"),
						@JoinColumn(name = "owner_id1", referencedColumnName = "id1")
				}
		)
		@Column(name = "label")
		private Set<String> labels;
	}

	@Entity(name="ImplicitCollectionTableOwner")
	public static class ImplicitCollectionTableOwner {
		@Id
		private Integer id;
		@ElementCollection
		@Column(name = "label")
		private Set<String> labels;
	}

	@Entity(name="ListOwner")
	@Table(name="list_owners")
	public static class ListOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "list_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@OrderColumn(name = "position")
		@Column(name = "label")
		private List<String> labels;
	}

	@Entity(name="ListIndexBaseOwner")
	@Table(name="list_index_base_owners")
	public static class ListIndexBaseOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "list_index_base_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@OrderColumn(name = "position")
		@ListIndexBase(5)
		@Column(name = "label")
		private List<String> labels;
	}

	@Entity(name="ListIndexJavaTypeOwner")
	@Table(name="list_index_java_type_owners")
	public static class ListIndexJavaTypeOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "list_index_java_type_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@OrderColumn(name = "position")
		@ListIndexJavaType(LongJavaType.class)
		@Column(name = "label")
		private List<String> labels;
	}

	@Entity(name="ListIndexJdbcTypeOwner")
	@Table(name="list_index_jdbc_type_owners")
	public static class ListIndexJdbcTypeOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "list_index_jdbc_type_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@OrderColumn(name = "position")
		@ListIndexJdbcType(IntegerJdbcType.class)
		@Column(name = "label")
		private List<String> labels;
	}

	@Entity(name="ListIndexJdbcTypeCodeOwner")
	@Table(name="list_index_jdbc_type_code_owners")
	public static class ListIndexJdbcTypeCodeOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "list_index_jdbc_type_code_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@OrderColumn(name = "position")
		@ListIndexJdbcTypeCode(Types.SMALLINT)
		@Column(name = "label")
		private List<String> labels;
	}

	@Entity(name="ImplicitListOwner")
	@Table(name="implicit_list_owners")
	public static class ImplicitListOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "implicit_list_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@Column(name = "label")
		private List<String> labels;
	}

	@Entity(name="BagListOwner")
	@Table(name="bag_list_owners")
	public static class BagListOwner {
		@Id
		private Integer id;
		@ElementCollection
		@Bag
		@CollectionTable(
				name = "bag_list_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@Column(name = "label")
		private List<String> labels;
	}

	@Entity(name="CustomCollectionTypeOwner")
	@Table(name="custom_collection_type_owners")
	public static class CustomCollectionTypeOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionType(
				type = LocalCollectionType.class,
				parameters = @Parameter(name = "strategy", value = "local")
		)
		@CollectionTable(
				name = "custom_collection_type_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@Column(name = "label")
		private java.util.Collection<String> labels;
	}

	public static class LocalCollectionType implements UserCollectionType, ParameterizedType {
		private String strategy;

		@Override
		public void setParameterValues(Properties parameters) {
			strategy = parameters.getProperty( "strategy" );
		}

		@Override
		public CollectionClassification getClassification() {
			return CollectionClassification.BAG;
		}

		@Override
		public Class<?> getCollectionClass() {
			return java.util.ArrayList.class;
		}

		@Override
		public PersistentCollection<?> instantiate(
				SharedSessionContractImplementor session,
				CollectionPersister persister) throws HibernateException {
			return null;
		}

		@Override
		public PersistentCollection<?> wrap(
				SharedSessionContractImplementor session,
				Object collection) {
			return null;
		}

		@Override
		public java.util.Iterator<?> getElementsIterator(Object collection) {
			return java.util.Collections.emptyIterator();
		}

		@Override
		public boolean contains(Object collection, Object entity) {
			return false;
		}

		@Override
		public Object indexOf(Object collection, Object entity) {
			return null;
		}

		@Override
		public Object replaceElements(
				Object original,
				Object target,
				CollectionPersister persister,
				Object owner,
				java.util.Map copyCache,
				SharedSessionContractImplementor session) throws HibernateException {
			return target;
		}

		@Override
		public Object instantiate(int anticipatedSize) {
			return new java.util.ArrayList<>();
		}
	}

	@Entity(name="IdBagOwner")
	@Table(name="idbag_owners")
	public static class IdBagOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "idbag_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@CollectionId(generator = "increment", column = @Column(name = "label_row_id"))
		@CollectionIdJavaClass(idType = Integer.class)
		@Column(name = "label")
		private java.util.Collection<String> labels;
	}

	@Entity(name="IdBagGeneratorImplementationOwner")
	@Table(name="idbag_generator_impl_owners")
	public static class IdBagGeneratorImplementationOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "idbag_generator_impl_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@CollectionId(generatorImplementation = IncrementGenerator.class, column = @Column(name = "label_row_id"))
		@CollectionIdJavaClass(idType = Integer.class)
		@Column(name = "label")
		private java.util.Collection<String> labels;
	}

	@Entity(name="IdBagLocalSequenceGeneratorOwner")
	@Table(name="idbag_local_sequence_generator_owners")
	public static class IdBagLocalSequenceGeneratorOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "idbag_local_sequence_generator_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@SequenceGenerator(name = "local_label_sequence", sequenceName = "local_label_sequence")
		@CollectionId(generator = "local_label_sequence", column = @Column(name = "label_row_id"))
		@CollectionIdJavaClass(idType = Integer.class)
		@Column(name = "label")
		private java.util.Collection<String> labels;
	}

	@Entity(name="IdBagCollectionIdJavaTypeOwner")
	@Table(name="idbag_collection_id_java_type_owners")
	public static class IdBagCollectionIdJavaTypeOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "idbag_collection_id_java_type_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@CollectionId(generator = "increment", column = @Column(name = "label_row_id"))
		@CollectionIdJavaType(LongJavaType.class)
		@Column(name = "label")
		private java.util.Collection<String> labels;
	}

	@Entity(name="IdBagCollectionIdJdbcTypeOwner")
	@Table(name="idbag_collection_id_jdbc_type_owners")
	public static class IdBagCollectionIdJdbcTypeOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "idbag_collection_id_jdbc_type_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@CollectionId(generator = "increment", column = @Column(name = "label_row_id"))
		@CollectionIdJavaClass(idType = Integer.class)
		@CollectionIdJdbcType(IntegerJdbcType.class)
		@Column(name = "label")
		private java.util.Collection<String> labels;
	}

	@Entity(name="IdBagCollectionIdJdbcTypeCodeOwner")
	@Table(name="idbag_collection_id_jdbc_type_code_owners")
	public static class IdBagCollectionIdJdbcTypeCodeOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "idbag_collection_id_jdbc_type_code_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@CollectionId(generator = "increment", column = @Column(name = "label_row_id"))
		@CollectionIdJavaClass(idType = Integer.class)
		@CollectionIdJdbcTypeCode(Types.SMALLINT)
		@Column(name = "label")
		private java.util.Collection<String> labels;
	}

	@Entity(name="IdBagCollectionIdMutabilityOwner")
	@Table(name="idbag_collection_id_mutability_owners")
	public static class IdBagCollectionIdMutabilityOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "idbag_collection_id_mutability_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@CollectionId(generator = "increment", column = @Column(name = "label_row_id"))
		@CollectionIdJavaClass(idType = Integer.class)
		@CollectionIdMutability(MutableIntegerMutabilityPlan.class)
		@Column(name = "label")
		private java.util.Collection<String> labels;
	}

	@Entity(name="IdBagCollectionIdTypeOwner")
	@Table(name="idbag_collection_id_type_owners")
	public static class IdBagCollectionIdTypeOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "idbag_collection_id_type_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@CollectionId(generator = "increment", column = @Column(name = "label_row_id"))
		@CollectionIdType(
				value = IntegerCollectionIdUserType.class,
				parameters = @Parameter(name = "strategy", value = "collection-id")
		)
		@Column(name = "label")
		private java.util.Collection<String> labels;
	}

	public static class MutableIntegerMutabilityPlan implements MutabilityPlan<Integer> {
		@Override
		public boolean isMutable() {
			return true;
		}

		@Override
		public Integer deepCopy(Integer value) {
			return value;
		}

		@Override
		public Serializable disassemble(Integer value, SharedSessionContract session) {
			return value;
		}

		@Override
		public Integer assemble(Serializable cached, SharedSessionContract session) {
			return (Integer) cached;
		}
	}

	public static class IntegerCollectionIdUserType implements UserType<Integer>, ParameterizedType {
		private String strategy;

		@Override
		public void setParameterValues(Properties parameters) {
			strategy = parameters.getProperty( "strategy" );
		}

		@Override
		public int getSqlType() {
			return Types.INTEGER;
		}

		@Override
		public Class<Integer> returnedClass() {
			return Integer.class;
		}

		@Override
		public Integer deepCopy(Integer value) {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}
	}

	@Entity(name="ArrayOwner")
	@Table(name="array_owners")
	public static class ArrayOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "array_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@OrderColumn(name = "position")
		@Column(name = "label")
		private String[] labels;
	}

	@Entity(name="SqlArrayOwner")
	@Table(name="sql_array_owners")
	public static class SqlArrayOwner {
		@Id
		private Integer id;
		@Array(length = 7)
		@Column(name = "tags")
		private String[] tags;
	}

	@Entity(name="SqlOrderedSetOwner")
	@Table(name="sql_ordered_set_owners")
	public static class SqlOrderedSetOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "sql_ordered_set_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@Column(name = "label")
		@SQLOrder("label desc")
		private Set<String> labels;
	}

	@Entity(name="JpaOrderedEmbeddableOwner")
	@Table(name="jpa_ordered_embeddable_owners")
	public static class JpaOrderedEmbeddableOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "jpa_ordered_embeddable_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@OrderBy("label desc")
		private Set<OrderedLabel> labels;
	}

	@Embeddable
	public static class OrderedLabel {
		@Column(name = "label")
		private String label;
	}

	@Entity(name="NaturalSortedSetOwner")
	@Table(name="natural_sorted_set_owners")
	public static class NaturalSortedSetOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "natural_sorted_set_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@Column(name = "label")
		@SortNatural
		private SortedSet<String> labels;
	}

	@Entity(name="ComparatorSortedMapOwner")
	@Table(name="comparator_sorted_map_owners")
	public static class ComparatorSortedMapOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "comparator_sorted_map_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@MapKeyColumn(name = "label_key")
		@Column(name = "label")
		@SortComparator(ReverseStringComparator.class)
		private SortedMap<String, String> labels;
	}

	public static class ReverseStringComparator implements Comparator<String> {
		@Override
		public int compare(String first, String second) {
			return second.compareTo( first );
		}
	}

	@Entity(name="MapOwner")
	@Table(name="map_owners")
	public static class MapOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "map_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@MapKeyColumn(name = "label_key")
		@Column(name = "label")
		private Map<String, String> labels;
	}

	@Entity(name="ImplicitMapOwner")
	@Table(name="implicit_map_owners")
	public static class ImplicitMapOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "implicit_map_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@Column(name = "label")
		private Map<String, String> labels;
	}

	@Entity(name="EntityMapKeyOwner")
	@Table(name="entity_map_key_owners")
	public static class EntityMapKeyOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "entity_map_key_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@MapKeyJoinColumn(name = "label_key_id", referencedColumnName = "id")
		@Column(name = "label")
		private Map<LabelKey, String> labels;
	}

	@Entity(name="LabelKey")
	@Table(name="label_keys")
	public static class LabelKey {
		@Id
		private Integer id;
	}

	@Entity(name="EntityMapKeyNonPkOwner")
	@Table(name="entity_map_key_non_pk_owners")
	public static class EntityMapKeyNonPkOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "entity_map_key_non_pk_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@MapKeyJoinColumn(name = "label_key_code", referencedColumnName = "code")
		@Column(name = "label")
		private Map<LabelKeyNonPk, String> labels;
	}

	@Entity(name="LabelKeyNonPk")
	@Table(name="label_key_non_pks")
	public static class LabelKeyNonPk {
		@Id
		private Integer id;
		private String code;
	}

	@Entity(name="CompositeEntityMapKeyOwner")
	@Table(name="composite_entity_map_key_owners")
	public static class CompositeEntityMapKeyOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "composite_entity_map_key_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@MapKeyJoinColumns({
				@MapKeyJoinColumn(name = "label_key_fk2", referencedColumnName = "id2"),
				@MapKeyJoinColumn(name = "label_key_fk1", referencedColumnName = "id1")
		})
		@Column(name = "label")
		private Map<CompositeLabelKey, String> labels;
	}

	@Entity(name="CompositeLabelKey")
	@Table(name="composite_label_keys")
	public static class CompositeLabelKey {
		@EmbeddedId
		private Pk id;
	}

	@Entity(name="PropertyMapKeyEmbeddableOwner")
	@Table(name="property_map_key_embeddable_owners")
	public static class PropertyMapKeyEmbeddableOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "property_map_key_embeddable_owner_addresses",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@MapKey(name = "zipCode")
		private Map<String, Address> addresses;
	}

	@Entity(name="EnumMapKeyOwner")
	@Table(name="enum_map_key_owners")
	public static class EnumMapKeyOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "enum_map_key_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@MapKeyColumn(name = "label_kind")
		@MapKeyEnumerated(EnumType.STRING)
		@Column(name = "label")
		private Map<LabelKind, String> labels;
	}

	@Entity(name="MapKeyClassOwner")
	@Table(name="map_key_class_owners")
	public static class MapKeyClassOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "map_key_class_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@MapKeyColumn(name = "label_kind")
		@MapKeyClass(LabelKind.class)
		@MapKeyEnumerated(EnumType.STRING)
		@Column(name = "label")
		private Map<Object, String> labels;
	}

	@Entity(name="CompositeTypeMapKeyOwner")
	@Table(name="composite_type_map_key_owners")
	public static class CompositeTypeMapKeyOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "composite_type_map_key_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@MapKeyCompositeType(CompositeLabelKeyUserType.class)
		@AttributeOverride(name = "key.code", column = @Column(name = "key_code"))
		@AttributeOverride(name = "key.region", column = @Column(name = "key_region"))
		@Column(name = "label")
		private Map<CompositeLabelKeyDomain, String> labels;
	}

	public record CompositeLabelKeyDomain(String code, String region) implements Serializable {
	}

	@Embeddable
	public static class CompositeLabelKeyEmbeddable {
		private String code;
		private String region;
	}

	public static class CompositeLabelKeyUserType implements CompositeUserType<CompositeLabelKeyDomain> {
		@Override
		public Object getPropertyValue(CompositeLabelKeyDomain component, int property) throws HibernateException {
			return property == 0 ? component.code() : component.region();
		}

		@Override
		public CompositeLabelKeyDomain instantiate(ValueAccess values) {
			return new CompositeLabelKeyDomain(
					values.getValue( 0, String.class ),
					values.getValue( 1, String.class )
			);
		}

		@Override
		public Class<?> embeddable() {
			return CompositeLabelKeyEmbeddable.class;
		}

		@Override
		public Class<CompositeLabelKeyDomain> returnedClass() {
			return CompositeLabelKeyDomain.class;
		}

		@Override
		public boolean equals(CompositeLabelKeyDomain x, CompositeLabelKeyDomain y) {
			return java.util.Objects.equals( x, y );
		}

		@Override
		public int hashCode(CompositeLabelKeyDomain x) {
			return java.util.Objects.hashCode( x );
		}

		@Override
		public CompositeLabelKeyDomain deepCopy(CompositeLabelKeyDomain value) {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Serializable disassemble(CompositeLabelKeyDomain value) {
			return value;
		}

		@Override
		public CompositeLabelKeyDomain assemble(Serializable cached, Object owner) {
			return (CompositeLabelKeyDomain) cached;
		}

		@Override
		public CompositeLabelKeyDomain replace(
				CompositeLabelKeyDomain detached,
				CompositeLabelKeyDomain managed,
				Object owner) {
			return detached;
		}
	}

	@Entity(name="OverrideEmbeddableMapValueOwner")
	@Table(name="override_embeddable_map_value_owners")
	public static class OverrideEmbeddableMapValueOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "override_map_value_owner_addresses",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@MapKeyColumn(name = "address_key")
		@AttributeOverride(name = "value.line1", column = @Column(name = "value_street"))
		@AttributeOverride(name = "value.zipCode", column = @Column(name = "value_postal_code"))
		private Map<String, Address> addresses;
	}

	@Entity(name="TemporalMapKeyOwner")
	@Table(name="temporal_map_key_owners")
	public static class TemporalMapKeyOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "temporal_map_key_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@MapKeyColumn(name = "label_date")
		@MapKeyTemporal(TemporalType.DATE)
		@Column(name = "label")
		private Map<Date, String> labels;
	}

	@Entity(name="ConvertedMapKeyOwner")
	@Table(name="converted_map_key_owners")
	public static class ConvertedMapKeyOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "converted_map_key_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@MapKeyColumn(name = "label_code")
		@Convert(attributeName = "key", converter = LabelCodeConverter.class)
		@Column(name = "label")
		private Map<LabelCode, String> labels;
	}

	@Entity(name="ConvertedElementOwner")
	@Table(name="converted_element_owners")
	public static class ConvertedElementOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "converted_element_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@Convert(attributeName = "value", converter = LabelCodeConverter.class)
		@Column(name = "label")
		private Set<LabelCode> labels;
	}

	@Entity(name="EmbeddableElementOwner")
	@Table(name="embeddable_element_owners")
	public static class EmbeddableElementOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "owner_addresses",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		private Set<Address> addresses;
	}

	@Entity(name="EmbeddableListOwner")
	@Table(name="embeddable_list_owners")
	public static class EmbeddableListOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "list_owner_addresses",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@OrderColumn(name = "address_position")
		private List<Address> addresses;
	}

	@Entity(name="EmbeddableMapOwner")
	@Table(name="embeddable_map_owners")
	public static class EmbeddableMapOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "map_owner_addresses",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@MapKeyColumn(name = "address_key")
		private Map<String, Address> addresses;
	}

	@Entity(name="OverrideEmbeddableElementOwner")
	@Table(name="override_embeddable_element_owners")
	public static class OverrideEmbeddableElementOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "override_owner_addresses",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@AttributeOverride(name = "line1", column = @Column(name = "street"))
		@AttributeOverride(name = "zipCode", column = @Column(name = "postal_code"))
		private Set<Address> addresses;
	}

	@Entity(name="EmbeddedIntentElementOwner")
	@Table(name="embedded_intent_element_owners")
	public static class EmbeddedIntentElementOwner {
		@Id
		private Integer id;
		@ElementCollection
		@Embedded
		@CollectionTable(
				name = "embedded_intent_owner_addresses",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		private Set<Address> addresses;
	}

	@Entity(name="NestedEmbeddableElementOwner")
	@Table(name="nested_embeddable_element_owners")
	public static class NestedEmbeddableElementOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "nested_owner_addresses",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@AttributeOverride(name = "location.city", column = @Column(name = "home_city"))
		@AttributeOverride(name = "location.country", column = @Column(name = "home_country"))
		private Set<AddressWithLocation> addresses;
	}

	@Entity(name="NestedConvertedEmbeddableElementOwner")
	@Table(name="nested_converted_embeddable_element_owners")
	public static class NestedConvertedEmbeddableElementOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "nested_converted_owner_addresses",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@Convert(attributeName = "location.city", converter = CityConverter.class)
		private Set<AddressWithConvertedLocation> addresses;
	}

	@Embeddable
	public static class Pk {
		private Integer id1;
		private Integer id2;
	}

	@Embeddable
	public static class Address {
		private String line1;
		private String zipCode;
	}

	@Embeddable
	public static class AddressWithLocation {
		private String line1;
		private String zipCode;
		private Location location;
	}

	@Embeddable
	public static class Location {
		private String city;
		private String country;
	}

	@Embeddable
	public static class AddressWithConvertedLocation {
		private String line1;
		private String zipCode;
		private ConvertedLocation location;
	}

	@Embeddable
	public static class ConvertedLocation {
		private String city;
		@Convert(converter = CountryConverter.class)
		private String country;
	}

	public enum LabelKind {
		PRIMARY,
		SECONDARY
	}

	private static CollectionValueIntent collectionIntent(
			BootBindingModel bootBindingModel,
			Class<?> javaType,
			String attributeName) {
		final AttributeBindingView attributeBindingView = attribute(
				managedTypeBinding( bootBindingModel, javaType ),
				attributeName
		);
		assertThat( attributeBindingView.valueIntent() ).isSameAs( attributeBindingView.collectionValueIntent() );
		assertThat( attributeBindingView.collectionValueIntent() ).isNotNull();
		return attributeBindingView.collectionValueIntent();
	}

	private static ManagedTypeBinding managedTypeBinding(BootBindingModel bootBindingModel, Class<?> javaType) {
		for ( ManagedTypeBinding managedTypeBinding : bootBindingModel.managedTypeBindings() ) {
			if ( managedTypeBinding.classDetails().toJavaClass().equals( javaType ) ) {
				return managedTypeBinding;
			}
		}
		throw new AssertionError( "Could not locate managed type binding " + javaType.getName() );
	}

	private static AttributeBindingView attribute(ManagedTypeBinding managedTypeBinding, String name) {
		for ( var attributeUsage : managedTypeBinding.attributeUsages() ) {
			if ( attributeUsage.attributeName().equals( name ) ) {
				return new AttributeBindingView( attributeUsage );
			}
		}
		throw new AssertionError( "Could not locate attribute binding " + name );
	}

	public record LabelCode(String code) {
	}

	public static class LabelCodeConverter implements AttributeConverter<LabelCode, String> {
		@Override
		public String convertToDatabaseColumn(LabelCode attribute) {
			return attribute == null ? null : attribute.code();
		}

		@Override
		public LabelCode convertToEntityAttribute(String dbData) {
			return dbData == null ? null : new LabelCode( dbData );
		}
	}

	public static class CityConverter implements AttributeConverter<String, String> {
		@Override
		public String convertToDatabaseColumn(String attribute) {
			return attribute;
		}

		@Override
		public String convertToEntityAttribute(String dbData) {
			return dbData;
		}
	}

	public static class CountryConverter implements AttributeConverter<String, String> {
		@Override
		public String convertToDatabaseColumn(String attribute) {
			return attribute;
		}

		@Override
		public String convertToEntityAttribute(String dbData) {
			return dbData;
		}
	}
}
