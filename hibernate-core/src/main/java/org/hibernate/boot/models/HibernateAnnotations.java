/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models;

import java.lang.annotation.Annotation;
import java.util.function.Consumer;

import org.hibernate.annotations.*;
import org.hibernate.boot.internal.AnyKeyType;
import org.hibernate.boot.internal.CollectionClassification;
import org.hibernate.boot.models.annotations.internal.*;
import org.hibernate.boot.models.internal.OrmAnnotationHelper;
import org.hibernate.models.internal.OrmAnnotationDescriptor;
import org.hibernate.models.spi.AnnotationDescriptor;

/**
 * Details about Hibernate annotations.
 *
 * @apiNote Here we only collect "stateless" annotations - namely those where we do not care about
 * meta-annotations, which is the vast majority.
 *
 * @implNote Suppressed for deprecation and removal because we refer to many deprecated annotations; suppressed
 * for unused because
 *
 * @author Steve Ebersole
 */
@SuppressWarnings({ "deprecation", "removal", "unused" })
public interface HibernateAnnotations {
	OrmAnnotationDescriptor<Any,AnyAnnotation> ANY = new OrmAnnotationDescriptor<>(
			Any.class,
			AnyAnnotation.class
	);
	OrmAnnotationDescriptor<AnyDiscriminator,AnyDiscriminatorAnnotation> ANY_DISCRIMINATOR = new OrmAnnotationDescriptor<>(
			AnyDiscriminator.class,
			AnyDiscriminatorAnnotation.class
	);
	OrmAnnotationDescriptor<AnyDiscriminatorImplicitValues,AnyDiscriminatorImplicitValuesAnnotation> ANY_DISCRIMINATOR_IMPLICIT_VALUES = new OrmAnnotationDescriptor<>(
			AnyDiscriminatorImplicitValues.class,
			AnyDiscriminatorImplicitValuesAnnotation.class
	);
	OrmAnnotationDescriptor<AnyDiscriminatorValues,AnyDiscriminatorValuesAnnotation> ANY_DISCRIMINATOR_VALUES = new OrmAnnotationDescriptor<>(
			AnyDiscriminatorValues.class,
			AnyDiscriminatorValuesAnnotation.class
	);
	OrmAnnotationDescriptor<AnyDiscriminatorValue,AnyDiscriminatorValueAnnotation> ANY_DISCRIMINATOR_VALUE = new OrmAnnotationDescriptor<>(
			AnyDiscriminatorValue.class,
			AnyDiscriminatorValueAnnotation.class,
			ANY_DISCRIMINATOR_VALUES
	);
	OrmAnnotationDescriptor<AnyKeyJavaClass,AnyKeyJavaClassAnnotation> ANY_KEY_JAVA_CLASS = new OrmAnnotationDescriptor<>(
			AnyKeyJavaClass.class,
			AnyKeyJavaClassAnnotation.class
	);
	OrmAnnotationDescriptor<AnyKeyJavaType,AnyKeyJavaTypeAnnotation> ANY_KEY_JAVA_TYPE = new OrmAnnotationDescriptor<>(
			AnyKeyJavaType.class,
			AnyKeyJavaTypeAnnotation.class
	);
	OrmAnnotationDescriptor<AnyKeyJdbcType,AnyKeyJdbcTypeAnnotation> ANY_KEY_JDBC_TYPE = new OrmAnnotationDescriptor<>(
			AnyKeyJdbcType.class,
			AnyKeyJdbcTypeAnnotation.class
	);
	OrmAnnotationDescriptor<AnyKeyJdbcTypeCode,AnyKeyJdbcTypeCodeAnnotation> ANY_KEY_JDBC_TYPE_CODE = new OrmAnnotationDescriptor<>(
			AnyKeyJdbcTypeCode.class,
			AnyKeyJdbcTypeCodeAnnotation.class
	);
	OrmAnnotationDescriptor<AnyKeyType, AnyKeTypeAnnotation> ANY_KEY_TYPE = new OrmAnnotationDescriptor<>(
			AnyKeyType.class,
			AnyKeTypeAnnotation.class
	);
	OrmAnnotationDescriptor<Array,ArrayAnnotation> ARRAY = new OrmAnnotationDescriptor<>(
			Array.class,
			ArrayAnnotation.class
	);
	SpecializedAnnotationDescriptor<AttributeAccessor,AttributeAccessorAnnotation> ATTRIBUTE_ACCESSOR = new SpecializedAnnotationDescriptor<>(
			AttributeAccessor.class,
			AttributeAccessorAnnotation.class
	);
	OrmAnnotationDescriptor<AttributeBinderType,AttributeBinderTypeAnnotation> ATTRIBUTE_BINDER_TYPE = new OrmAnnotationDescriptor<>(
			AttributeBinderType.class,
			AttributeBinderTypeAnnotation.class
	);
	OrmAnnotationDescriptor<Bag,BagAnnotation> BAG = new OrmAnnotationDescriptor<>(
			Bag.class,
			BagAnnotation.class
	);
	SpecializedAnnotationDescriptor<BatchSize,BatchSizeAnnotation> BATCH_SIZE = new SpecializedAnnotationDescriptor<>(
			BatchSize.class,
			BatchSizeAnnotation.class
	);
	OrmAnnotationDescriptor<Cache,CacheAnnotation> CACHE = new OrmAnnotationDescriptor<>(
			Cache.class,
			CacheAnnotation.class
	);
	OrmAnnotationDescriptor<Cascade,CascadeAnnotation> CASCADE = new OrmAnnotationDescriptor<>(
			Cascade.class,
			CascadeAnnotation.class
	);
	OrmAnnotationDescriptor<Checks,ChecksAnnotation> CHECKS = new OrmAnnotationDescriptor<>(
			Checks.class,
			ChecksAnnotation.class
	);
	OrmAnnotationDescriptor<Check,CheckAnnotation> CHECK = new OrmAnnotationDescriptor<>(
			Check.class,
			CheckAnnotation.class,
			CHECKS
	);
	SpecializedAnnotationDescriptor<Collate,CollateAnnotation> COLLATE = new SpecializedAnnotationDescriptor<>(
			Collate.class,
			CollateAnnotation.class
	);
	OrmAnnotationDescriptor<CollectionId,CollectionIdAnnotation> COLLECTION_ID = new OrmAnnotationDescriptor<>(
			CollectionId.class,
			CollectionIdAnnotation.class
	);
	OrmAnnotationDescriptor<CollectionIdJavaClass,CollectionIdJavaClassAnnotation> COLLECTION_ID_JAVA_CLASS = new OrmAnnotationDescriptor<>(
			CollectionIdJavaClass.class,
			CollectionIdJavaClassAnnotation.class
	);
	OrmAnnotationDescriptor<CollectionIdJavaType,CollectionIdJavaTypeAnnotation> COLLECTION_ID_JAVA_TYPE = new OrmAnnotationDescriptor<>(
			CollectionIdJavaType.class,
			CollectionIdJavaTypeAnnotation.class
	);
	OrmAnnotationDescriptor<CollectionIdJdbcType,CollectionIdJdbcTypeAnnotation> COLLECTION_ID_JDBC_TYPE = new OrmAnnotationDescriptor<>(
			CollectionIdJdbcType.class,
			CollectionIdJdbcTypeAnnotation.class
	);
	OrmAnnotationDescriptor<CollectionIdJdbcTypeCode,CollectionIdJdbcTypeCodeAnnotation> COLLECTION_ID_JDBC_TYPE_CODE = new OrmAnnotationDescriptor<>(
			CollectionIdJdbcTypeCode.class,
			CollectionIdJdbcTypeCodeAnnotation.class
	);
	OrmAnnotationDescriptor<CollectionIdMutability,CollectionIdMutabilityAnnotation> COLLECTION_ID_MUTABILITY = new OrmAnnotationDescriptor<>(
			CollectionIdMutability.class,
			CollectionIdMutabilityAnnotation.class
	);
	OrmAnnotationDescriptor<CollectionIdType,CollectionIdTypeAnnotation> COLLECTION_ID_TYPE = new OrmAnnotationDescriptor<>(
			CollectionIdType.class,
			CollectionIdTypeAnnotation.class
	);
	OrmAnnotationDescriptor<CollectionType,CollectionTypeAnnotation> COLLECTION_TYPE = new OrmAnnotationDescriptor<>(
			CollectionType.class,
			CollectionTypeAnnotation.class
	);
	OrmAnnotationDescriptor<CollectionClassification, CollectionClassificationXmlAnnotation> COLLECTION_CLASSIFICATION = new OrmAnnotationDescriptor<>(
			CollectionClassification.class,
			CollectionClassificationXmlAnnotation.class
	);
	OrmAnnotationDescriptor<CollectionTypeRegistrations,CollectionTypeRegistrationsAnnotation> COLLECTION_TYPE_REGISTRATIONS = new OrmAnnotationDescriptor<>(
			CollectionTypeRegistrations.class,
			CollectionTypeRegistrationsAnnotation.class
	);
	OrmAnnotationDescriptor<CollectionTypeRegistration,CollectionTypeRegistrationAnnotation> COLLECTION_TYPE_REGISTRATION = new OrmAnnotationDescriptor<>(
			CollectionTypeRegistration.class,
			CollectionTypeRegistrationAnnotation.class,
			COLLECTION_TYPE_REGISTRATIONS
	);
	OrmAnnotationDescriptor<ColumnDefault,ColumnDefaultAnnotation> COLUMN_DEFAULT = new OrmAnnotationDescriptor<>(
			ColumnDefault.class,
			ColumnDefaultAnnotation.class
	);
	OrmAnnotationDescriptor<Columns,ColumnsAnnotation> COLUMNS = new OrmAnnotationDescriptor<>(
			Columns.class,
			ColumnsAnnotation.class
	);
	OrmAnnotationDescriptor<ColumnTransformers,ColumnTransformersAnnotation> COLUMN_TRANSFORMERS = new OrmAnnotationDescriptor<>(
			ColumnTransformers.class,
			ColumnTransformersAnnotation.class
	);
	OrmAnnotationDescriptor<ColumnTransformer,ColumnTransformerAnnotation> COLUMN_TRANSFORMER = new OrmAnnotationDescriptor<>(
			ColumnTransformer.class,
			ColumnTransformerAnnotation.class,
			COLUMN_TRANSFORMERS
	);
	SpecializedAnnotationDescriptor<Comments,CommentsAnnotation> COMMENTS = new SpecializedAnnotationDescriptor<>(
			Comments.class,
			CommentsAnnotation.class
	);
	SpecializedAnnotationDescriptor<Comment,CommentAnnotation> COMMENT = new SpecializedAnnotationDescriptor<>(
			Comment.class,
			CommentAnnotation.class,
			COMMENTS
	);
	OrmAnnotationDescriptor<CompositeType,CompositeTypeAnnotation> COMPOSITE_TYPE = new OrmAnnotationDescriptor<>(
			CompositeType.class,
			CompositeTypeAnnotation.class
	);
	OrmAnnotationDescriptor<CompositeTypeRegistrations,CompositeTypeRegistrationsAnnotation> COMPOSITE_TYPE_REGISTRATIONS = new OrmAnnotationDescriptor<>(
			CompositeTypeRegistrations.class,
			CompositeTypeRegistrationsAnnotation.class
	);
	OrmAnnotationDescriptor<CompositeTypeRegistration,CompositeTypeRegistrationAnnotation> COMPOSITE_TYPE_REGISTRATION = new OrmAnnotationDescriptor<>(
			CompositeTypeRegistration.class,
			CompositeTypeRegistrationAnnotation.class,
			COMPOSITE_TYPE_REGISTRATIONS
	);
	OrmAnnotationDescriptor<ConcreteProxy,ConcreteProxyAnnotation> CONCRETE_PROXY = new OrmAnnotationDescriptor<>(
			ConcreteProxy.class,
			ConcreteProxyAnnotation.class
	);
	OrmAnnotationDescriptor<ConverterRegistrations,ConverterRegistrationsAnnotation> CONVERTER_REGISTRATIONS = new OrmAnnotationDescriptor<>(
			ConverterRegistrations.class,
			ConverterRegistrationsAnnotation.class
	);
	OrmAnnotationDescriptor<ConverterRegistration,ConverterRegistrationAnnotation> CONVERTER_REGISTRATION = new OrmAnnotationDescriptor<>(
			ConverterRegistration.class,
			ConverterRegistrationAnnotation.class,
			CONVERTER_REGISTRATIONS
	);
	OrmAnnotationDescriptor<CreationTimestamp,CreationTimestampAnnotation> CREATION_TIMESTAMP = new OrmAnnotationDescriptor<>(
			CreationTimestamp.class,
			CreationTimestampAnnotation.class
	);
	OrmAnnotationDescriptor<CurrentTimestamp,CurrentTimestampAnnotation> CURRENT_TIMESTAMP = new OrmAnnotationDescriptor<>(
			CurrentTimestamp.class,
			CurrentTimestampAnnotation.class
	);
	OrmAnnotationDescriptor<DiscriminatorFormula,DiscriminatorFormulaAnnotation> DISCRIMINATOR_FORMULA = new OrmAnnotationDescriptor<>(
			DiscriminatorFormula.class,
			DiscriminatorFormulaAnnotation.class
	);
	SpecializedAnnotationDescriptor<DiscriminatorOptions,DiscriminatorOptionsAnnotation> DISCRIMINATOR_OPTIONS = new SpecializedAnnotationDescriptor<>(
			DiscriminatorOptions.class,
			DiscriminatorOptionsAnnotation.class
	);
	OrmAnnotationDescriptor<DynamicInsert,DynamicInsertAnnotation> DYNAMIC_INSERT = new OrmAnnotationDescriptor<>(
			DynamicInsert.class,
			DynamicInsertAnnotation.class
	);
	OrmAnnotationDescriptor<DynamicUpdate,DynamicUpdateAnnotation> DYNAMIC_UPDATE = new OrmAnnotationDescriptor<>(
			DynamicUpdate.class,
			DynamicUpdateAnnotation.class
	);
	OrmAnnotationDescriptor<EmbeddableInstantiator,EmbeddableInstantiatorAnnotation> EMBEDDABLE_INSTANTIATOR = new OrmAnnotationDescriptor<>(
			EmbeddableInstantiator.class,
			EmbeddableInstantiatorAnnotation.class
	);
	OrmAnnotationDescriptor<EmbeddableInstantiatorRegistrations,EmbeddableInstantiatorRegistrationsAnnotation> EMBEDDABLE_INSTANTIATOR_REGISTRATIONS = new OrmAnnotationDescriptor<>(
			EmbeddableInstantiatorRegistrations.class,
			EmbeddableInstantiatorRegistrationsAnnotation.class
	);
	OrmAnnotationDescriptor<EmbeddableInstantiatorRegistration,EmbeddableInstantiatorRegistrationAnnotation> EMBEDDABLE_INSTANTIATOR_REGISTRATION = new OrmAnnotationDescriptor<>(
			EmbeddableInstantiatorRegistration.class,
			EmbeddableInstantiatorRegistrationAnnotation.class,
			EMBEDDABLE_INSTANTIATOR_REGISTRATIONS
	);
	OrmAnnotationDescriptor<EmbeddedColumnNaming,EmbeddedColumnNamingAnnotation> EMBEDDED_COLUMN_NAMING = new OrmAnnotationDescriptor<>(
			EmbeddedColumnNaming.class,
			EmbeddedColumnNamingAnnotation.class
	);
	OrmAnnotationDescriptor<EmbeddedTable,EmbeddedTableAnnotation> EMBEDDED_TABLE = new OrmAnnotationDescriptor<>(
			EmbeddedTable.class,
			EmbeddedTableAnnotation.class
	);
	OrmAnnotationDescriptor<Fetch,FetchAnnotation> FETCH = new OrmAnnotationDescriptor<>(
			Fetch.class,
			FetchAnnotation.class
	);
	OrmAnnotationDescriptor<FetchProfiles,FetchProfilesAnnotation> FETCH_PROFILES = new OrmAnnotationDescriptor<>(
			FetchProfiles.class,
			FetchProfilesAnnotation.class
	);
	OrmAnnotationDescriptor<FetchProfile,FetchProfileAnnotation> FETCH_PROFILE = new OrmAnnotationDescriptor<>(
			FetchProfile.class,
			FetchProfileAnnotation.class,
			FETCH_PROFILES
	);
	OrmAnnotationDescriptor<Filters,FiltersAnnotation> FILTERS = new OrmAnnotationDescriptor<>(
			Filters.class,
			FiltersAnnotation.class
	);
	OrmAnnotationDescriptor<Filter,FilterAnnotation> FILTER = new OrmAnnotationDescriptor<>(
			Filter.class,
			FilterAnnotation.class,
			FILTERS
	);
	OrmAnnotationDescriptor<FilterDefs,FilterDefsAnnotation> FILTER_DEFS = new OrmAnnotationDescriptor<>(
			FilterDefs.class,
			FilterDefsAnnotation.class
	);
	OrmAnnotationDescriptor<FilterDef,FilterDefAnnotation> FILTER_DEF = new OrmAnnotationDescriptor<>(
			FilterDef.class,
			FilterDefAnnotation.class,
			FILTER_DEFS
	);
	OrmAnnotationDescriptor<FilterJoinTables,FilterJoinTablesAnnotation> FILTER_JOIN_TABLES = new OrmAnnotationDescriptor<>(
			FilterJoinTables.class,
			FilterJoinTablesAnnotation.class
	);
	OrmAnnotationDescriptor<FilterJoinTable,FilterJoinTableAnnotation> FILTER_JOIN_TABLE = new OrmAnnotationDescriptor<>(
			FilterJoinTable.class,
			FilterJoinTableAnnotation.class,
			FILTER_JOIN_TABLES
	);
	OrmAnnotationDescriptor<Formula,FormulaAnnotation> FORMULA = new OrmAnnotationDescriptor<>(
			Formula.class,
			FormulaAnnotation.class
	);
	OrmAnnotationDescriptor<FractionalSeconds,FractionalSecondsAnnotation> FRACTIONAL_SECONDS = new OrmAnnotationDescriptor<>(
			FractionalSeconds.class,
			FractionalSecondsAnnotation.class
	);
	OrmAnnotationDescriptor<Generated,GeneratedAnnotation> GENERATED = new OrmAnnotationDescriptor<>(
			Generated.class,
			GeneratedAnnotation.class
	);
	OrmAnnotationDescriptor<GeneratedColumn,GeneratedColumnAnnotation> GENERATED_COLUMN = new OrmAnnotationDescriptor<>(
			GeneratedColumn.class,
			GeneratedColumnAnnotation.class
	);
	OrmAnnotationDescriptor<GenericGenerators,GenericGeneratorsAnnotation> GENERIC_GENERATORS = new OrmAnnotationDescriptor<>(
			GenericGenerators.class,
			GenericGeneratorsAnnotation.class
	);
	OrmAnnotationDescriptor<GenericGenerator,GenericGeneratorAnnotation> GENERIC_GENERATOR = new OrmAnnotationDescriptor<>(
			GenericGenerator.class,
			GenericGeneratorAnnotation.class,
			GENERIC_GENERATORS
	);
	OrmAnnotationDescriptor<HQLSelect,HQLSelectAnnotation> HQL_SELECT = new OrmAnnotationDescriptor<>(
			HQLSelect.class,
			HQLSelectAnnotation.class
	);
	OrmAnnotationDescriptor<IdGeneratorType,IdGeneratorTypeAnnotation> ID_GENERATOR_TYPE = new OrmAnnotationDescriptor<>(
			IdGeneratorType.class,
			IdGeneratorTypeAnnotation.class
	);
	OrmAnnotationDescriptor<Immutable,ImmutableAnnotation> IMMUTABLE = new OrmAnnotationDescriptor<>(
			Immutable.class,
			ImmutableAnnotation.class
	);
	OrmAnnotationDescriptor<Imported,ImportedAnnotation> IMPORTED = new OrmAnnotationDescriptor<>(
			Imported.class,
			ImportedAnnotation.class
	);
	OrmAnnotationDescriptor<Instantiator,InstantiatorAnnotation> INSTANTIATOR = new OrmAnnotationDescriptor<>(
			Instantiator.class,
			InstantiatorAnnotation.class
	);
	OrmAnnotationDescriptor<JavaType,JavaTypeAnnotation> JAVA_TYPE = new OrmAnnotationDescriptor<>(
			JavaType.class,
			JavaTypeAnnotation.class
	);
	OrmAnnotationDescriptor<JavaTypeRegistrations,JavaTypeRegistrationsAnnotation> JAVA_TYPE_REGISTRATIONS = new OrmAnnotationDescriptor<>(
			JavaTypeRegistrations.class,
			JavaTypeRegistrationsAnnotation.class
	);
	OrmAnnotationDescriptor<JavaTypeRegistration,JavaTypeRegistrationAnnotation> JAVA_TYPE_REGISTRATION = new OrmAnnotationDescriptor<>(
			JavaTypeRegistration.class,
			JavaTypeRegistrationAnnotation.class,
			JAVA_TYPE_REGISTRATIONS
	);
	OrmAnnotationDescriptor<JdbcType,JdbcTypeAnnotation> JDBC_TYPE = new OrmAnnotationDescriptor<>(
			JdbcType.class,
			JdbcTypeAnnotation.class
	);
	OrmAnnotationDescriptor<JdbcTypeCode,JdbcTypeCodeAnnotation> JDBC_TYPE_CODE = new OrmAnnotationDescriptor<>(
			JdbcTypeCode.class,
			JdbcTypeCodeAnnotation.class
	);
	OrmAnnotationDescriptor<JdbcTypeRegistrations,JdbcTypeRegistrationsAnnotation> JDBC_TYPE_REGISTRATIONS = new OrmAnnotationDescriptor<>(
			JdbcTypeRegistrations.class,
			JdbcTypeRegistrationsAnnotation.class
	);
	OrmAnnotationDescriptor<JdbcTypeRegistration,JdbcTypeRegistrationAnnotation> JDBC_TYPE_REGISTRATION = new OrmAnnotationDescriptor<>(
			JdbcTypeRegistration.class,
			JdbcTypeRegistrationAnnotation.class,
			JDBC_TYPE_REGISTRATIONS
	);
	OrmAnnotationDescriptor<JoinColumnsOrFormulas, JoinColumnsOrFormulasAnnotation> JOIN_COLUMNS_OR_FORMULAS = new OrmAnnotationDescriptor<>(
			JoinColumnsOrFormulas.class,
			JoinColumnsOrFormulasAnnotation.class
	);
	OrmAnnotationDescriptor<JoinColumnOrFormula, JoinColumnOrFormulaAnnotation> JOIN_COLUMN_OR_FORMULA = new OrmAnnotationDescriptor<>(
			JoinColumnOrFormula.class,
			JoinColumnOrFormulaAnnotation.class,
			JOIN_COLUMNS_OR_FORMULAS
	);
	OrmAnnotationDescriptor<JoinFormula, JoinFormulaAnnotation> JOIN_FORMULA = new OrmAnnotationDescriptor<>(
			JoinFormula.class,
			JoinFormulaAnnotation.class
	);
	OrmAnnotationDescriptor<LazyGroup, LazyGroupAnnotation> LAZY_GROUP = new OrmAnnotationDescriptor<>(
			LazyGroup.class,
			LazyGroupAnnotation.class
	);
	OrmAnnotationDescriptor<ListIndexBase, ListIndexBaseAnnotation> LIST_INDEX_BASE = new OrmAnnotationDescriptor<>(
			ListIndexBase.class,
			ListIndexBaseAnnotation.class
	);
	OrmAnnotationDescriptor<ListIndexJavaType, ListIndexJavaTypeAnnotation> LIST_INDEX_JAVA_TYPE = new OrmAnnotationDescriptor<>(
			ListIndexJavaType.class,
			ListIndexJavaTypeAnnotation.class
	);
	OrmAnnotationDescriptor<ListIndexJdbcType, ListIndexJdbcTypeAnnotation> LIST_INDEX_JDBC_TYPE = new OrmAnnotationDescriptor<>(
			ListIndexJdbcType.class,
			ListIndexJdbcTypeAnnotation.class
	);
	OrmAnnotationDescriptor<ListIndexJdbcTypeCode, ListIndexJdbcTypeCodeAnnotation> LIST_INDEX_JDBC_TYPE_CODE = new OrmAnnotationDescriptor<>(
			ListIndexJdbcTypeCode.class,
			ListIndexJdbcTypeCodeAnnotation.class
	);
	OrmAnnotationDescriptor<ManyToAny, ManyToAnyAnnotation> MANY_TO_ANY = new OrmAnnotationDescriptor<>(
			ManyToAny.class,
			ManyToAnyAnnotation.class
	);
	OrmAnnotationDescriptor<MapKeyCompositeType, MapKeyCompositeTypeAnnotation> MAP_KEY_COMPOSITE_TYPE = new OrmAnnotationDescriptor<>(
			MapKeyCompositeType.class,
			MapKeyCompositeTypeAnnotation.class
	);
	OrmAnnotationDescriptor<MapKeyJavaType, MapKeyJavaTypeAnnotation> MAP_KEY_JAVA_TYPE = new OrmAnnotationDescriptor<>(
			MapKeyJavaType.class,
			MapKeyJavaTypeAnnotation.class
	);
	OrmAnnotationDescriptor<MapKeyJdbcType, MapKeyJdbcTypeAnnotation> MAP_KEY_JDBC_TYPE = new OrmAnnotationDescriptor<>(
			MapKeyJdbcType.class,
			MapKeyJdbcTypeAnnotation.class
	);
	OrmAnnotationDescriptor<MapKeyJdbcTypeCode, MapKeyJdbcTypeCodeAnnotation> MAP_KEY_JDBC_TYPE_CODE = new OrmAnnotationDescriptor<>(
			MapKeyJdbcTypeCode.class,
			MapKeyJdbcTypeCodeAnnotation.class
	);
	OrmAnnotationDescriptor<MapKeyMutability, MapKeyMutabilityAnnotation> MAP_KEY_MUTABILITY = new OrmAnnotationDescriptor<>(
			MapKeyMutability.class,
			MapKeyMutabilityAnnotation.class
	);
	OrmAnnotationDescriptor<MapKeyType, MapKeyTypeAnnotation> MAP_KEY_TYPE = new OrmAnnotationDescriptor<>(
			MapKeyType.class,
			MapKeyTypeAnnotation.class
	);
	OrmAnnotationDescriptor<Mutability, MutabilityAnnotation> MUTABILITY = new OrmAnnotationDescriptor<>(
			Mutability.class,
			MutabilityAnnotation.class
	);
	OrmAnnotationDescriptor<NamedEntityGraphs, NamedEntityGraphsAnnotation> NAMED_ENTITY_GRAPHS = new OrmAnnotationDescriptor<>(
			NamedEntityGraphs.class,
			NamedEntityGraphsAnnotation.class
	);
	OrmAnnotationDescriptor<NamedEntityGraph, NamedEntityGraphAnnotation> NAMED_ENTITY_GRAPH = new OrmAnnotationDescriptor<>(
			NamedEntityGraph.class,
			NamedEntityGraphAnnotation.class,
			NAMED_ENTITY_GRAPHS
	);
	OrmAnnotationDescriptor<NamedNativeQueries, NamedNativeQueriesAnnotation> NAMED_NATIVE_QUERIES = new OrmAnnotationDescriptor<>(
			NamedNativeQueries.class,
			NamedNativeQueriesAnnotation.class
	);
	OrmAnnotationDescriptor<NamedNativeQuery, NamedNativeQueryAnnotation> NAMED_NATIVE_QUERY = new OrmAnnotationDescriptor<>(
			NamedNativeQuery.class,
			NamedNativeQueryAnnotation.class,
			NAMED_NATIVE_QUERIES
	);
	OrmAnnotationDescriptor<NamedQueries, NamedQueriesAnnotation> NAMED_QUERIES = new OrmAnnotationDescriptor<>(
			NamedQueries.class,
			NamedQueriesAnnotation.class
	);
	OrmAnnotationDescriptor<NamedQuery, NamedQueryAnnotation> NAMED_QUERY = new OrmAnnotationDescriptor<>(
			NamedQuery.class,
			NamedQueryAnnotation.class,
			NAMED_QUERIES
	);
	OrmAnnotationDescriptor<Nationalized, NationalizedAnnotation> NATIONALIZED = new OrmAnnotationDescriptor<>(
			Nationalized.class,
			NationalizedAnnotation.class
	);
	OrmAnnotationDescriptor<NativeGenerator, NativeGeneratorAnnotation> NATIVE_GENERATOR = new OrmAnnotationDescriptor<>(
			NativeGenerator.class,
			NativeGeneratorAnnotation.class
	);
	OrmAnnotationDescriptor<NaturalId, NaturalIdAnnotation> NATURAL_ID = new OrmAnnotationDescriptor<>(
			NaturalId.class,
			NaturalIdAnnotation.class
	);
	OrmAnnotationDescriptor<NaturalIdCache, NaturalIdCacheAnnotation> NATURAL_ID_CACHE = new OrmAnnotationDescriptor<>(
			NaturalIdCache.class,
			NaturalIdCacheAnnotation.class
	);
	OrmAnnotationDescriptor<NotFound, NotFoundAnnotation> NOT_FOUND = new OrmAnnotationDescriptor<>(
			NotFound.class,
			NotFoundAnnotation.class
	);
	OrmAnnotationDescriptor<OnDelete,OnDeleteAnnotation> ON_DELETE = new OrmAnnotationDescriptor<>(
			OnDelete.class,
			OnDeleteAnnotation.class
	);
	OrmAnnotationDescriptor<OptimisticLock,OptimisticLockAnnotation> OPTIMISTIC_LOCK = new OrmAnnotationDescriptor<>(
			OptimisticLock.class,
			OptimisticLockAnnotation.class
	);
	OrmAnnotationDescriptor<OptimisticLocking,OptimisticLockingAnnotation> OPTIMISTIC_LOCKING = new OrmAnnotationDescriptor<>(
			OptimisticLocking.class,
			OptimisticLockingAnnotation.class
	);
	OrmAnnotationDescriptor<ParamDef,ParamDefAnnotation> PARAM_DEF = new OrmAnnotationDescriptor<>(
			ParamDef.class,
			ParamDefAnnotation.class
	);
	OrmAnnotationDescriptor<Parameter,ParameterAnnotation> PARAMETER = new OrmAnnotationDescriptor<>(
			Parameter.class,
			ParameterAnnotation.class
	);
	OrmAnnotationDescriptor<Parent,ParentAnnotation> PARENT = new OrmAnnotationDescriptor<>(
			Parent.class,
			ParentAnnotation.class
	);
	OrmAnnotationDescriptor<PartitionKey,PartitionKeyAnnotation> PARTITION_KEY = new OrmAnnotationDescriptor<>(
			PartitionKey.class,
			PartitionKeyAnnotation.class
	);
	OrmAnnotationDescriptor<PropertyRef,PropertyRefAnnotation> PROPERTY_REF = new OrmAnnotationDescriptor<>(
			PropertyRef.class,
			PropertyRefAnnotation.class
	);
	OrmAnnotationDescriptor<QueryCacheLayout,QueryCacheLayoutAnnotation> QUERY_CACHE_LAYOUT = new OrmAnnotationDescriptor<>(
			QueryCacheLayout.class,
			QueryCacheLayoutAnnotation.class
	);
	OrmAnnotationDescriptor<RowId,RowIdAnnotation> ROW_ID = new OrmAnnotationDescriptor<>(
			RowId.class,
			RowIdAnnotation.class
	);
	OrmAnnotationDescriptor<SecondaryRows,SecondaryRowsAnnotation> SECONDARY_ROWS = new OrmAnnotationDescriptor<>(
			SecondaryRows.class,
			SecondaryRowsAnnotation.class
	);
	OrmAnnotationDescriptor<SecondaryRow,SecondaryRowAnnotation> SECONDARY_ROW = new OrmAnnotationDescriptor<>(
			SecondaryRow.class,
			SecondaryRowAnnotation.class,
			SECONDARY_ROWS
	);
	OrmAnnotationDescriptor<SoftDelete,SoftDeleteAnnotation> SOFT_DELETE = new OrmAnnotationDescriptor<>(
			SoftDelete.class,
			SoftDeleteAnnotation.class
	);
	OrmAnnotationDescriptor<SortComparator,SortComparatorAnnotation> SORT_COMPARATOR = new OrmAnnotationDescriptor<>(
			SortComparator.class,
			SortComparatorAnnotation.class
	);
	OrmAnnotationDescriptor<SortNatural,SortNaturalAnnotation> SORT_NATURAL = new OrmAnnotationDescriptor<>(
			SortNatural.class,
			SortNaturalAnnotation.class
	);
	OrmAnnotationDescriptor<Source,SourceAnnotation> SOURCE = new OrmAnnotationDescriptor<>(
			Source.class,
			SourceAnnotation.class
	);
	OrmAnnotationDescriptor<SQLDeletes,SQLDeletesAnnotation> SQL_DELETES = new OrmAnnotationDescriptor<>(
			SQLDeletes.class,
			SQLDeletesAnnotation.class
	);
	OrmAnnotationDescriptor<SQLDelete,SQLDeleteAnnotation> SQL_DELETE = new OrmAnnotationDescriptor<>(
			SQLDelete.class,
			SQLDeleteAnnotation.class,
			SQL_DELETES
	);
	OrmAnnotationDescriptor<SQLDeleteAll,SQLDeleteAllAnnotation> SQL_DELETE_ALL = new OrmAnnotationDescriptor<>(
			SQLDeleteAll.class,
			SQLDeleteAllAnnotation.class
	);
	OrmAnnotationDescriptor<SqlFragmentAlias,SqlFragmentAliasAnnotation> SQL_FRAGMENT_ALIAS = new OrmAnnotationDescriptor<>(
			SqlFragmentAlias.class,
			SqlFragmentAliasAnnotation.class
	);
	OrmAnnotationDescriptor<SQLInserts,SQLInsertsAnnotation> SQL_INSERTS = new OrmAnnotationDescriptor<>(
			SQLInserts.class,
			SQLInsertsAnnotation.class
	);
	OrmAnnotationDescriptor<SQLInsert,SQLInsertAnnotation> SQL_INSERT = new OrmAnnotationDescriptor<>(
			SQLInsert.class,
			SQLInsertAnnotation.class,
			SQL_INSERTS
	);
	OrmAnnotationDescriptor<SQLOrder,SQLOrderAnnotation> SQL_ORDER = new OrmAnnotationDescriptor<>(
			SQLOrder.class,
			SQLOrderAnnotation.class
	);
	OrmAnnotationDescriptor<SQLRestriction,SQLRestrictionAnnotation> SQL_RESTRICTION = new OrmAnnotationDescriptor<>(
			SQLRestriction.class,
			SQLRestrictionAnnotation.class
	);
	OrmAnnotationDescriptor<SQLSelect,SQLSelectAnnotation> SQL_SELECT = new OrmAnnotationDescriptor<>(
			SQLSelect.class,
			SQLSelectAnnotation.class
	);
	OrmAnnotationDescriptor<SQLJoinTableRestriction,SQLJoinTableRestrictionAnnotation> SQL_JOIN_TABLE_RESTRICTION = new OrmAnnotationDescriptor<>(
			SQLJoinTableRestriction.class,
			SQLJoinTableRestrictionAnnotation.class
	);
	OrmAnnotationDescriptor<SQLUpdates,SQLUpdatesAnnotation> SQL_UPDATES = new OrmAnnotationDescriptor<>(
			SQLUpdates.class,
			SQLUpdatesAnnotation.class
	);
	OrmAnnotationDescriptor<SQLUpdate,SQLUpdateAnnotation> SQL_UPDATE = new OrmAnnotationDescriptor<>(
			SQLUpdate.class,
			SQLUpdateAnnotation.class,
			SQL_UPDATES
	);
	OrmAnnotationDescriptor<Struct,StructAnnotation> STRUCT = new OrmAnnotationDescriptor<>(
			Struct.class,
			StructAnnotation.class
	);
	OrmAnnotationDescriptor<Subselect,SubselectAnnotation> SUBSELECT = new OrmAnnotationDescriptor<>(
			Subselect.class,
			SubselectAnnotation.class
	);
	OrmAnnotationDescriptor<Synchronize,SynchronizeAnnotation> SYNCHRONIZE = new OrmAnnotationDescriptor<>(
			Synchronize.class,
			SynchronizeAnnotation.class
	);
	OrmAnnotationDescriptor<TargetEmbeddable,TargetEmbeddableAnnotation> TARGET_EMBEDDABLE = new OrmAnnotationDescriptor<>(
			TargetEmbeddable.class,
			TargetEmbeddableAnnotation.class
	);
	SpecializedAnnotationDescriptor<TenantId,TenantIdAnnotation> TENANT_ID = new SpecializedAnnotationDescriptor<>(
			TenantId.class,
			TenantIdAnnotation.class
	);
	OrmAnnotationDescriptor<TimeZoneColumn,TimeZoneColumnAnnotation> TIME_ZONE_COLUMN = new OrmAnnotationDescriptor<>(
			TimeZoneColumn.class,
			TimeZoneColumnAnnotation.class
	);
	OrmAnnotationDescriptor<TimeZoneStorage,TimeZoneStorageAnnotation> TIME_ZONE_STORAGE = new OrmAnnotationDescriptor<>(
			TimeZoneStorage.class,
			TimeZoneStorageAnnotation.class
	);
	OrmAnnotationDescriptor<Type,TypeAnnotation> TYPE = new OrmAnnotationDescriptor<>(
			Type.class,
			TypeAnnotation.class
	);
	SpecializedAnnotationDescriptor<TypeBinderType,TypeBinderTypeAnnotation> TYPE_BINDER_TYPE = new SpecializedAnnotationDescriptor<>(
			TypeBinderType.class,
			TypeBinderTypeAnnotation.class
	);
	OrmAnnotationDescriptor<TypeRegistrations,TypeRegistrationsAnnotation> TYPE_REGISTRATIONS = new OrmAnnotationDescriptor<>(
			TypeRegistrations.class,
			TypeRegistrationsAnnotation.class
	);
	OrmAnnotationDescriptor<TypeRegistration,TypeRegistrationAnnotation> TYPE_REGISTRATION = new OrmAnnotationDescriptor<>(
			TypeRegistration.class,
			TypeRegistrationAnnotation.class,
			TYPE_REGISTRATIONS
	);
	OrmAnnotationDescriptor<UpdateTimestamp,UpdateTimestampAnnotation> UPDATE_TIMESTAMP = new OrmAnnotationDescriptor<>(
			UpdateTimestamp.class,
			UpdateTimestampAnnotation.class
	);
	OrmAnnotationDescriptor<UuidGenerator,UuidGeneratorAnnotation> UUID_GENERATOR = new OrmAnnotationDescriptor<>(
			UuidGenerator.class,
			UuidGeneratorAnnotation.class
	);
	OrmAnnotationDescriptor<ValueGenerationType,ValueGenerationTypeAnnotation> VALUE_GENERATION_TYPE = new OrmAnnotationDescriptor<>(
			ValueGenerationType.class,
			ValueGenerationTypeAnnotation.class
	);
	OrmAnnotationDescriptor<View,ViewAnnotation> VIEW = new OrmAnnotationDescriptor<>(
			View.class,
			ViewAnnotation.class
	);

	static void forEachAnnotation(Consumer<AnnotationDescriptor<? extends Annotation>> consumer) {
		OrmAnnotationHelper.forEachOrmAnnotation( HibernateAnnotations.class, consumer );
	}
}
