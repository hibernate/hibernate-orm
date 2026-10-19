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
import org.hibernate.models.CompleteAnnotationDescriptor;
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
	CompleteAnnotationDescriptor<Access,AccessJpaAnnotation> ACCESS = new CompleteAnnotationDescriptor<>(
			Access.class,
			AccessJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<AssociationOverrides,AssociationOverridesJpaAnnotation> ASSOCIATION_OVERRIDES = new CompleteAnnotationDescriptor<>(
			AssociationOverrides.class,
			AssociationOverridesJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<AssociationOverride,AssociationOverrideJpaAnnotation> ASSOCIATION_OVERRIDE = new CompleteAnnotationDescriptor<>(
			AssociationOverride.class,
			AssociationOverrideJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD ),
			false,
			ASSOCIATION_OVERRIDES
	);

	CompleteAnnotationDescriptor<AttributeOverrides,AttributeOverridesJpaAnnotation> ATTRIBUTE_OVERRIDES = new CompleteAnnotationDescriptor<>(
			AttributeOverrides.class,
			AttributeOverridesJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<AttributeOverride,AttributeOverrideJpaAnnotation> ATTRIBUTE_OVERRIDE = new CompleteAnnotationDescriptor<>(
			AttributeOverride.class,
			AttributeOverrideJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD ),
			false,
			ATTRIBUTE_OVERRIDES
	);

	CompleteAnnotationDescriptor<Basic,BasicJpaAnnotation> BASIC = new CompleteAnnotationDescriptor<>(
			Basic.class,
			BasicJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<Cacheable,CacheableJpaAnnotation> CACHEABLE = new CompleteAnnotationDescriptor<>(
			Cacheable.class,
			CacheableJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	CompleteAnnotationDescriptor<CheckConstraint,CheckConstraintJpaAnnotation> CHECK_CONSTRAINT = new CompleteAnnotationDescriptor<>(
			CheckConstraint.class,
			CheckConstraintJpaAnnotation.class,
			EnumSet.noneOf( Kind.class ),
			false
	);

	CompleteAnnotationDescriptor<CollectionTable,CollectionTableJpaAnnotation> COLLECTION_TABLE = new CompleteAnnotationDescriptor<>(
			CollectionTable.class,
			CollectionTableJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<Column,ColumnJpaAnnotation> COLUMN = new CompleteAnnotationDescriptor<>(
			Column.class,
			ColumnJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<ColumnResult.ColumnResults,ColumnResultsJpaAnnotation> COLUMN_RESULTS = new CompleteAnnotationDescriptor<>(
			ColumnResult.ColumnResults.class,
			ColumnResultsJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	CompleteAnnotationDescriptor<ColumnResult,ColumnResultJpaAnnotation> COLUMN_RESULT = new CompleteAnnotationDescriptor<>(
			ColumnResult.class,
			ColumnResultJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false,
			COLUMN_RESULTS
	);

	CompleteAnnotationDescriptor<ConstructorResult.ConstructorResults,ConstructorResultsJpaAnnotation> CONSTRUCTOR_RESULTS = new CompleteAnnotationDescriptor<>(
			ConstructorResult.ConstructorResults.class,
			ConstructorResultsJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	CompleteAnnotationDescriptor<ConstructorResult,ConstructorResultJpaAnnotation> CONSTRUCTOR_RESULT = new CompleteAnnotationDescriptor<>(
			ConstructorResult.class,
			ConstructorResultJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false,
			CONSTRUCTOR_RESULTS
	);

	CompleteAnnotationDescriptor<Converts,ConvertsJpaAnnotation> CONVERTS = new CompleteAnnotationDescriptor<>(
			Converts.class,
			ConvertsJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);

	CompleteAnnotationDescriptor<Convert,ConvertJpaAnnotation> CONVERT = new CompleteAnnotationDescriptor<>(
			Convert.class,
			ConvertJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false,
			CONVERTS
	);

	CompleteAnnotationDescriptor<Converter,ConverterJpaAnnotation> CONVERTER = new CompleteAnnotationDescriptor<>(
			Converter.class,
			ConverterJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	CompleteAnnotationDescriptor<DiscriminatorColumn,DiscriminatorColumnJpaAnnotation> DISCRIMINATOR_COLUMN = new CompleteAnnotationDescriptor<>(
			DiscriminatorColumn.class,
			DiscriminatorColumnJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	CompleteAnnotationDescriptor<Discoverable, DiscoverableJpaAnnotation> DISCOVERABLE = new CompleteAnnotationDescriptor<>(
			Discoverable.class,
			DiscoverableJpaAnnotation.class,
			EnumSet.of( Kind.ANNOTATION ),
			false
	);

	CompleteAnnotationDescriptor<DiscriminatorValue,DiscriminatorValueJpaAnnotation> DISCRIMINATOR_VALUE = new CompleteAnnotationDescriptor<>(
			DiscriminatorValue.class,
			DiscriminatorValueJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	CompleteAnnotationDescriptor<ElementCollection,ElementCollectionJpaAnnotation> ELEMENT_COLLECTION = new CompleteAnnotationDescriptor<>(
			ElementCollection.class,
			ElementCollectionJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<Embeddable,EmbeddableJpaAnnotation> EMBEDDABLE = new CompleteAnnotationDescriptor<>(
			Embeddable.class,
			EmbeddableJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	CompleteAnnotationDescriptor<Embedded,EmbeddedJpaAnnotation> EMBEDDED = new CompleteAnnotationDescriptor<>(
			Embedded.class,
			EmbeddedJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<EmbeddedId,EmbeddedIdJpaAnnotation> EMBEDDED_ID = new CompleteAnnotationDescriptor<>(
			EmbeddedId.class,
			EmbeddedIdJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<Entity,EntityJpaAnnotation> ENTITY = new CompleteAnnotationDescriptor<>(
			Entity.class,
			EntityJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	CompleteAnnotationDescriptor<EntityListener,EntityListenerJpaAnnotation> ENTITY_LISTENER = new CompleteAnnotationDescriptor<>(
			EntityListener.class,
			EntityListenerJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	CompleteAnnotationDescriptor<EntityListeners,EntityListenersJpaAnnotation> ENTITY_LISTENERS = new CompleteAnnotationDescriptor<>(
			EntityListeners.class,
			EntityListenersJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false
	);

	CompleteAnnotationDescriptor<EntityResult.EntityResults,EntityResultsJpaAnnotation> ENTITY_RESULTS = new CompleteAnnotationDescriptor<>(
			EntityResult.EntityResults.class,
			EntityResultsJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	CompleteAnnotationDescriptor<EntityResult,EntityResultJpaAnnotation> ENTITY_RESULT = new CompleteAnnotationDescriptor<>(
			EntityResult.class,
			EntityResultJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false,
			ENTITY_RESULTS
	);

	CompleteAnnotationDescriptor<Enumerated,EnumeratedJpaAnnotation> ENUMERATED = new CompleteAnnotationDescriptor<>(
			Enumerated.class,
			EnumeratedJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<EnumeratedValue,EnumeratedValueJpaAnnotation> ENUMERATED_VALUE = new CompleteAnnotationDescriptor<>(
			EnumeratedValue.class,
			EnumeratedValueJpaAnnotation.class,
			EnumSet.of( Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<ExcludeDefaultListeners,ExcludeDefaultListenersJpaAnnotation> EXCLUDE_DEFAULT_LISTENERS = new CompleteAnnotationDescriptor<>(
			ExcludeDefaultListeners.class,
			ExcludeDefaultListenersJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<ExcludedFromVersioning, ExcludedFromVersioningJpaAnnotation> EXCLUDE_FROM_VERSIONING = new CompleteAnnotationDescriptor<>(
			ExcludedFromVersioning.class,
			ExcludedFromVersioningJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<ExcludeSuperclassListeners,ExcludeSuperclassListenersJpaAnnotation> EXCLUDE_SUPERCLASS_LISTENERS = new CompleteAnnotationDescriptor<>(
			ExcludeSuperclassListeners.class,
			ExcludeSuperclassListenersJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	CompleteAnnotationDescriptor<Fetch.Fetches,FetchesJpaAnnotation> FETCHES = new CompleteAnnotationDescriptor<>(
			Fetch.Fetches.class,
			FetchesJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<Fetch,FetchJpaAnnotation> FETCH = new CompleteAnnotationDescriptor<>(
			Fetch.class,
			FetchJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false,
			FETCHES
	);

	CompleteAnnotationDescriptor<FieldResult,FieldResultJpaAnnotation> FIELD_RESULT = new CompleteAnnotationDescriptor<>(
			FieldResult.class,
			FieldResultJpaAnnotation.class,
			EnumSet.noneOf( Kind.class ),
			false
	);

	CompleteAnnotationDescriptor<ForeignKey,ForeignKeyJpaAnnotation> FOREIGN_KEY = new CompleteAnnotationDescriptor<>(
			ForeignKey.class,
			ForeignKeyJpaAnnotation.class,
			EnumSet.noneOf( Kind.class ),
			false
	);

	CompleteAnnotationDescriptor<GeneratedValue,GeneratedValueJpaAnnotation> GENERATED_VALUE = new CompleteAnnotationDescriptor<>(
			GeneratedValue.class,
			GeneratedValueJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<Id,IdJpaAnnotation> ID = new CompleteAnnotationDescriptor<>(
			Id.class,
			IdJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<IdClass,IdClassJpaAnnotation> ID_CLASS = new CompleteAnnotationDescriptor<>(
			IdClass.class,
			IdClassJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	CompleteAnnotationDescriptor<Index,IndexJpaAnnotation> INDEX = new CompleteAnnotationDescriptor<>(
			Index.class,
			IndexJpaAnnotation.class,
			EnumSet.noneOf( Kind.class ),
			false
	);

	CompleteAnnotationDescriptor<Inheritance,InheritanceJpaAnnotation> INHERITANCE = new CompleteAnnotationDescriptor<>(
			Inheritance.class,
			InheritanceJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	CompleteAnnotationDescriptor<JoinColumns,JoinColumnsJpaAnnotation> JOIN_COLUMNS = new CompleteAnnotationDescriptor<>(
			JoinColumns.class,
			JoinColumnsJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<JoinColumn,JoinColumnJpaAnnotation> JOIN_COLUMN = new CompleteAnnotationDescriptor<>(
			JoinColumn.class,
			JoinColumnJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false,
			JOIN_COLUMNS
	);

	CompleteAnnotationDescriptor<JoinTable,JoinTableJpaAnnotation> JOIN_TABLE = new CompleteAnnotationDescriptor<>(
			JoinTable.class,
			JoinTableJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<Lob,LobJpaAnnotation> LOB = new CompleteAnnotationDescriptor<>(
			Lob.class,
			LobJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<ManyToMany,ManyToManyJpaAnnotation> MANY_TO_MANY = new CompleteAnnotationDescriptor<>(
			ManyToMany.class,
			ManyToManyJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<ManyToOne,ManyToOneJpaAnnotation> MANY_TO_ONE = new CompleteAnnotationDescriptor<>(
			ManyToOne.class,
			ManyToOneJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<MapKey,MapKeyJpaAnnotation> MAP_KEY = new CompleteAnnotationDescriptor<>(
			MapKey.class,
			MapKeyJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<MapKeyClass,MapKeyClassJpaAnnotation> MAP_KEY_CLASS = new CompleteAnnotationDescriptor<>(
			MapKeyClass.class,
			MapKeyClassJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<MapKeyColumn,MapKeyColumnJpaAnnotation> MAP_KEY_COLUMN = new CompleteAnnotationDescriptor<>(
			MapKeyColumn.class,
			MapKeyColumnJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<MapKeyEnumerated,MapKeyEnumeratedJpaAnnotation> MAP_KEY_ENUMERATED = new CompleteAnnotationDescriptor<>(
			MapKeyEnumerated.class,
			MapKeyEnumeratedJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<MapKeyJoinColumns,MapKeyJoinColumnsJpaAnnotation> MAP_KEY_JOIN_COLUMNS = new CompleteAnnotationDescriptor<>(
			MapKeyJoinColumns.class,
			MapKeyJoinColumnsJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<MapKeyJoinColumn,MapKeyJoinColumnJpaAnnotation> MAP_KEY_JOIN_COLUMN = new CompleteAnnotationDescriptor<>(
			MapKeyJoinColumn.class,
			MapKeyJoinColumnJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false,
			MAP_KEY_JOIN_COLUMNS
	);

	CompleteAnnotationDescriptor<MapKeyTemporal,MapKeyTemporalJpaAnnotation> MAP_KEY_TEMPORAL = new CompleteAnnotationDescriptor<>(
			MapKeyTemporal.class,
			MapKeyTemporalJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<MappedSuperclass,MappedSuperclassJpaAnnotation> MAPPED_SUPERCLASS = new CompleteAnnotationDescriptor<>(
			MappedSuperclass.class,
			MappedSuperclassJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	CompleteAnnotationDescriptor<MapsId,MapsIdJpaAnnotation> MAPS_ID = new CompleteAnnotationDescriptor<>(
			MapsId.class,
			MapsIdJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<NamedAttributeNode,NamedAttributeNodeJpaAnnotation> NAMED_ATTRIBUTE_NODE = new CompleteAnnotationDescriptor<>(
			NamedAttributeNode.class,
			NamedAttributeNodeJpaAnnotation.class,
			EnumSet.noneOf( Kind.class ),
			false
	);

	CompleteAnnotationDescriptor<NamedEntityGraphs,NamedEntityGraphsJpaAnnotation> NAMED_ENTITY_GRAPHS = new CompleteAnnotationDescriptor<>(
			NamedEntityGraphs.class,
			NamedEntityGraphsJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	CompleteAnnotationDescriptor<NamedEntityGraph,NamedEntityGraphJpaAnnotation> NAMED_ENTITY_GRAPH = new CompleteAnnotationDescriptor<>(
			NamedEntityGraph.class,
			NamedEntityGraphJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false,
			NAMED_ENTITY_GRAPHS
	);

	CompleteAnnotationDescriptor<NamedNativeQueries,NamedNativeQueriesJpaAnnotation> NAMED_NATIVE_QUERIES = new CompleteAnnotationDescriptor<>(
			NamedNativeQueries.class,
			NamedNativeQueriesJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false
	);

	CompleteAnnotationDescriptor<NamedNativeQuery,NamedNativeQueryJpaAnnotation> NAMED_NATIVE_QUERY = new CompleteAnnotationDescriptor<>(
			NamedNativeQuery.class,
			NamedNativeQueryJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false,
			NAMED_NATIVE_QUERIES
	);

	CompleteAnnotationDescriptor<NamedQueries,NamedQueriesJpaAnnotation> NAMED_QUERIES = new CompleteAnnotationDescriptor<>(
			NamedQueries.class,
			NamedQueriesJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false
	);

	CompleteAnnotationDescriptor<NamedQuery,NamedQueryJpaAnnotation> NAMED_QUERY = new CompleteAnnotationDescriptor<>(
			NamedQuery.class,
			NamedQueryJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false,
			NAMED_QUERIES
	);
	CompleteAnnotationDescriptor<NamedStatements, NamedStatementsJpaAnnotation> NAMED_STATEMENTS = new CompleteAnnotationDescriptor<>(
			NamedStatements.class,
			NamedStatementsJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false
	);
	CompleteAnnotationDescriptor<NamedStatement, NamedStatementJpaAnnotation> NAMED_STATEMENT = new CompleteAnnotationDescriptor<>(
			NamedStatement.class,
			NamedStatementJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false,
			NAMED_STATEMENTS
	);
	CompleteAnnotationDescriptor<NamedNativeStatements, NamedNativeStatementsJpaAnnotation> NAMED_NATIVE_STATEMENTS = new CompleteAnnotationDescriptor<>(
			NamedNativeStatements.class,
			NamedNativeStatementsJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false
	);
	CompleteAnnotationDescriptor<NamedNativeStatement, NamedNativeStatementJpaAnnotation> NAMED_NATIVE_STATEMENT = new CompleteAnnotationDescriptor<>(
			NamedNativeStatement.class,
			NamedNativeStatementJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false,
			NAMED_NATIVE_STATEMENTS
	);
	CompleteAnnotationDescriptor<NamedStoredProcedureQueries,NamedStoredProcedureQueriesJpaAnnotation> NAMED_STORED_PROCEDURE_QUERIES = new CompleteAnnotationDescriptor<>(
			NamedStoredProcedureQueries.class,
			NamedStoredProcedureQueriesJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false
	);

	CompleteAnnotationDescriptor<NamedStoredProcedureQuery,NamedStoredProcedureQueryJpaAnnotation> NAMED_STORED_PROCEDURE_QUERY = new CompleteAnnotationDescriptor<>(
			NamedStoredProcedureQuery.class,
			NamedStoredProcedureQueryJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false,
			NAMED_STORED_PROCEDURE_QUERIES
	);

	CompleteAnnotationDescriptor<NamedSubgraph,NamedSubgraphJpaAnnotation> NAMED_SUBGRAPH = new CompleteAnnotationDescriptor<>(
			NamedSubgraph.class,
			NamedSubgraphJpaAnnotation.class,
			EnumSet.noneOf( Kind.class ),
			false
	);

	CompleteAnnotationDescriptor<OneToMany,OneToManyJpaAnnotation> ONE_TO_MANY = new CompleteAnnotationDescriptor<>(
			OneToMany.class,
			OneToManyJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<OneToOne,OneToOneJpaAnnotation> ONE_TO_ONE = new CompleteAnnotationDescriptor<>(
			OneToOne.class,
			OneToOneJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<OrderBy,OrderByJpaAnnotation> ORDER_BY = new CompleteAnnotationDescriptor<>(
			OrderBy.class,
			OrderByJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<OrderColumn,OrderColumnJpaAnnotation> ORDER_COLUMN = new CompleteAnnotationDescriptor<>(
			OrderColumn.class,
			OrderColumnJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<PostLoad,PostLoadJpaAnnotation> POST_LOAD = new CompleteAnnotationDescriptor<>(
			PostLoad.class,
			PostLoadJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	CompleteAnnotationDescriptor<PostPersist,PostPersistJpaAnnotation> POST_PERSIST = new CompleteAnnotationDescriptor<>(
			PostPersist.class,
			PostPersistJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	CompleteAnnotationDescriptor<PostRemove,PostRemoveJpaAnnotation> POST_REMOVE = new CompleteAnnotationDescriptor<>(
			PostRemove.class,
			PostRemoveJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	CompleteAnnotationDescriptor<PostUpdate,PostUpdateJpaAnnotation> POST_UPDATE = new CompleteAnnotationDescriptor<>(
			PostUpdate.class,
			PostUpdateJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	CompleteAnnotationDescriptor<PrePersist,PrePersistJpaAnnotation> PRE_PERSIST = new CompleteAnnotationDescriptor<>(
			PrePersist.class,
			PrePersistJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	CompleteAnnotationDescriptor<PreRemove,PreRemoveJpaAnnotation> PRE_REMOVE = new CompleteAnnotationDescriptor<>(
			PreRemove.class,
			PreRemoveJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	CompleteAnnotationDescriptor<PreUpdate,PreUpdateJpaAnnotation> PRE_UPDATE = new CompleteAnnotationDescriptor<>(
			PreUpdate.class,
			PreUpdateJpaAnnotation.class,
			EnumSet.of( Kind.METHOD ),
			false
	);

	CompleteAnnotationDescriptor<PrimaryKeyJoinColumns,PrimaryKeyJoinColumnsJpaAnnotation> PRIMARY_KEY_JOIN_COLUMNS = new CompleteAnnotationDescriptor<>(
			PrimaryKeyJoinColumns.class,
			PrimaryKeyJoinColumnsJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<PrimaryKeyJoinColumn,PrimaryKeyJoinColumnJpaAnnotation> PRIMARY_KEY_JOIN_COLUMN = new CompleteAnnotationDescriptor<>(
			PrimaryKeyJoinColumn.class,
			PrimaryKeyJoinColumnJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD ),
			false,
			PRIMARY_KEY_JOIN_COLUMNS
	);

	CompleteAnnotationDescriptor<QueryHint,QueryHintJpaAnnotation> QUERY_HINT = new CompleteAnnotationDescriptor<>(
			QueryHint.class,
			QueryHintJpaAnnotation.class,
			EnumSet.noneOf( Kind.class ),
			false
	);

	CompleteAnnotationDescriptor<SecondaryTables,SecondaryTablesJpaAnnotation> SECONDARY_TABLES = new CompleteAnnotationDescriptor<>(
			SecondaryTables.class,
			SecondaryTablesJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	CompleteAnnotationDescriptor<SecondaryTable,SecondaryTableJpaAnnotation> SECONDARY_TABLE = new CompleteAnnotationDescriptor<>(
			SecondaryTable.class,
			SecondaryTableJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false,
			SECONDARY_TABLES
	);

	CompleteAnnotationDescriptor<SequenceGenerators,SequenceGeneratorsJpaAnnotation> SEQUENCE_GENERATORS = new CompleteAnnotationDescriptor<>(
			SequenceGenerators.class,
			SequenceGeneratorsJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD, Kind.PACKAGE ),
			false
	);

	CompleteAnnotationDescriptor<SequenceGenerator,SequenceGeneratorJpaAnnotation> SEQUENCE_GENERATOR = new CompleteAnnotationDescriptor<>(
			SequenceGenerator.class,
			SequenceGeneratorJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD, Kind.PACKAGE ),
			false,
			SEQUENCE_GENERATORS
	);

	CompleteAnnotationDescriptor<SqlResultSetMappings,SqlResultSetMappingsJpaAnnotation> SQL_RESULT_SET_MAPPINGS = new CompleteAnnotationDescriptor<>(
			SqlResultSetMappings.class,
			SqlResultSetMappingsJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	CompleteAnnotationDescriptor<SqlResultSetMapping,SqlResultSetMappingJpaAnnotation> SQL_RESULT_SET_MAPPING = new CompleteAnnotationDescriptor<>(
			SqlResultSetMapping.class,
			SqlResultSetMappingJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD ),
			false,
			SQL_RESULT_SET_MAPPINGS
	);

	CompleteAnnotationDescriptor<StoredProcedureParameter,StoredProcedureParameterJpaAnnotation> STORED_PROCEDURE_PARAMETER = new CompleteAnnotationDescriptor<>(
			StoredProcedureParameter.class,
			StoredProcedureParameterJpaAnnotation.class,
			EnumSet.noneOf( Kind.class ),
			false
	);

	CompleteAnnotationDescriptor<Table,TableJpaAnnotation> TABLE = new CompleteAnnotationDescriptor<>(
			Table.class,
			TableJpaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	CompleteAnnotationDescriptor<TableGenerators,TableGeneratorsJpaAnnotation> TABLE_GENERATORS = new CompleteAnnotationDescriptor<>(
			TableGenerators.class,
			TableGeneratorsJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD, Kind.PACKAGE ),
			false
	);

	CompleteAnnotationDescriptor<TableGenerator,TableGeneratorJpaAnnotation> TABLE_GENERATOR = new CompleteAnnotationDescriptor<>(
			TableGenerator.class,
			TableGeneratorJpaAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.METHOD, Kind.FIELD, Kind.PACKAGE ),
			false,
			TABLE_GENERATORS
	);

	CompleteAnnotationDescriptor<Temporal,TemporalJpaAnnotation> TEMPORAL = new CompleteAnnotationDescriptor<>(
			Temporal.class,
			TemporalJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<Transient,TransientJpaAnnotation> TRANSIENT = new CompleteAnnotationDescriptor<>(
			Transient.class,
			TransientJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	CompleteAnnotationDescriptor<UniqueConstraint,UniqueConstraintJpaAnnotation> UNIQUE_CONSTRAINT = new CompleteAnnotationDescriptor<>(
			UniqueConstraint.class,
			UniqueConstraintJpaAnnotation.class,
			EnumSet.noneOf( Kind.class ),
			false
	);

	CompleteAnnotationDescriptor<Version,VersionJpaAnnotation> VERSION = new CompleteAnnotationDescriptor<>(
			Version.class,
			VersionJpaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);

	static void forEachAnnotation(Consumer<AnnotationDescriptor<? extends Annotation>> consumer) {
		OrmAnnotationHelper.forEachOrmAnnotation( JpaAnnotations.class, consumer );
	}
}
