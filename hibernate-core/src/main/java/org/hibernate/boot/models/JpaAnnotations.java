/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models;

import java.lang.annotation.Annotation;
import java.util.function.Consumer;

import org.hibernate.boot.models.annotations.internal.AccessJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.AssociationOverrideJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.AssociationOverridesJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.AttributeOverrideJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.AttributeOverridesJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.BasicJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.CacheableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.CheckConstraintJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.CollectionTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ConstructorResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ConvertJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ConverterJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ConvertsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.DiscriminatorColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.DiscriminatorValueJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ElementCollectionJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EmbeddableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EmbeddedIdJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EmbeddedJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityListenersJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EnumeratedJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EnumeratedValueJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ExcludeDefaultListenersJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ExcludeSuperclassListenersJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.FieldResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ForeignKeyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.GeneratedValueJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.IdClassJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.IdJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.IndexJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.InheritanceJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.LobJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToManyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToOneJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyClassJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyEnumeratedJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyJoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyJoinColumnsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyTemporalJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.MappedSuperclassJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.MapsIdJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedAttributeNodeJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedEntityGraphJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedEntityGraphsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedNativeQueriesJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedNativeQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedQueriesJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedStoredProcedureQueriesJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedStoredProcedureQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedSubgraphJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OneToManyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OneToOneJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OrderByJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OrderColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PostLoadJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PostPersistJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PostRemoveJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PostUpdateJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PrePersistJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PreRemoveJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PreUpdateJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PrimaryKeyJoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PrimaryKeyJoinColumnsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.QueryHintJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SecondaryTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SecondaryTablesJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SequenceGeneratorJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SequenceGeneratorsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SqlResultSetMappingJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SqlResultSetMappingsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.StoredProcedureParameterJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TableGeneratorJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TableGeneratorsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TemporalJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TransientJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.UniqueConstraintJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.VersionJpaAnnotation;
import org.hibernate.boot.models.internal.OrmAnnotationHelper;
import org.hibernate.models.internal.OrmAnnotationDescriptor;
import org.hibernate.models.spi.AnnotationDescriptor;

import jakarta.persistence.Access;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Converts;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EntityResult;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumeratedValue;
import jakarta.persistence.ExcludeDefaultListeners;
import jakarta.persistence.ExcludeSuperclassListeners;
import jakarta.persistence.FieldResult;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.MapKeyJoinColumns;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.MapsId;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.SequenceGenerators;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.TableGenerators;
import jakarta.persistence.Temporal;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

/**
 * Descriptors for JPA annotations
 *
 * @author Steve Ebersole
 */
public interface JpaAnnotations {
	OrmAnnotationDescriptor<Access,AccessJpaAnnotation> ACCESS = new OrmAnnotationDescriptor<>(
			Access.class,
			AccessJpaAnnotation.class
	);

	OrmAnnotationDescriptor<AssociationOverrides,AssociationOverridesJpaAnnotation> ASSOCIATION_OVERRIDES = new OrmAnnotationDescriptor<>(
			AssociationOverrides.class,
			AssociationOverridesJpaAnnotation.class
	);
	OrmAnnotationDescriptor<AssociationOverride,AssociationOverrideJpaAnnotation> ASSOCIATION_OVERRIDE = new OrmAnnotationDescriptor<>(
			AssociationOverride.class,
			AssociationOverrideJpaAnnotation.class,
			ASSOCIATION_OVERRIDES
	);
	OrmAnnotationDescriptor<AttributeOverrides,AttributeOverridesJpaAnnotation> ATTRIBUTE_OVERRIDES = new OrmAnnotationDescriptor<>(
			AttributeOverrides.class,
			AttributeOverridesJpaAnnotation.class
	);
	OrmAnnotationDescriptor<AttributeOverride,AttributeOverrideJpaAnnotation> ATTRIBUTE_OVERRIDE = new OrmAnnotationDescriptor<>(
			AttributeOverride.class,
			AttributeOverrideJpaAnnotation.class,
			ATTRIBUTE_OVERRIDES
	);
	OrmAnnotationDescriptor<Basic,BasicJpaAnnotation> BASIC = new OrmAnnotationDescriptor<>(
			Basic.class,
			BasicJpaAnnotation.class
	);
	OrmAnnotationDescriptor<Cacheable,CacheableJpaAnnotation> CACHEABLE = new OrmAnnotationDescriptor<>(
			Cacheable.class,
			CacheableJpaAnnotation.class
	);
	OrmAnnotationDescriptor<CheckConstraint,CheckConstraintJpaAnnotation> CHECK_CONSTRAINT = new OrmAnnotationDescriptor<>(
			CheckConstraint.class,
			CheckConstraintJpaAnnotation.class
	);
	OrmAnnotationDescriptor<CollectionTable,CollectionTableJpaAnnotation> COLLECTION_TABLE = new OrmAnnotationDescriptor<>(
			CollectionTable.class,
			CollectionTableJpaAnnotation.class
	);
	OrmAnnotationDescriptor<Column,ColumnJpaAnnotation> COLUMN = new OrmAnnotationDescriptor<>(
			Column.class,
			ColumnJpaAnnotation.class
	);
	OrmAnnotationDescriptor<ColumnResult,ColumnResultJpaAnnotation> COLUMN_RESULT = new OrmAnnotationDescriptor<>(
			ColumnResult.class,
			ColumnResultJpaAnnotation.class
	);
	OrmAnnotationDescriptor<ConstructorResult,ConstructorResultJpaAnnotation> CONSTRUCTOR_RESULT = new OrmAnnotationDescriptor<>(
			ConstructorResult.class,
			ConstructorResultJpaAnnotation.class
	);
	OrmAnnotationDescriptor<Converts,ConvertsJpaAnnotation> CONVERTS = new OrmAnnotationDescriptor<>(
			Converts.class,
			ConvertsJpaAnnotation.class
	);
	OrmAnnotationDescriptor<Convert,ConvertJpaAnnotation> CONVERT = new OrmAnnotationDescriptor<>(
			Convert.class,
			ConvertJpaAnnotation.class,
			CONVERTS
	);
	OrmAnnotationDescriptor<Converter,ConverterJpaAnnotation> CONVERTER = new OrmAnnotationDescriptor<>(
			Converter.class,
			ConverterJpaAnnotation.class
	);
	OrmAnnotationDescriptor<DiscriminatorColumn,DiscriminatorColumnJpaAnnotation> DISCRIMINATOR_COLUMN = new OrmAnnotationDescriptor<>(
			DiscriminatorColumn.class,
			DiscriminatorColumnJpaAnnotation.class
	);
	OrmAnnotationDescriptor<DiscriminatorValue,DiscriminatorValueJpaAnnotation> DISCRIMINATOR_VALUE = new OrmAnnotationDescriptor<>(
			DiscriminatorValue.class,
			DiscriminatorValueJpaAnnotation.class
	);
	OrmAnnotationDescriptor<ElementCollection,ElementCollectionJpaAnnotation> ELEMENT_COLLECTION = new OrmAnnotationDescriptor<>(
			ElementCollection.class,
			ElementCollectionJpaAnnotation.class
	);
	OrmAnnotationDescriptor<Embeddable,EmbeddableJpaAnnotation> EMBEDDABLE = new OrmAnnotationDescriptor<>(
			Embeddable.class,
			EmbeddableJpaAnnotation.class
	);
	OrmAnnotationDescriptor<Embedded,EmbeddedJpaAnnotation> EMBEDDED = new OrmAnnotationDescriptor<>(
			Embedded.class,
			EmbeddedJpaAnnotation.class
	);
	OrmAnnotationDescriptor<EmbeddedId,EmbeddedIdJpaAnnotation> EMBEDDED_ID = new OrmAnnotationDescriptor<>(
			EmbeddedId.class,
			EmbeddedIdJpaAnnotation.class
	);
	OrmAnnotationDescriptor<Entity,EntityJpaAnnotation> ENTITY = new OrmAnnotationDescriptor<>(
			Entity.class,
			EntityJpaAnnotation.class
	);
	OrmAnnotationDescriptor<EntityListeners,EntityListenersJpaAnnotation> ENTITY_LISTENERS = new OrmAnnotationDescriptor<>(
			EntityListeners.class,
			EntityListenersJpaAnnotation.class
	);
	OrmAnnotationDescriptor<EntityResult,EntityResultJpaAnnotation> ENTITY_RESULT = new OrmAnnotationDescriptor<>(
			EntityResult.class,
			EntityResultJpaAnnotation.class
	);
	OrmAnnotationDescriptor<Enumerated,EnumeratedJpaAnnotation> ENUMERATED = new OrmAnnotationDescriptor<>(
			Enumerated.class,
			EnumeratedJpaAnnotation.class
	);
	OrmAnnotationDescriptor<EnumeratedValue, EnumeratedValueJpaAnnotation> ENUMERATED_VALUE = new OrmAnnotationDescriptor<>(
			EnumeratedValue.class,
			EnumeratedValueJpaAnnotation.class
	);
	OrmAnnotationDescriptor<ExcludeDefaultListeners,ExcludeDefaultListenersJpaAnnotation> EXCLUDE_DEFAULT_LISTENERS = new OrmAnnotationDescriptor<>(
			ExcludeDefaultListeners.class,
			ExcludeDefaultListenersJpaAnnotation.class
	);
	OrmAnnotationDescriptor<ExcludeSuperclassListeners,ExcludeSuperclassListenersJpaAnnotation> EXCLUDE_SUPERCLASS_LISTENERS = new OrmAnnotationDescriptor<>(
			ExcludeSuperclassListeners.class,
			ExcludeSuperclassListenersJpaAnnotation.class
	);
	OrmAnnotationDescriptor<FieldResult,FieldResultJpaAnnotation> FIELD_RESULT = new OrmAnnotationDescriptor<>(
			FieldResult.class,
			FieldResultJpaAnnotation.class
	);
	OrmAnnotationDescriptor<ForeignKey,ForeignKeyJpaAnnotation> FOREIGN_KEY = new OrmAnnotationDescriptor<>(
			ForeignKey.class,
			ForeignKeyJpaAnnotation.class
	);
	OrmAnnotationDescriptor<GeneratedValue,GeneratedValueJpaAnnotation> GENERATED_VALUE = new OrmAnnotationDescriptor<>(
			GeneratedValue.class,
			GeneratedValueJpaAnnotation.class
	);
	OrmAnnotationDescriptor<Id,IdJpaAnnotation> ID = new OrmAnnotationDescriptor<>(
			Id.class,
			IdJpaAnnotation.class
	);
	OrmAnnotationDescriptor<IdClass,IdClassJpaAnnotation> ID_CLASS = new OrmAnnotationDescriptor<>(
			IdClass.class,
			IdClassJpaAnnotation.class
	);
	OrmAnnotationDescriptor<Index,IndexJpaAnnotation> INDEX = new OrmAnnotationDescriptor<>(
			Index.class,
			IndexJpaAnnotation.class
	);
	OrmAnnotationDescriptor<Inheritance,InheritanceJpaAnnotation> INHERITANCE = new OrmAnnotationDescriptor<>(
			Inheritance.class,
			InheritanceJpaAnnotation.class
	);
	OrmAnnotationDescriptor<JoinColumns,JoinColumnsJpaAnnotation> JOIN_COLUMNS = new OrmAnnotationDescriptor<>(
			JoinColumns.class,
			JoinColumnsJpaAnnotation.class
	);
	OrmAnnotationDescriptor<JoinColumn,JoinColumnJpaAnnotation> JOIN_COLUMN = new OrmAnnotationDescriptor<>(
			JoinColumn.class,
			JoinColumnJpaAnnotation.class,
			JOIN_COLUMNS
	);
	OrmAnnotationDescriptor<JoinTable,JoinTableJpaAnnotation> JOIN_TABLE = new OrmAnnotationDescriptor<>(
			JoinTable.class,
			JoinTableJpaAnnotation.class
	);
	OrmAnnotationDescriptor<Lob,LobJpaAnnotation> LOB = new OrmAnnotationDescriptor<>(
			Lob.class,
			LobJpaAnnotation.class
	);
	OrmAnnotationDescriptor<ManyToMany,ManyToManyJpaAnnotation> MANY_TO_MANY = new OrmAnnotationDescriptor<>(
			ManyToMany.class,
			ManyToManyJpaAnnotation.class
	);
	OrmAnnotationDescriptor<ManyToOne,ManyToOneJpaAnnotation> MANY_TO_ONE = new OrmAnnotationDescriptor<>(
			ManyToOne.class,
			ManyToOneJpaAnnotation.class
	);
	OrmAnnotationDescriptor<MapKey,MapKeyJpaAnnotation> MAP_KEY = new OrmAnnotationDescriptor<>(
			MapKey.class,
			MapKeyJpaAnnotation.class
	);
	OrmAnnotationDescriptor<MapKeyClass,MapKeyClassJpaAnnotation> MAP_KEY_CLASS = new OrmAnnotationDescriptor<>(
			MapKeyClass.class,
			MapKeyClassJpaAnnotation.class
	);
	OrmAnnotationDescriptor<MapKeyColumn,MapKeyColumnJpaAnnotation> MAP_KEY_COLUMN = new OrmAnnotationDescriptor<>(
			MapKeyColumn.class,
			MapKeyColumnJpaAnnotation.class
	);
	OrmAnnotationDescriptor<MapKeyEnumerated,MapKeyEnumeratedJpaAnnotation> MAP_KEY_ENUMERATED = new OrmAnnotationDescriptor<>(
			MapKeyEnumerated.class,
			MapKeyEnumeratedJpaAnnotation.class
	);
	OrmAnnotationDescriptor<MapKeyJoinColumns,MapKeyJoinColumnsJpaAnnotation> MAP_KEY_JOIN_COLUMNS = new OrmAnnotationDescriptor<>(
			MapKeyJoinColumns.class,
			MapKeyJoinColumnsJpaAnnotation.class
	);
	OrmAnnotationDescriptor<MapKeyJoinColumn, MapKeyJoinColumnJpaAnnotation> MAP_KEY_JOIN_COLUMN = new OrmAnnotationDescriptor<>(
			MapKeyJoinColumn.class,
			MapKeyJoinColumnJpaAnnotation.class,
			MAP_KEY_JOIN_COLUMNS
	);
	OrmAnnotationDescriptor<MapKeyTemporal,MapKeyTemporalJpaAnnotation> MAP_KEY_TEMPORAL = new OrmAnnotationDescriptor<>(
			MapKeyTemporal.class,
			MapKeyTemporalJpaAnnotation.class
	);
	OrmAnnotationDescriptor<MappedSuperclass,MappedSuperclassJpaAnnotation> MAPPED_SUPERCLASS = new OrmAnnotationDescriptor<>(
			MappedSuperclass.class,
			MappedSuperclassJpaAnnotation.class
	);
	OrmAnnotationDescriptor<MapsId,MapsIdJpaAnnotation> MAPS_ID = new OrmAnnotationDescriptor<>(
			MapsId.class,
			MapsIdJpaAnnotation.class
	);
	OrmAnnotationDescriptor<NamedAttributeNode,NamedAttributeNodeJpaAnnotation> NAMED_ATTRIBUTE_NODE = new OrmAnnotationDescriptor<>(
			NamedAttributeNode.class,
			NamedAttributeNodeJpaAnnotation.class
	);
	OrmAnnotationDescriptor<NamedEntityGraphs,NamedEntityGraphsJpaAnnotation> NAMED_ENTITY_GRAPHS = new OrmAnnotationDescriptor<>(
			NamedEntityGraphs.class,
			NamedEntityGraphsJpaAnnotation.class
	);
	OrmAnnotationDescriptor<NamedEntityGraph,NamedEntityGraphJpaAnnotation> NAMED_ENTITY_GRAPH = new OrmAnnotationDescriptor<>(
			NamedEntityGraph.class,
			NamedEntityGraphJpaAnnotation.class,
			NAMED_ENTITY_GRAPHS
	);
	OrmAnnotationDescriptor<NamedNativeQueries,NamedNativeQueriesJpaAnnotation> NAMED_NATIVE_QUERIES = new OrmAnnotationDescriptor<>(
			NamedNativeQueries.class,
			NamedNativeQueriesJpaAnnotation.class
	);
	OrmAnnotationDescriptor<NamedNativeQuery,NamedNativeQueryJpaAnnotation> NAMED_NATIVE_QUERY = new OrmAnnotationDescriptor<>(
			NamedNativeQuery.class,
			NamedNativeQueryJpaAnnotation.class,
			NAMED_NATIVE_QUERIES
	);
	OrmAnnotationDescriptor<NamedQueries,NamedQueriesJpaAnnotation> NAMED_QUERIES = new OrmAnnotationDescriptor<>(
			NamedQueries.class,
			NamedQueriesJpaAnnotation.class
	);
	OrmAnnotationDescriptor<NamedQuery,NamedQueryJpaAnnotation> NAMED_QUERY = new OrmAnnotationDescriptor<>(
			NamedQuery.class,
			NamedQueryJpaAnnotation.class,
			NAMED_QUERIES
	);
	OrmAnnotationDescriptor<NamedStoredProcedureQueries,NamedStoredProcedureQueriesJpaAnnotation> NAMED_STORED_PROCEDURE_QUERIES = new OrmAnnotationDescriptor<>(
			NamedStoredProcedureQueries.class,
			NamedStoredProcedureQueriesJpaAnnotation.class
	);
	OrmAnnotationDescriptor<NamedStoredProcedureQuery,NamedStoredProcedureQueryJpaAnnotation> NAMED_STORED_PROCEDURE_QUERY = new OrmAnnotationDescriptor<>(
			NamedStoredProcedureQuery.class,
			NamedStoredProcedureQueryJpaAnnotation.class,
			NAMED_STORED_PROCEDURE_QUERIES
	);
	OrmAnnotationDescriptor<NamedSubgraph,NamedSubgraphJpaAnnotation> NAMED_SUBGRAPH = new OrmAnnotationDescriptor<>(
			NamedSubgraph.class,
			NamedSubgraphJpaAnnotation.class
	);
	OrmAnnotationDescriptor<OneToMany,OneToManyJpaAnnotation> ONE_TO_MANY = new OrmAnnotationDescriptor<>(
			OneToMany.class,
			OneToManyJpaAnnotation.class
	);
	OrmAnnotationDescriptor<OneToOne, OneToOneJpaAnnotation> ONE_TO_ONE = new OrmAnnotationDescriptor<>(
			OneToOne.class,
			OneToOneJpaAnnotation.class
	);
	OrmAnnotationDescriptor<OrderBy,OrderByJpaAnnotation> ORDER_BY = new OrmAnnotationDescriptor<>(
			OrderBy.class,
			OrderByJpaAnnotation.class
	);
	OrmAnnotationDescriptor<OrderColumn,OrderColumnJpaAnnotation> ORDER_COLUMN = new OrmAnnotationDescriptor<>(
			OrderColumn.class,
			OrderColumnJpaAnnotation.class
	);
	OrmAnnotationDescriptor<PostLoad,PostLoadJpaAnnotation> POST_LOAD = new OrmAnnotationDescriptor<>(
			PostLoad.class,
			PostLoadJpaAnnotation.class
	);
	OrmAnnotationDescriptor<PostPersist,PostPersistJpaAnnotation> POST_PERSIST = new OrmAnnotationDescriptor<>(
			PostPersist.class,
			PostPersistJpaAnnotation.class
	);
	OrmAnnotationDescriptor<PostRemove,PostRemoveJpaAnnotation> POST_REMOVE = new OrmAnnotationDescriptor<>(
			PostRemove.class,
			PostRemoveJpaAnnotation.class
	);
	OrmAnnotationDescriptor<PostUpdate,PostUpdateJpaAnnotation> POST_UPDATE = new OrmAnnotationDescriptor<>(
			PostUpdate.class,
			PostUpdateJpaAnnotation.class
	);
	OrmAnnotationDescriptor<PrePersist,PrePersistJpaAnnotation> PRE_PERSIST = new OrmAnnotationDescriptor<>(
			PrePersist.class,
			PrePersistJpaAnnotation.class
	);
	OrmAnnotationDescriptor<PreRemove,PreRemoveJpaAnnotation> PRE_REMOVE = new OrmAnnotationDescriptor<>(
			PreRemove.class,
			PreRemoveJpaAnnotation.class
	);
	OrmAnnotationDescriptor<PreUpdate,PreUpdateJpaAnnotation> PRE_UPDATE = new OrmAnnotationDescriptor<>(
			PreUpdate.class,
			PreUpdateJpaAnnotation.class
	);
	OrmAnnotationDescriptor<PrimaryKeyJoinColumns,PrimaryKeyJoinColumnsJpaAnnotation> PRIMARY_KEY_JOIN_COLUMNS = new OrmAnnotationDescriptor<>(
			PrimaryKeyJoinColumns.class,
			PrimaryKeyJoinColumnsJpaAnnotation.class
	);
	OrmAnnotationDescriptor<PrimaryKeyJoinColumn, PrimaryKeyJoinColumnJpaAnnotation> PRIMARY_KEY_JOIN_COLUMN = new OrmAnnotationDescriptor<>(
			PrimaryKeyJoinColumn.class,
			PrimaryKeyJoinColumnJpaAnnotation.class,
			PRIMARY_KEY_JOIN_COLUMNS
	);
	OrmAnnotationDescriptor<QueryHint,QueryHintJpaAnnotation> QUERY_HINT = new OrmAnnotationDescriptor<>(
			QueryHint.class,
			QueryHintJpaAnnotation.class
	);
	OrmAnnotationDescriptor<SecondaryTables,SecondaryTablesJpaAnnotation> SECONDARY_TABLES = new OrmAnnotationDescriptor<>(
			SecondaryTables.class,
			SecondaryTablesJpaAnnotation.class
	);
	OrmAnnotationDescriptor<SecondaryTable,SecondaryTableJpaAnnotation> SECONDARY_TABLE = new OrmAnnotationDescriptor<>(
			SecondaryTable.class,
			SecondaryTableJpaAnnotation.class,
			SECONDARY_TABLES
	);
	OrmAnnotationDescriptor<SequenceGenerators,SequenceGeneratorsJpaAnnotation> SEQUENCE_GENERATORS = new OrmAnnotationDescriptor<>(
			SequenceGenerators.class,
			SequenceGeneratorsJpaAnnotation.class
	);
	OrmAnnotationDescriptor<SequenceGenerator,SequenceGeneratorJpaAnnotation> SEQUENCE_GENERATOR = new OrmAnnotationDescriptor<>(
			SequenceGenerator.class,
			SequenceGeneratorJpaAnnotation.class,
			SEQUENCE_GENERATORS
	);
	OrmAnnotationDescriptor<SqlResultSetMappings,SqlResultSetMappingsJpaAnnotation> SQL_RESULT_SET_MAPPINGS = new OrmAnnotationDescriptor<>(
			SqlResultSetMappings.class,
			SqlResultSetMappingsJpaAnnotation.class
	);
	OrmAnnotationDescriptor<SqlResultSetMapping,SqlResultSetMappingJpaAnnotation> SQL_RESULT_SET_MAPPING = new OrmAnnotationDescriptor<>(
			SqlResultSetMapping.class,
			SqlResultSetMappingJpaAnnotation.class,
			SQL_RESULT_SET_MAPPINGS
	);
	OrmAnnotationDescriptor<StoredProcedureParameter,StoredProcedureParameterJpaAnnotation> STORED_PROCEDURE_PARAMETER = new OrmAnnotationDescriptor<>(
			StoredProcedureParameter.class,
			StoredProcedureParameterJpaAnnotation.class
	);
	OrmAnnotationDescriptor<Table,TableJpaAnnotation> TABLE = new OrmAnnotationDescriptor<>(
			Table.class,
			TableJpaAnnotation.class
	);
	OrmAnnotationDescriptor<TableGenerators,TableGeneratorsJpaAnnotation> TABLE_GENERATORS = new OrmAnnotationDescriptor<>(
			TableGenerators.class,
			TableGeneratorsJpaAnnotation.class
	);
	OrmAnnotationDescriptor<TableGenerator,TableGeneratorJpaAnnotation> TABLE_GENERATOR = new OrmAnnotationDescriptor<>(
			TableGenerator.class,
			TableGeneratorJpaAnnotation.class,
			TABLE_GENERATORS
	);
	OrmAnnotationDescriptor<Temporal,TemporalJpaAnnotation> TEMPORAL = new OrmAnnotationDescriptor<>(
			Temporal.class,
			TemporalJpaAnnotation.class
	);
	OrmAnnotationDescriptor<Transient,TransientJpaAnnotation> TRANSIENT = new OrmAnnotationDescriptor<>(
			Transient.class,
			TransientJpaAnnotation.class
	);
	OrmAnnotationDescriptor<UniqueConstraint,UniqueConstraintJpaAnnotation> UNIQUE_CONSTRAINT = new OrmAnnotationDescriptor<>(
			UniqueConstraint.class,
			UniqueConstraintJpaAnnotation.class
	);
	OrmAnnotationDescriptor<Version,VersionJpaAnnotation> VERSION = new OrmAnnotationDescriptor<>(
			Version.class,
			VersionJpaAnnotation.class
	);

	static void forEachAnnotation(Consumer<AnnotationDescriptor<? extends Annotation>> consumer) {
		OrmAnnotationHelper.forEachOrmAnnotation( JpaAnnotations.class, consumer );
	}
}
