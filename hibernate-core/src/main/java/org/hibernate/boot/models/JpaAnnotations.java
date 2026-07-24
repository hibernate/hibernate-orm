/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models;

import java.lang.annotation.Annotation;
import java.util.EnumSet;
import java.util.function.Consumer;

import jakarta.persistence.EntityListener;
import jakarta.persistence.ExcludedFromVersioning;
import jakarta.persistence.NamedNativeStatement;
import jakarta.persistence.NamedNativeStatements;
import jakarta.persistence.NamedStatement;
import jakarta.persistence.NamedStatements;
import jakarta.persistence.spi.Discoverable;
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
import org.hibernate.boot.models.annotations.internal.ColumnResultsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ConstructorResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ConstructorResultsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ConvertJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ConverterJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ConvertsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.DiscoverableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.DiscriminatorColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.DiscriminatorValueJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ElementCollectionJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EmbeddableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EmbeddedIdJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EmbeddedJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityListenerJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityListenersJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityResultJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EntityResultsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EnumeratedJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.EnumeratedValueJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ExcludeDefaultListenersJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ExcludeSuperclassListenersJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ExcludedFromVersioningJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.FetchJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.FetchesJpaAnnotation;
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
import org.hibernate.boot.models.annotations.internal.NamedNativeStatementJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedNativeStatementsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedQueriesJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedStatementJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedStatementsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedStoredProcedureQueriesJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedStoredProcedureQueryJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedSubgraphJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OneToManyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OneToOneJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OrderByJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OrderColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PostDeleteJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PostInsertJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PostLoadJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PostPersistJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PostRemoveJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PostUpdateJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PostUpsertJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PreDeleteJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PreInsertJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PreMergeJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PrePersistJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PreRemoveJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PreUpdateJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PreUpsertJpaAnnotation;
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
import org.hibernate.models.Creator;
import org.hibernate.models.spi.MutableAnnotationDescriptor;
import org.hibernate.models.spi.AnnotationDescriptor;

import static org.hibernate.models.spi.AnnotationTarget.Kind;

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
import jakarta.persistence.Fetch;
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
import jakarta.persistence.PostDelete;
import jakarta.persistence.PostInsert;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PostUpsert;
import jakarta.persistence.PreDelete;
import jakarta.persistence.PreInsert;
import jakarta.persistence.PreMerge;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.PreUpsert;
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
	MutableAnnotationDescriptor<Access,AccessJpaAnnotation> ACCESS = Creator.createCompleteAnnotationDescriptor(
			Access.class,
			AccessJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<AssociationOverrides,AssociationOverridesJpaAnnotation> ASSOCIATION_OVERRIDES = Creator.createCompleteAnnotationDescriptor(
			AssociationOverrides.class,
			AssociationOverridesJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<AssociationOverride,AssociationOverrideJpaAnnotation> ASSOCIATION_OVERRIDE = Creator.createCompleteAnnotationDescriptor(
			AssociationOverride.class,
			AssociationOverrideJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD ),
			false,
			ASSOCIATION_OVERRIDES
	);

	MutableAnnotationDescriptor<AttributeOverrides,AttributeOverridesJpaAnnotation> ATTRIBUTE_OVERRIDES = Creator.createCompleteAnnotationDescriptor(
			AttributeOverrides.class,
			AttributeOverridesJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<AttributeOverride,AttributeOverrideJpaAnnotation> ATTRIBUTE_OVERRIDE = Creator.createCompleteAnnotationDescriptor(
			AttributeOverride.class,
			AttributeOverrideJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD ),
			false,
			ATTRIBUTE_OVERRIDES
	);

	MutableAnnotationDescriptor<Basic,BasicJpaAnnotation> BASIC = Creator.createCompleteAnnotationDescriptor(
			Basic.class,
			BasicJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<Cacheable,CacheableJpaAnnotation> CACHEABLE = Creator.createCompleteAnnotationDescriptor(
			Cacheable.class,
			CacheableJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	MutableAnnotationDescriptor<CheckConstraint,CheckConstraintJpaAnnotation> CHECK_CONSTRAINT = Creator.createCompleteAnnotationDescriptor(
			CheckConstraint.class,
			CheckConstraintJpaAnnotation.class,
			EnumSet.noneOf( Kind.class ),
			false
	);

	MutableAnnotationDescriptor<CollectionTable,CollectionTableJpaAnnotation> COLLECTION_TABLE = Creator.createCompleteAnnotationDescriptor(
			CollectionTable.class,
			CollectionTableJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<Column,ColumnJpaAnnotation> COLUMN = Creator.createCompleteAnnotationDescriptor(
			Column.class,
			ColumnJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<ColumnResult.ColumnResults,ColumnResultsJpaAnnotation> COLUMN_RESULTS = Creator.createCompleteAnnotationDescriptor(
			ColumnResult.ColumnResults.class,
			ColumnResultsJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	MutableAnnotationDescriptor<ColumnResult,ColumnResultJpaAnnotation> COLUMN_RESULT = Creator.createCompleteAnnotationDescriptor(
			ColumnResult.class,
			ColumnResultJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false,
			COLUMN_RESULTS
	);

	MutableAnnotationDescriptor<ConstructorResult.ConstructorResults,ConstructorResultsJpaAnnotation> CONSTRUCTOR_RESULTS = Creator.createCompleteAnnotationDescriptor(
			ConstructorResult.ConstructorResults.class,
			ConstructorResultsJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	MutableAnnotationDescriptor<ConstructorResult,ConstructorResultJpaAnnotation> CONSTRUCTOR_RESULT = Creator.createCompleteAnnotationDescriptor(
			ConstructorResult.class,
			ConstructorResultJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false,
			CONSTRUCTOR_RESULTS
	);

	MutableAnnotationDescriptor<Converts,ConvertsJpaAnnotation> CONVERTS = Creator.createCompleteAnnotationDescriptor(
			Converts.class,
			ConvertsJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);

	MutableAnnotationDescriptor<Convert,ConvertJpaAnnotation> CONVERT = Creator.createCompleteAnnotationDescriptor(
			Convert.class,
			ConvertJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false,
			CONVERTS
	);

	MutableAnnotationDescriptor<Converter,ConverterJpaAnnotation> CONVERTER = Creator.createCompleteAnnotationDescriptor(
			Converter.class,
			ConverterJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	MutableAnnotationDescriptor<DiscriminatorColumn,DiscriminatorColumnJpaAnnotation> DISCRIMINATOR_COLUMN = Creator.createCompleteAnnotationDescriptor(
			DiscriminatorColumn.class,
			DiscriminatorColumnJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	MutableAnnotationDescriptor<Discoverable, DiscoverableJpaAnnotation> DISCOVERABLE = Creator.createCompleteAnnotationDescriptor(
			Discoverable.class,
			DiscoverableJpaAnnotation.class,
			EnumSet.of( Kind.ANNOTATION ),
			false
	);

	MutableAnnotationDescriptor<DiscriminatorValue,DiscriminatorValueJpaAnnotation> DISCRIMINATOR_VALUE = Creator.createCompleteAnnotationDescriptor(
			DiscriminatorValue.class,
			DiscriminatorValueJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	MutableAnnotationDescriptor<ElementCollection,ElementCollectionJpaAnnotation> ELEMENT_COLLECTION = Creator.createCompleteAnnotationDescriptor(
			ElementCollection.class,
			ElementCollectionJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<Embeddable,EmbeddableJpaAnnotation> EMBEDDABLE = Creator.createCompleteAnnotationDescriptor(
			Embeddable.class,
			EmbeddableJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	MutableAnnotationDescriptor<Embedded,EmbeddedJpaAnnotation> EMBEDDED = Creator.createCompleteAnnotationDescriptor(
			Embedded.class,
			EmbeddedJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<EmbeddedId,EmbeddedIdJpaAnnotation> EMBEDDED_ID = Creator.createCompleteAnnotationDescriptor(
			EmbeddedId.class,
			EmbeddedIdJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<Entity,EntityJpaAnnotation> ENTITY = Creator.createCompleteAnnotationDescriptor(
			Entity.class,
			EntityJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	MutableAnnotationDescriptor<EntityListener,EntityListenerJpaAnnotation> ENTITY_LISTENER = Creator.createCompleteAnnotationDescriptor(
			EntityListener.class,
			EntityListenerJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	MutableAnnotationDescriptor<EntityListeners,EntityListenersJpaAnnotation> ENTITY_LISTENERS = Creator.createCompleteAnnotationDescriptor(
			EntityListeners.class,
			EntityListenersJpaAnnotation.class
	);

	MutableAnnotationDescriptor<EntityResult.EntityResults,EntityResultsJpaAnnotation> ENTITY_RESULTS = Creator.createCompleteAnnotationDescriptor(
			EntityResult.EntityResults.class,
			EntityResultsJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	MutableAnnotationDescriptor<EntityResult,EntityResultJpaAnnotation> ENTITY_RESULT = Creator.createCompleteAnnotationDescriptor(
			EntityResult.class,
			EntityResultJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false,
			ENTITY_RESULTS
	);

	MutableAnnotationDescriptor<Enumerated,EnumeratedJpaAnnotation> ENUMERATED = Creator.createCompleteAnnotationDescriptor(
			Enumerated.class,
			EnumeratedJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<EnumeratedValue,EnumeratedValueJpaAnnotation> ENUMERATED_VALUE = Creator.createCompleteAnnotationDescriptor(
			EnumeratedValue.class,
			EnumeratedValueJpaAnnotation.class,
			EnumSet.of( Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<ExcludeDefaultListeners,ExcludeDefaultListenersJpaAnnotation> EXCLUDE_DEFAULT_LISTENERS = Creator.createCompleteAnnotationDescriptor(
			ExcludeDefaultListeners.class,
			ExcludeDefaultListenersJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<ExcludedFromVersioning, ExcludedFromVersioningJpaAnnotation> EXCLUDE_FROM_VERSIONING = Creator.createCompleteAnnotationDescriptor(
			ExcludedFromVersioning.class,
			ExcludedFromVersioningJpaAnnotation.class
	);
	MutableAnnotationDescriptor<ExcludeSuperclassListeners,ExcludeSuperclassListenersJpaAnnotation> EXCLUDE_SUPERCLASS_LISTENERS = Creator.createCompleteAnnotationDescriptor(
			ExcludeSuperclassListeners.class,
			ExcludeSuperclassListenersJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	MutableAnnotationDescriptor<Fetch.Fetches,FetchesJpaAnnotation> FETCHES = Creator.createCompleteAnnotationDescriptor(
			Fetch.Fetches.class,
			FetchesJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<Fetch,FetchJpaAnnotation> FETCH = Creator.createCompleteAnnotationDescriptor(
			Fetch.class,
			FetchJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false,
			FETCHES
	);

	MutableAnnotationDescriptor<FieldResult,FieldResultJpaAnnotation> FIELD_RESULT = Creator.createCompleteAnnotationDescriptor(
			FieldResult.class,
			FieldResultJpaAnnotation.class,
			EnumSet.noneOf( Kind.class ),
			false
	);

	MutableAnnotationDescriptor<ForeignKey,ForeignKeyJpaAnnotation> FOREIGN_KEY = Creator.createCompleteAnnotationDescriptor(
			ForeignKey.class,
			ForeignKeyJpaAnnotation.class,
			EnumSet.noneOf( Kind.class ),
			false
	);

	MutableAnnotationDescriptor<GeneratedValue,GeneratedValueJpaAnnotation> GENERATED_VALUE = Creator.createCompleteAnnotationDescriptor(
			GeneratedValue.class,
			GeneratedValueJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<Id,IdJpaAnnotation> ID = Creator.createCompleteAnnotationDescriptor(
			Id.class,
			IdJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<IdClass,IdClassJpaAnnotation> ID_CLASS = Creator.createCompleteAnnotationDescriptor(
			IdClass.class,
			IdClassJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	MutableAnnotationDescriptor<Index,IndexJpaAnnotation> INDEX = Creator.createCompleteAnnotationDescriptor(
			Index.class,
			IndexJpaAnnotation.class,
			EnumSet.noneOf( Kind.class ),
			false
	);

	MutableAnnotationDescriptor<Inheritance,InheritanceJpaAnnotation> INHERITANCE = Creator.createCompleteAnnotationDescriptor(
			Inheritance.class,
			InheritanceJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	MutableAnnotationDescriptor<JoinColumns,JoinColumnsJpaAnnotation> JOIN_COLUMNS = Creator.createCompleteAnnotationDescriptor(
			JoinColumns.class,
			JoinColumnsJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<JoinColumn,JoinColumnJpaAnnotation> JOIN_COLUMN = Creator.createCompleteAnnotationDescriptor(
			JoinColumn.class,
			JoinColumnJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false,
			JOIN_COLUMNS
	);

	MutableAnnotationDescriptor<JoinTable,JoinTableJpaAnnotation> JOIN_TABLE = Creator.createCompleteAnnotationDescriptor(
			JoinTable.class,
			JoinTableJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<Lob,LobJpaAnnotation> LOB = Creator.createCompleteAnnotationDescriptor(
			Lob.class,
			LobJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<ManyToMany,ManyToManyJpaAnnotation> MANY_TO_MANY = Creator.createCompleteAnnotationDescriptor(
			ManyToMany.class,
			ManyToManyJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<ManyToOne,ManyToOneJpaAnnotation> MANY_TO_ONE = Creator.createCompleteAnnotationDescriptor(
			ManyToOne.class,
			ManyToOneJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<MapKey,MapKeyJpaAnnotation> MAP_KEY = Creator.createCompleteAnnotationDescriptor(
			MapKey.class,
			MapKeyJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<MapKeyClass,MapKeyClassJpaAnnotation> MAP_KEY_CLASS = Creator.createCompleteAnnotationDescriptor(
			MapKeyClass.class,
			MapKeyClassJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<MapKeyColumn,MapKeyColumnJpaAnnotation> MAP_KEY_COLUMN = Creator.createCompleteAnnotationDescriptor(
			MapKeyColumn.class,
			MapKeyColumnJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<MapKeyEnumerated,MapKeyEnumeratedJpaAnnotation> MAP_KEY_ENUMERATED = Creator.createCompleteAnnotationDescriptor(
			MapKeyEnumerated.class,
			MapKeyEnumeratedJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<MapKeyJoinColumns,MapKeyJoinColumnsJpaAnnotation> MAP_KEY_JOIN_COLUMNS = Creator.createCompleteAnnotationDescriptor(
			MapKeyJoinColumns.class,
			MapKeyJoinColumnsJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<MapKeyJoinColumn,MapKeyJoinColumnJpaAnnotation> MAP_KEY_JOIN_COLUMN = Creator.createCompleteAnnotationDescriptor(
			MapKeyJoinColumn.class,
			MapKeyJoinColumnJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false,
			MAP_KEY_JOIN_COLUMNS
	);

	MutableAnnotationDescriptor<MapKeyTemporal,MapKeyTemporalJpaAnnotation> MAP_KEY_TEMPORAL = Creator.createCompleteAnnotationDescriptor(
			MapKeyTemporal.class,
			MapKeyTemporalJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<MappedSuperclass,MappedSuperclassJpaAnnotation> MAPPED_SUPERCLASS = Creator.createCompleteAnnotationDescriptor(
			MappedSuperclass.class,
			MappedSuperclassJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	MutableAnnotationDescriptor<MapsId,MapsIdJpaAnnotation> MAPS_ID = Creator.createCompleteAnnotationDescriptor(
			MapsId.class,
			MapsIdJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<NamedAttributeNode,NamedAttributeNodeJpaAnnotation> NAMED_ATTRIBUTE_NODE = Creator.createCompleteAnnotationDescriptor(
			NamedAttributeNode.class,
			NamedAttributeNodeJpaAnnotation.class,
			EnumSet.noneOf( Kind.class ),
			false
	);

	MutableAnnotationDescriptor<NamedEntityGraphs,NamedEntityGraphsJpaAnnotation> NAMED_ENTITY_GRAPHS = Creator.createCompleteAnnotationDescriptor(
			NamedEntityGraphs.class,
			NamedEntityGraphsJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	MutableAnnotationDescriptor<NamedEntityGraph,NamedEntityGraphJpaAnnotation> NAMED_ENTITY_GRAPH = Creator.createCompleteAnnotationDescriptor(
			NamedEntityGraph.class,
			NamedEntityGraphJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false,
			NAMED_ENTITY_GRAPHS
	);

	MutableAnnotationDescriptor<NamedNativeQueries,NamedNativeQueriesJpaAnnotation> NAMED_NATIVE_QUERIES = Creator.createCompleteAnnotationDescriptor(
			NamedNativeQueries.class,
			NamedNativeQueriesJpaAnnotation.class
	);

	MutableAnnotationDescriptor<NamedNativeQuery,NamedNativeQueryJpaAnnotation> NAMED_NATIVE_QUERY = Creator.createCompleteAnnotationDescriptor(
			NamedNativeQuery.class,
			NamedNativeQueryJpaAnnotation.class,
			NAMED_NATIVE_QUERIES
	);

	MutableAnnotationDescriptor<NamedQueries,NamedQueriesJpaAnnotation> NAMED_QUERIES = Creator.createCompleteAnnotationDescriptor(
			NamedQueries.class,
			NamedQueriesJpaAnnotation.class
	);

	MutableAnnotationDescriptor<NamedQuery,NamedQueryJpaAnnotation> NAMED_QUERY = Creator.createCompleteAnnotationDescriptor(
			NamedQuery.class,
			NamedQueryJpaAnnotation.class,
			NAMED_QUERIES
	);
	MutableAnnotationDescriptor<NamedStatements, NamedStatementsJpaAnnotation> NAMED_STATEMENTS = Creator.createCompleteAnnotationDescriptor(
			NamedStatements.class,
			NamedStatementsJpaAnnotation.class
	);
	MutableAnnotationDescriptor<NamedStatement, NamedStatementJpaAnnotation> NAMED_STATEMENT = Creator.createCompleteAnnotationDescriptor(
			NamedStatement.class,
			NamedStatementJpaAnnotation.class,
			NAMED_STATEMENTS
	);
	MutableAnnotationDescriptor<NamedNativeStatements, NamedNativeStatementsJpaAnnotation> NAMED_NATIVE_STATEMENTS = Creator.createCompleteAnnotationDescriptor(
			NamedNativeStatements.class,
			NamedNativeStatementsJpaAnnotation.class
	);
	MutableAnnotationDescriptor<NamedNativeStatement, NamedNativeStatementJpaAnnotation> NAMED_NATIVE_STATEMENT = Creator.createCompleteAnnotationDescriptor(
			NamedNativeStatement.class,
			NamedNativeStatementJpaAnnotation.class,
			NAMED_NATIVE_STATEMENTS
	);
	MutableAnnotationDescriptor<NamedStoredProcedureQueries,NamedStoredProcedureQueriesJpaAnnotation> NAMED_STORED_PROCEDURE_QUERIES = Creator.createCompleteAnnotationDescriptor(
			NamedStoredProcedureQueries.class,
			NamedStoredProcedureQueriesJpaAnnotation.class
	);

	MutableAnnotationDescriptor<NamedStoredProcedureQuery,NamedStoredProcedureQueryJpaAnnotation> NAMED_STORED_PROCEDURE_QUERY = Creator.createCompleteAnnotationDescriptor(
			NamedStoredProcedureQuery.class,
			NamedStoredProcedureQueryJpaAnnotation.class,
			NAMED_STORED_PROCEDURE_QUERIES
	);

	MutableAnnotationDescriptor<NamedSubgraph,NamedSubgraphJpaAnnotation> NAMED_SUBGRAPH = Creator.createCompleteAnnotationDescriptor(
			NamedSubgraph.class,
			NamedSubgraphJpaAnnotation.class,
			EnumSet.noneOf( Kind.class ),
			false
	);

	MutableAnnotationDescriptor<OneToMany,OneToManyJpaAnnotation> ONE_TO_MANY = Creator.createCompleteAnnotationDescriptor(
			OneToMany.class,
			OneToManyJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<OneToOne,OneToOneJpaAnnotation> ONE_TO_ONE = Creator.createCompleteAnnotationDescriptor(
			OneToOne.class,
			OneToOneJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<OrderBy,OrderByJpaAnnotation> ORDER_BY = Creator.createCompleteAnnotationDescriptor(
			OrderBy.class,
			OrderByJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<OrderColumn,OrderColumnJpaAnnotation> ORDER_COLUMN = Creator.createCompleteAnnotationDescriptor(
			OrderColumn.class,
			OrderColumnJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<PostLoad,PostLoadJpaAnnotation> POST_LOAD = Creator.createCompleteAnnotationDescriptor(
			PostLoad.class,
			PostLoadJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	MutableAnnotationDescriptor<PostInsert,PostInsertJpaAnnotation> POST_INSERT = Creator.createCompleteAnnotationDescriptor(
			PostInsert.class,
			PostInsertJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	MutableAnnotationDescriptor<PostPersist,PostPersistJpaAnnotation> POST_PERSIST = Creator.createCompleteAnnotationDescriptor(
			PostPersist.class,
			PostPersistJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	MutableAnnotationDescriptor<PostDelete,PostDeleteJpaAnnotation> POST_DELETE = Creator.createCompleteAnnotationDescriptor(
			PostDelete.class,
			PostDeleteJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	MutableAnnotationDescriptor<PostRemove,PostRemoveJpaAnnotation> POST_REMOVE = Creator.createCompleteAnnotationDescriptor(
			PostRemove.class,
			PostRemoveJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	MutableAnnotationDescriptor<PostUpdate,PostUpdateJpaAnnotation> POST_UPDATE = Creator.createCompleteAnnotationDescriptor(
			PostUpdate.class,
			PostUpdateJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	MutableAnnotationDescriptor<PostUpsert,PostUpsertJpaAnnotation> POST_UPSERT = Creator.createCompleteAnnotationDescriptor(
			PostUpsert.class,
			PostUpsertJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	MutableAnnotationDescriptor<PreInsert,PreInsertJpaAnnotation> PRE_INSERT = Creator.createCompleteAnnotationDescriptor(
			PreInsert.class,
			PreInsertJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	MutableAnnotationDescriptor<PreMerge,PreMergeJpaAnnotation> PRE_MERGE = Creator.createCompleteAnnotationDescriptor(
			PreMerge.class,
			PreMergeJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	MutableAnnotationDescriptor<PrePersist,PrePersistJpaAnnotation> PRE_PERSIST = Creator.createCompleteAnnotationDescriptor(
			PrePersist.class,
			PrePersistJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	MutableAnnotationDescriptor<PreDelete,PreDeleteJpaAnnotation> PRE_DELETE = Creator.createCompleteAnnotationDescriptor(
			PreDelete.class,
			PreDeleteJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	MutableAnnotationDescriptor<PreRemove,PreRemoveJpaAnnotation> PRE_REMOVE = Creator.createCompleteAnnotationDescriptor(
			PreRemove.class,
			PreRemoveJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	MutableAnnotationDescriptor<PreUpdate,PreUpdateJpaAnnotation> PRE_UPDATE = Creator.createCompleteAnnotationDescriptor(
			PreUpdate.class,
			PreUpdateJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	MutableAnnotationDescriptor<PreUpsert,PreUpsertJpaAnnotation> PRE_UPSERT = Creator.createCompleteAnnotationDescriptor(
			PreUpsert.class,
			PreUpsertJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	MutableAnnotationDescriptor<PrimaryKeyJoinColumns,PrimaryKeyJoinColumnsJpaAnnotation> PRIMARY_KEY_JOIN_COLUMNS = Creator.createCompleteAnnotationDescriptor(
			PrimaryKeyJoinColumns.class,
			PrimaryKeyJoinColumnsJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<PrimaryKeyJoinColumn,PrimaryKeyJoinColumnJpaAnnotation> PRIMARY_KEY_JOIN_COLUMN = Creator.createCompleteAnnotationDescriptor(
			PrimaryKeyJoinColumn.class,
			PrimaryKeyJoinColumnJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD ),
			false,
			PRIMARY_KEY_JOIN_COLUMNS
	);

	MutableAnnotationDescriptor<QueryHint,QueryHintJpaAnnotation> QUERY_HINT = Creator.createCompleteAnnotationDescriptor(
			QueryHint.class,
			QueryHintJpaAnnotation.class,
			EnumSet.noneOf( Kind.class ),
			false
	);

	MutableAnnotationDescriptor<SecondaryTables,SecondaryTablesJpaAnnotation> SECONDARY_TABLES = Creator.createCompleteAnnotationDescriptor(
			SecondaryTables.class,
			SecondaryTablesJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	MutableAnnotationDescriptor<SecondaryTable,SecondaryTableJpaAnnotation> SECONDARY_TABLE = Creator.createCompleteAnnotationDescriptor(
			SecondaryTable.class,
			SecondaryTableJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false,
			SECONDARY_TABLES
	);

	MutableAnnotationDescriptor<SequenceGenerators,SequenceGeneratorsJpaAnnotation> SEQUENCE_GENERATORS = Creator.createCompleteAnnotationDescriptor(
			SequenceGenerators.class,
			SequenceGeneratorsJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD, Kind.PACKAGE ),
			false
	);

	MutableAnnotationDescriptor<SequenceGenerator,SequenceGeneratorJpaAnnotation> SEQUENCE_GENERATOR = Creator.createCompleteAnnotationDescriptor(
			SequenceGenerator.class,
			SequenceGeneratorJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD, Kind.PACKAGE ),
			false,
			SEQUENCE_GENERATORS
	);

	MutableAnnotationDescriptor<SqlResultSetMappings,SqlResultSetMappingsJpaAnnotation> SQL_RESULT_SET_MAPPINGS = Creator.createCompleteAnnotationDescriptor(
			SqlResultSetMappings.class,
			SqlResultSetMappingsJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	MutableAnnotationDescriptor<SqlResultSetMapping,SqlResultSetMappingJpaAnnotation> SQL_RESULT_SET_MAPPING = Creator.createCompleteAnnotationDescriptor(
			SqlResultSetMapping.class,
			SqlResultSetMappingJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD ),
			false,
			SQL_RESULT_SET_MAPPINGS
	);

	MutableAnnotationDescriptor<StoredProcedureParameter,StoredProcedureParameterJpaAnnotation> STORED_PROCEDURE_PARAMETER = Creator.createCompleteAnnotationDescriptor(
			StoredProcedureParameter.class,
			StoredProcedureParameterJpaAnnotation.class,
			EnumSet.noneOf( Kind.class ),
			false
	);

	MutableAnnotationDescriptor<Table,TableJpaAnnotation> TABLE = Creator.createCompleteAnnotationDescriptor(
			Table.class,
			TableJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	MutableAnnotationDescriptor<TableGenerators,TableGeneratorsJpaAnnotation> TABLE_GENERATORS = Creator.createCompleteAnnotationDescriptor(
			TableGenerators.class,
			TableGeneratorsJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD, Kind.PACKAGE ),
			false
	);

	MutableAnnotationDescriptor<TableGenerator,TableGeneratorJpaAnnotation> TABLE_GENERATOR = Creator.createCompleteAnnotationDescriptor(
			TableGenerator.class,
			TableGeneratorJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD, Kind.PACKAGE ),
			false,
			TABLE_GENERATORS
	);

	MutableAnnotationDescriptor<Temporal,TemporalJpaAnnotation> TEMPORAL = Creator.createCompleteAnnotationDescriptor(
			Temporal.class,
			TemporalJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<Transient,TransientJpaAnnotation> TRANSIENT = Creator.createCompleteAnnotationDescriptor(
			Transient.class,
			TransientJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	MutableAnnotationDescriptor<UniqueConstraint,UniqueConstraintJpaAnnotation> UNIQUE_CONSTRAINT = Creator.createCompleteAnnotationDescriptor(
			UniqueConstraint.class,
			UniqueConstraintJpaAnnotation.class,
			EnumSet.noneOf( Kind.class ),
			false
	);

	MutableAnnotationDescriptor<Version,VersionJpaAnnotation> VERSION = Creator.createCompleteAnnotationDescriptor(
			Version.class,
			VersionJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	static void forEachAnnotation(Consumer<AnnotationDescriptor<? extends Annotation>> consumer) {
		OrmAnnotationHelper.forEachOrmAnnotation( JpaAnnotations.class, consumer );
	}
}
