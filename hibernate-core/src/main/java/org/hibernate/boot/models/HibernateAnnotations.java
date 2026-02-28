/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models;

import java.util.EnumSet;
import java.lang.annotation.Annotation;
import java.util.function.Consumer;

import org.hibernate.annotations.*;
import org.hibernate.boot.internal.AnyKeyType;
import org.hibernate.boot.internal.CollectionClassification;
import org.hibernate.boot.models.annotations.internal.*;
import org.hibernate.boot.models.internal.OrmAnnotationHelper;
import org.hibernate.models.internal.OrmAnnotationDescriptor;
import org.hibernate.models.spi.AnnotationDescriptor;

import static org.hibernate.models.spi.AnnotationTarget.Kind;

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
	OrmAnnotationDescriptor<Any, AnyAnnotation> ANY = new OrmAnnotationDescriptor<>(
			Any.class,
			AnyAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<AnyDiscriminator, AnyDiscriminatorAnnotation> ANY_DISCRIMINATOR = new OrmAnnotationDescriptor<>(
			AnyDiscriminator.class,
			AnyDiscriminatorAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	OrmAnnotationDescriptor<AnyDiscriminatorImplicitValues, AnyDiscriminatorImplicitValuesAnnotation> ANY_DISCRIMINATOR_IMPLICIT_VALUES = new OrmAnnotationDescriptor<>(
			AnyDiscriminatorImplicitValues.class,
			AnyDiscriminatorImplicitValuesAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	OrmAnnotationDescriptor<AnyDiscriminatorValues, AnyDiscriminatorValuesAnnotation> ANY_DISCRIMINATOR_VALUES = new OrmAnnotationDescriptor<>(
			AnyDiscriminatorValues.class,
			AnyDiscriminatorValuesAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	OrmAnnotationDescriptor<AnyDiscriminatorValue, AnyDiscriminatorValueAnnotation> ANY_DISCRIMINATOR_VALUE = new OrmAnnotationDescriptor<>(
			AnyDiscriminatorValue.class,
			AnyDiscriminatorValueAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false,
			ANY_DISCRIMINATOR_VALUES
	);
	OrmAnnotationDescriptor<AnyKeyJavaClass, AnyKeyJavaClassAnnotation> ANY_KEY_JAVA_CLASS = new OrmAnnotationDescriptor<>(
			AnyKeyJavaClass.class,
			AnyKeyJavaClassAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	OrmAnnotationDescriptor<AnyKeyJavaType, AnyKeyJavaTypeAnnotation> ANY_KEY_JAVA_TYPE = new OrmAnnotationDescriptor<>(
			AnyKeyJavaType.class,
			AnyKeyJavaTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	OrmAnnotationDescriptor<AnyKeyJdbcType, AnyKeyJdbcTypeAnnotation> ANY_KEY_JDBC_TYPE = new OrmAnnotationDescriptor<>(
			AnyKeyJdbcType.class,
			AnyKeyJdbcTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	OrmAnnotationDescriptor<AnyKeyJdbcTypeCode, AnyKeyJdbcTypeCodeAnnotation> ANY_KEY_JDBC_TYPE_CODE = new OrmAnnotationDescriptor<>(
			AnyKeyJdbcTypeCode.class,
			AnyKeyJdbcTypeCodeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	OrmAnnotationDescriptor<AnyKeyType, AnyKeTypeAnnotation> ANY_KEY_TYPE = new OrmAnnotationDescriptor<>(
			AnyKeyType.class,
			AnyKeTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<Array, ArrayAnnotation> ARRAY = new OrmAnnotationDescriptor<>(
			Array.class,
			ArrayAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	SpecializedAnnotationDescriptor<AttributeAccessor, AttributeAccessorAnnotation> ATTRIBUTE_ACCESSOR = new SpecializedAnnotationDescriptor<>(
			AttributeAccessor.class,
			AttributeAccessorAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	OrmAnnotationDescriptor<AttributeBinderType, AttributeBinderTypeAnnotation> ATTRIBUTE_BINDER_TYPE = new OrmAnnotationDescriptor<>(
			AttributeBinderType.class,
			AttributeBinderTypeAnnotation.class,
			EnumSet.of( Kind.ANNOTATION ),
			false
	);
	OrmAnnotationDescriptor<Bag, BagAnnotation> BAG = new OrmAnnotationDescriptor<>(
			Bag.class,
			BagAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	SpecializedAnnotationDescriptor<BatchSize, BatchSizeAnnotation> BATCH_SIZE = new SpecializedAnnotationDescriptor<>(
			BatchSize.class,
			BatchSizeAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.FIELD, Kind.METHOD ),
			false
	);
	OrmAnnotationDescriptor<Cache, CacheAnnotation> CACHE = new OrmAnnotationDescriptor<>(
			Cache.class,
			CacheAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<Cascade, CascadeAnnotation> CASCADE = new OrmAnnotationDescriptor<>(
			Cascade.class,
			CascadeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<Checks, ChecksAnnotation> CHECKS = new OrmAnnotationDescriptor<>(
			Checks.class,
			ChecksAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<Check, CheckAnnotation> CHECK = new OrmAnnotationDescriptor<>(
			Check.class,
			CheckAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false,
			CHECKS
	);
	SpecializedAnnotationDescriptor<Collate, CollateAnnotation> COLLATE = new SpecializedAnnotationDescriptor<>(
			Collate.class,
			CollateAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	OrmAnnotationDescriptor<CollectionId, CollectionIdAnnotation> COLLECTION_ID = new OrmAnnotationDescriptor<>(
			CollectionId.class,
			CollectionIdAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<CollectionIdJavaClass, CollectionIdJavaClassAnnotation> COLLECTION_ID_JAVA_CLASS = new OrmAnnotationDescriptor<>(
			CollectionIdJavaClass.class,
			CollectionIdJavaClassAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	OrmAnnotationDescriptor<CollectionIdJavaType, CollectionIdJavaTypeAnnotation> COLLECTION_ID_JAVA_TYPE = new OrmAnnotationDescriptor<>(
			CollectionIdJavaType.class,
			CollectionIdJavaTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	OrmAnnotationDescriptor<CollectionIdJdbcType, CollectionIdJdbcTypeAnnotation> COLLECTION_ID_JDBC_TYPE = new OrmAnnotationDescriptor<>(
			CollectionIdJdbcType.class,
			CollectionIdJdbcTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	OrmAnnotationDescriptor<CollectionIdJdbcTypeCode, CollectionIdJdbcTypeCodeAnnotation> COLLECTION_ID_JDBC_TYPE_CODE = new OrmAnnotationDescriptor<>(
			CollectionIdJdbcTypeCode.class,
			CollectionIdJdbcTypeCodeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	OrmAnnotationDescriptor<CollectionIdMutability, CollectionIdMutabilityAnnotation> COLLECTION_ID_MUTABILITY = new OrmAnnotationDescriptor<>(
			CollectionIdMutability.class,
			CollectionIdMutabilityAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	OrmAnnotationDescriptor<CollectionIdType, CollectionIdTypeAnnotation> COLLECTION_ID_TYPE = new OrmAnnotationDescriptor<>(
			CollectionIdType.class,
			CollectionIdTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	OrmAnnotationDescriptor<CollectionType, CollectionTypeAnnotation> COLLECTION_TYPE = new OrmAnnotationDescriptor<>(
			CollectionType.class,
			CollectionTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	OrmAnnotationDescriptor<CollectionClassification, CollectionClassificationXmlAnnotation> COLLECTION_CLASSIFICATION = new OrmAnnotationDescriptor<>(
			CollectionClassification.class,
			CollectionClassificationXmlAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<CollectionTypeRegistrations, CollectionTypeRegistrationsAnnotation> COLLECTION_TYPE_REGISTRATIONS = new OrmAnnotationDescriptor<>(
			CollectionTypeRegistrations.class,
			CollectionTypeRegistrationsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION ),
			false
	);
	OrmAnnotationDescriptor<CollectionTypeRegistration, CollectionTypeRegistrationAnnotation> COLLECTION_TYPE_REGISTRATION = new OrmAnnotationDescriptor<>(
			CollectionTypeRegistration.class,
			CollectionTypeRegistrationAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false,
			COLLECTION_TYPE_REGISTRATIONS
	);
	OrmAnnotationDescriptor<ColumnDefault, ColumnDefaultAnnotation> COLUMN_DEFAULT = new OrmAnnotationDescriptor<>(
			ColumnDefault.class,
			ColumnDefaultAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<ColumnTransformers, ColumnTransformersAnnotation> COLUMN_TRANSFORMERS = new OrmAnnotationDescriptor<>(
			ColumnTransformers.class,
			ColumnTransformersAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<ColumnTransformer, ColumnTransformerAnnotation> COLUMN_TRANSFORMER = new OrmAnnotationDescriptor<>(
			ColumnTransformer.class,
			ColumnTransformerAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false,
			COLUMN_TRANSFORMERS
	);
	SpecializedAnnotationDescriptor<Comments, CommentsAnnotation> COMMENTS = new SpecializedAnnotationDescriptor<>(
			Comments.class,
			CommentsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.FIELD, Kind.METHOD ),
			false
	);
	SpecializedAnnotationDescriptor<Comment, CommentAnnotation> COMMENT = new SpecializedAnnotationDescriptor<>(
			Comment.class,
			CommentAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.FIELD, Kind.METHOD ),
			false,
			COMMENTS
	);
	OrmAnnotationDescriptor<CompositeType, CompositeTypeAnnotation> COMPOSITE_TYPE = new OrmAnnotationDescriptor<>(
			CompositeType.class,
			CompositeTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	OrmAnnotationDescriptor<CompositeTypeRegistrations, CompositeTypeRegistrationsAnnotation> COMPOSITE_TYPE_REGISTRATIONS = new OrmAnnotationDescriptor<>(
			CompositeTypeRegistrations.class,
			CompositeTypeRegistrationsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false
	);
	OrmAnnotationDescriptor<CompositeTypeRegistration, CompositeTypeRegistrationAnnotation> COMPOSITE_TYPE_REGISTRATION = new OrmAnnotationDescriptor<>(
			CompositeTypeRegistration.class,
			CompositeTypeRegistrationAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false,
			COMPOSITE_TYPE_REGISTRATIONS
	);
	OrmAnnotationDescriptor<ConcreteProxy, ConcreteProxyAnnotation> CONCRETE_PROXY = new OrmAnnotationDescriptor<>(
			ConcreteProxy.class,
			ConcreteProxyAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<ConverterRegistrations, ConverterRegistrationsAnnotation> CONVERTER_REGISTRATIONS = new OrmAnnotationDescriptor<>(
			ConverterRegistrations.class,
			ConverterRegistrationsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false
	);
	OrmAnnotationDescriptor<ConverterRegistration, ConverterRegistrationAnnotation> CONVERTER_REGISTRATION = new OrmAnnotationDescriptor<>(
			ConverterRegistration.class,
			ConverterRegistrationAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false,
			CONVERTER_REGISTRATIONS
	);
	OrmAnnotationDescriptor<CreationTimestamp, CreationTimestampAnnotation> CREATION_TIMESTAMP = new OrmAnnotationDescriptor<>(
			CreationTimestamp.class,
			CreationTimestampAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<CurrentTimestamp, CurrentTimestampAnnotation> CURRENT_TIMESTAMP = new OrmAnnotationDescriptor<>(
			CurrentTimestamp.class,
			CurrentTimestampAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	OrmAnnotationDescriptor<DiscriminatorFormula, DiscriminatorFormulaAnnotation> DISCRIMINATOR_FORMULA = new OrmAnnotationDescriptor<>(
			DiscriminatorFormula.class,
			DiscriminatorFormulaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	SpecializedAnnotationDescriptor<DiscriminatorOptions, DiscriminatorOptionsAnnotation> DISCRIMINATOR_OPTIONS = new SpecializedAnnotationDescriptor<>(
			DiscriminatorOptions.class,
			DiscriminatorOptionsAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<DynamicInsert, DynamicInsertAnnotation> DYNAMIC_INSERT = new OrmAnnotationDescriptor<>(
			DynamicInsert.class,
			DynamicInsertAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<DynamicUpdate, DynamicUpdateAnnotation> DYNAMIC_UPDATE = new OrmAnnotationDescriptor<>(
			DynamicUpdate.class,
			DynamicUpdateAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<EmbeddableInstantiator, EmbeddableInstantiatorAnnotation> EMBEDDABLE_INSTANTIATOR = new OrmAnnotationDescriptor<>(
			EmbeddableInstantiator.class,
			EmbeddableInstantiatorAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS, Kind.ANNOTATION ),
			false
	);
	OrmAnnotationDescriptor<EmbeddableInstantiatorRegistrations, EmbeddableInstantiatorRegistrationsAnnotation> EMBEDDABLE_INSTANTIATOR_REGISTRATIONS = new OrmAnnotationDescriptor<>(
			EmbeddableInstantiatorRegistrations.class,
			EmbeddableInstantiatorRegistrationsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false
	);
	OrmAnnotationDescriptor<EmbeddableInstantiatorRegistration, EmbeddableInstantiatorRegistrationAnnotation> EMBEDDABLE_INSTANTIATOR_REGISTRATION = new OrmAnnotationDescriptor<>(
			EmbeddableInstantiatorRegistration.class,
			EmbeddableInstantiatorRegistrationAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false,
			EMBEDDABLE_INSTANTIATOR_REGISTRATIONS
	);
	OrmAnnotationDescriptor<EmbeddedColumnNaming, EmbeddedColumnNamingAnnotation> EMBEDDED_COLUMN_NAMING = new OrmAnnotationDescriptor<>(
			EmbeddedColumnNaming.class,
			EmbeddedColumnNamingAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<EmbeddedTable, EmbeddedTableAnnotation> EMBEDDED_TABLE = new OrmAnnotationDescriptor<>(
			EmbeddedTable.class,
			EmbeddedTableAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<Fetch, FetchAnnotation> FETCH = new OrmAnnotationDescriptor<>(
			Fetch.class,
			FetchAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<FetchProfiles, FetchProfilesAnnotation> FETCH_PROFILES = new OrmAnnotationDescriptor<>(
			FetchProfiles.class,
			FetchProfilesAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			false
	);
	OrmAnnotationDescriptor<FetchProfile, FetchProfileAnnotation> FETCH_PROFILE = new OrmAnnotationDescriptor<>(
			FetchProfile.class,
			FetchProfileAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			false,
			FETCH_PROFILES
	);
	OrmAnnotationDescriptor<Filters, FiltersAnnotation> FILTERS = new OrmAnnotationDescriptor<>(
			Filters.class,
			FiltersAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<Filter, FilterAnnotation> FILTER = new OrmAnnotationDescriptor<>(
			Filter.class,
			FilterAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false,
			FILTERS
	);
	OrmAnnotationDescriptor<FilterDefs, FilterDefsAnnotation> FILTER_DEFS = new OrmAnnotationDescriptor<>(
			FilterDefs.class,
			FilterDefsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			false
	);
	OrmAnnotationDescriptor<FilterDef, FilterDefAnnotation> FILTER_DEF = new OrmAnnotationDescriptor<>(
			FilterDef.class,
			FilterDefAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			false,
			FILTER_DEFS
	);
	OrmAnnotationDescriptor<FilterJoinTables, FilterJoinTablesAnnotation> FILTER_JOIN_TABLES = new OrmAnnotationDescriptor<>(
			FilterJoinTables.class,
			FilterJoinTablesAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<FilterJoinTable, FilterJoinTableAnnotation> FILTER_JOIN_TABLE = new OrmAnnotationDescriptor<>(
			FilterJoinTable.class,
			FilterJoinTableAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false,
			FILTER_JOIN_TABLES
	);
	OrmAnnotationDescriptor<Formula, FormulaAnnotation> FORMULA = new OrmAnnotationDescriptor<>(
			Formula.class,
			FormulaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<FractionalSeconds, FractionalSecondsAnnotation> FRACTIONAL_SECONDS = new OrmAnnotationDescriptor<>(
			FractionalSeconds.class,
			FractionalSecondsAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<Generated, GeneratedAnnotation> GENERATED = new OrmAnnotationDescriptor<>(
			Generated.class,
			GeneratedAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<GeneratedColumn, GeneratedColumnAnnotation> GENERATED_COLUMN = new OrmAnnotationDescriptor<>(
			GeneratedColumn.class,
			GeneratedColumnAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<GenericGenerators, GenericGeneratorsAnnotation> GENERIC_GENERATORS = new OrmAnnotationDescriptor<>(
			GenericGenerators.class,
			GenericGeneratorsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			false
	);
	OrmAnnotationDescriptor<GenericGenerator, GenericGeneratorAnnotation> GENERIC_GENERATOR = new OrmAnnotationDescriptor<>(
			GenericGenerator.class,
			GenericGeneratorAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS, Kind.PACKAGE ),
			false,
			GENERIC_GENERATORS
	);
	OrmAnnotationDescriptor<HQLSelect, HQLSelectAnnotation> HQL_SELECT = new OrmAnnotationDescriptor<>(
			HQLSelect.class,
			HQLSelectAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<IdGeneratorType, IdGeneratorTypeAnnotation> ID_GENERATOR_TYPE = new OrmAnnotationDescriptor<>(
			IdGeneratorType.class,
			IdGeneratorTypeAnnotation.class,
			EnumSet.of( Kind.ANNOTATION ),
			false
	);
	OrmAnnotationDescriptor<Immutable, ImmutableAnnotation> IMMUTABLE = new OrmAnnotationDescriptor<>(
			Immutable.class,
			ImmutableAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<Imported, ImportedAnnotation> IMPORTED = new OrmAnnotationDescriptor<>(
			Imported.class,
			ImportedAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	// @Instantiator has @Target(CONSTRUCTOR) which is not supported by AnnotationTarget.Kind
	OrmAnnotationDescriptor<Instantiator, InstantiatorAnnotation> INSTANTIATOR = new OrmAnnotationDescriptor<>(
			Instantiator.class,
			InstantiatorAnnotation.class,
			EnumSet.noneOf( Kind.class ), // @Target(CONSTRUCTOR) - not representable in Kind enum
			false
	);
	OrmAnnotationDescriptor<JavaType, JavaTypeAnnotation> JAVA_TYPE = new OrmAnnotationDescriptor<>(
			JavaType.class,
			JavaTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	OrmAnnotationDescriptor<JavaTypeRegistrations, JavaTypeRegistrationsAnnotation> JAVA_TYPE_REGISTRATIONS = new OrmAnnotationDescriptor<>(
			JavaTypeRegistrations.class,
			JavaTypeRegistrationsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			true
	);
	OrmAnnotationDescriptor<JavaTypeRegistration, JavaTypeRegistrationAnnotation> JAVA_TYPE_REGISTRATION = new OrmAnnotationDescriptor<>(
			JavaTypeRegistration.class,
			JavaTypeRegistrationAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			true,
			JAVA_TYPE_REGISTRATIONS
	);
	OrmAnnotationDescriptor<JdbcType, JdbcTypeAnnotation> JDBC_TYPE = new OrmAnnotationDescriptor<>(
			JdbcType.class,
			JdbcTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	OrmAnnotationDescriptor<JdbcTypeCode, JdbcTypeCodeAnnotation> JDBC_TYPE_CODE = new OrmAnnotationDescriptor<>(
			JdbcTypeCode.class,
			JdbcTypeCodeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	OrmAnnotationDescriptor<JdbcTypeRegistrations, JdbcTypeRegistrationsAnnotation> JDBC_TYPE_REGISTRATIONS = new OrmAnnotationDescriptor<>(
			JdbcTypeRegistrations.class,
			JdbcTypeRegistrationsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			true
	);
	OrmAnnotationDescriptor<JdbcTypeRegistration, JdbcTypeRegistrationAnnotation> JDBC_TYPE_REGISTRATION = new OrmAnnotationDescriptor<>(
			JdbcTypeRegistration.class,
			JdbcTypeRegistrationAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			true,
			JDBC_TYPE_REGISTRATIONS
	);
	OrmAnnotationDescriptor<JoinColumnsOrFormulas, JoinColumnsOrFormulasAnnotation> JOIN_COLUMNS_OR_FORMULAS = new OrmAnnotationDescriptor<>(
			JoinColumnsOrFormulas.class,
			JoinColumnsOrFormulasAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<JoinColumnOrFormula, JoinColumnOrFormulaAnnotation> JOIN_COLUMN_OR_FORMULA = new OrmAnnotationDescriptor<>(
			JoinColumnOrFormula.class,
			JoinColumnOrFormulaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false,
			JOIN_COLUMNS_OR_FORMULAS
	);
	OrmAnnotationDescriptor<JoinFormula, JoinFormulaAnnotation> JOIN_FORMULA = new OrmAnnotationDescriptor<>(
			JoinFormula.class,
			JoinFormulaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<LazyGroup, LazyGroupAnnotation> LAZY_GROUP = new OrmAnnotationDescriptor<>(
			LazyGroup.class,
			LazyGroupAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<ListIndexBase, ListIndexBaseAnnotation> LIST_INDEX_BASE = new OrmAnnotationDescriptor<>(
			ListIndexBase.class,
			ListIndexBaseAnnotation.class,
			EnumSet.allOf( Kind.class ),
			false
	);
	OrmAnnotationDescriptor<ListIndexJavaType, ListIndexJavaTypeAnnotation> LIST_INDEX_JAVA_TYPE = new OrmAnnotationDescriptor<>(
			ListIndexJavaType.class,
			ListIndexJavaTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	OrmAnnotationDescriptor<ListIndexJdbcType, ListIndexJdbcTypeAnnotation> LIST_INDEX_JDBC_TYPE = new OrmAnnotationDescriptor<>(
			ListIndexJdbcType.class,
			ListIndexJdbcTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	OrmAnnotationDescriptor<ListIndexJdbcTypeCode, ListIndexJdbcTypeCodeAnnotation> LIST_INDEX_JDBC_TYPE_CODE = new OrmAnnotationDescriptor<>(
			ListIndexJdbcTypeCode.class,
			ListIndexJdbcTypeCodeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	OrmAnnotationDescriptor<ManyToAny, ManyToAnyAnnotation> MANY_TO_ANY = new OrmAnnotationDescriptor<>(
			ManyToAny.class,
			ManyToAnyAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<MapKeyCompositeType, MapKeyCompositeTypeAnnotation> MAP_KEY_COMPOSITE_TYPE = new OrmAnnotationDescriptor<>(
			MapKeyCompositeType.class,
			MapKeyCompositeTypeAnnotation.class,
			EnumSet.allOf( Kind.class ),
			false
	);
	OrmAnnotationDescriptor<MapKeyJavaType, MapKeyJavaTypeAnnotation> MAP_KEY_JAVA_TYPE = new OrmAnnotationDescriptor<>(
			MapKeyJavaType.class,
			MapKeyJavaTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	OrmAnnotationDescriptor<MapKeyJdbcType, MapKeyJdbcTypeAnnotation> MAP_KEY_JDBC_TYPE = new OrmAnnotationDescriptor<>(
			MapKeyJdbcType.class,
			MapKeyJdbcTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	OrmAnnotationDescriptor<MapKeyJdbcTypeCode, MapKeyJdbcTypeCodeAnnotation> MAP_KEY_JDBC_TYPE_CODE = new OrmAnnotationDescriptor<>(
			MapKeyJdbcTypeCode.class,
			MapKeyJdbcTypeCodeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	OrmAnnotationDescriptor<MapKeyMutability, MapKeyMutabilityAnnotation> MAP_KEY_MUTABILITY = new OrmAnnotationDescriptor<>(
			MapKeyMutability.class,
			MapKeyMutabilityAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	OrmAnnotationDescriptor<MapKeyType, MapKeyTypeAnnotation> MAP_KEY_TYPE = new OrmAnnotationDescriptor<>(
			MapKeyType.class,
			MapKeyTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	OrmAnnotationDescriptor<Mutability, MutabilityAnnotation> MUTABILITY = new OrmAnnotationDescriptor<>(
			Mutability.class,
			MutabilityAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS, Kind.ANNOTATION ),
			true
	);
	OrmAnnotationDescriptor<NamedEntityGraphs, NamedEntityGraphsAnnotation> NAMED_ENTITY_GRAPHS = new OrmAnnotationDescriptor<>(
			NamedEntityGraphs.class,
			NamedEntityGraphsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false
	);
	OrmAnnotationDescriptor<NamedEntityGraph, NamedEntityGraphAnnotation> NAMED_ENTITY_GRAPH = new OrmAnnotationDescriptor<>(
			NamedEntityGraph.class,
			NamedEntityGraphAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			false,
			NAMED_ENTITY_GRAPHS
	);
	OrmAnnotationDescriptor<NamedNativeQueries, NamedNativeQueriesAnnotation> NAMED_NATIVE_QUERIES = new OrmAnnotationDescriptor<>(
			NamedNativeQueries.class,
			NamedNativeQueriesAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			false
	);
	OrmAnnotationDescriptor<NamedNativeQuery, NamedNativeQueryAnnotation> NAMED_NATIVE_QUERY = new OrmAnnotationDescriptor<>(
			NamedNativeQuery.class,
			NamedNativeQueryAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			false,
			NAMED_NATIVE_QUERIES
	);
	OrmAnnotationDescriptor<NamedQueries, NamedQueriesAnnotation> NAMED_QUERIES = new OrmAnnotationDescriptor<>(
			NamedQueries.class,
			NamedQueriesAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			false
	);
	OrmAnnotationDescriptor<NamedQuery, NamedQueryAnnotation> NAMED_QUERY = new OrmAnnotationDescriptor<>(
			NamedQuery.class,
			NamedQueryAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			false,
			NAMED_QUERIES
	);
	OrmAnnotationDescriptor<Nationalized, NationalizedAnnotation> NATIONALIZED = new OrmAnnotationDescriptor<>(
			Nationalized.class,
			NationalizedAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	OrmAnnotationDescriptor<NativeGenerator, NativeGeneratorAnnotation> NATIVE_GENERATOR = new OrmAnnotationDescriptor<>(
			NativeGenerator.class,
			NativeGeneratorAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS, Kind.PACKAGE ),
			false
	);
	OrmAnnotationDescriptor<NaturalId, NaturalIdAnnotation> NATURAL_ID = new OrmAnnotationDescriptor<>(
			NaturalId.class,
			NaturalIdAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<NaturalIdCache, NaturalIdCacheAnnotation> NATURAL_ID_CACHE = new OrmAnnotationDescriptor<>(
			NaturalIdCache.class,
			NaturalIdCacheAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<NaturalIdClass, NaturalIdClassAnnotation> NATURAL_ID_CLASS = new OrmAnnotationDescriptor<>(
			NaturalIdClass.class,
			NaturalIdClassAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<NotFound, NotFoundAnnotation> NOT_FOUND = new OrmAnnotationDescriptor<>(
			NotFound.class,
			NotFoundAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<OnDelete, OnDeleteAnnotation> ON_DELETE = new OrmAnnotationDescriptor<>(
			OnDelete.class,
			OnDeleteAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<OptimisticLock, OptimisticLockAnnotation> OPTIMISTIC_LOCK = new OrmAnnotationDescriptor<>(
			OptimisticLock.class,
			OptimisticLockAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<OptimisticLocking, OptimisticLockingAnnotation> OPTIMISTIC_LOCKING = new OrmAnnotationDescriptor<>(
			OptimisticLocking.class,
			OptimisticLockingAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	// @ParamDef has @Target({}) - used as nested annotation, not directly on code elements
	OrmAnnotationDescriptor<ParamDef, ParamDefAnnotation> PARAM_DEF = new OrmAnnotationDescriptor<>(
			ParamDef.class,
			ParamDefAnnotation.class,
			EnumSet.noneOf( Kind.class ), // @Target({})
			false
	);
	// @Parameter has @Target({}) - used as nested annotation, not directly on code elements
	OrmAnnotationDescriptor<Parameter, ParameterAnnotation> PARAMETER = new OrmAnnotationDescriptor<>(
			Parameter.class,
			ParameterAnnotation.class,
			EnumSet.noneOf( Kind.class ), // @Target({})
			false
	);
	OrmAnnotationDescriptor<Parent, ParentAnnotation> PARENT = new OrmAnnotationDescriptor<>(
			Parent.class,
			ParentAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<PartitionKey, PartitionKeyAnnotation> PARTITION_KEY = new OrmAnnotationDescriptor<>(
			PartitionKey.class,
			PartitionKeyAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<PropertyRef, PropertyRefAnnotation> PROPERTY_REF = new OrmAnnotationDescriptor<>(
			PropertyRef.class,
			PropertyRefAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	OrmAnnotationDescriptor<QueryCacheLayout, QueryCacheLayoutAnnotation> QUERY_CACHE_LAYOUT = new OrmAnnotationDescriptor<>(
			QueryCacheLayout.class,
			QueryCacheLayoutAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<RowId, RowIdAnnotation> ROW_ID = new OrmAnnotationDescriptor<>(
			RowId.class,
			RowIdAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<SecondaryRows, SecondaryRowsAnnotation> SECONDARY_ROWS = new OrmAnnotationDescriptor<>(
			SecondaryRows.class,
			SecondaryRowsAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<SecondaryRow, SecondaryRowAnnotation> SECONDARY_ROW = new OrmAnnotationDescriptor<>(
			SecondaryRow.class,
			SecondaryRowAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false,
			SECONDARY_ROWS
	);
	OrmAnnotationDescriptor<SoftDelete, SoftDeleteAnnotation> SOFT_DELETE = new OrmAnnotationDescriptor<>(
			SoftDelete.class,
			SoftDeleteAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false
	);
	OrmAnnotationDescriptor<SortComparator, SortComparatorAnnotation> SORT_COMPARATOR = new OrmAnnotationDescriptor<>(
			SortComparator.class,
			SortComparatorAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<SortNatural, SortNaturalAnnotation> SORT_NATURAL = new OrmAnnotationDescriptor<>(
			SortNatural.class,
			SortNaturalAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<Source, SourceAnnotation> SOURCE = new OrmAnnotationDescriptor<>(
			Source.class,
			SourceAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<SQLDeletes, SQLDeletesAnnotation> SQL_DELETES = new OrmAnnotationDescriptor<>(
			SQLDeletes.class,
			SQLDeletesAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<SQLDelete, SQLDeleteAnnotation> SQL_DELETE = new OrmAnnotationDescriptor<>(
			SQLDelete.class,
			SQLDeleteAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false,
			SQL_DELETES
	);
	OrmAnnotationDescriptor<SQLDeleteAll, SQLDeleteAllAnnotation> SQL_DELETE_ALL = new OrmAnnotationDescriptor<>(
			SQLDeleteAll.class,
			SQLDeleteAllAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	// @SqlFragmentAlias has @Target({}) - used as nested annotation, not directly on code elements
	OrmAnnotationDescriptor<SqlFragmentAlias, SqlFragmentAliasAnnotation> SQL_FRAGMENT_ALIAS = new OrmAnnotationDescriptor<>(
			SqlFragmentAlias.class,
			SqlFragmentAliasAnnotation.class,
			EnumSet.noneOf( Kind.class ), // @Target({})
			false
	);
	OrmAnnotationDescriptor<SQLInserts, SQLInsertsAnnotation> SQL_INSERTS = new OrmAnnotationDescriptor<>(
			SQLInserts.class,
			SQLInsertsAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<SQLInsert, SQLInsertAnnotation> SQL_INSERT = new OrmAnnotationDescriptor<>(
			SQLInsert.class,
			SQLInsertAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false,
			SQL_INSERTS
	);
	OrmAnnotationDescriptor<SQLOrder, SQLOrderAnnotation> SQL_ORDER = new OrmAnnotationDescriptor<>(
			SQLOrder.class,
			SQLOrderAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<SQLRestriction, SQLRestrictionAnnotation> SQL_RESTRICTION = new OrmAnnotationDescriptor<>(
			SQLRestriction.class,
			SQLRestrictionAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<SQLSelect, SQLSelectAnnotation> SQL_SELECT = new OrmAnnotationDescriptor<>(
			SQLSelect.class,
			SQLSelectAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<SQLJoinTableRestriction, SQLJoinTableRestrictionAnnotation> SQL_JOIN_TABLE_RESTRICTION = new OrmAnnotationDescriptor<>(
			SQLJoinTableRestriction.class,
			SQLJoinTableRestrictionAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<SQLUpdates, SQLUpdatesAnnotation> SQL_UPDATES = new OrmAnnotationDescriptor<>(
			SQLUpdates.class,
			SQLUpdatesAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<SQLUpdate, SQLUpdateAnnotation> SQL_UPDATE = new OrmAnnotationDescriptor<>(
			SQLUpdate.class,
			SQLUpdateAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false,
			SQL_UPDATES
	);
	OrmAnnotationDescriptor<Struct, StructAnnotation> STRUCT = new OrmAnnotationDescriptor<>(
			Struct.class,
			StructAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<Subselect, SubselectAnnotation> SUBSELECT = new OrmAnnotationDescriptor<>(
			Subselect.class,
			SubselectAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<Synchronize, SynchronizeAnnotation> SYNCHRONIZE = new OrmAnnotationDescriptor<>(
			Synchronize.class,
			SynchronizeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	OrmAnnotationDescriptor<TargetEmbeddable, TargetEmbeddableAnnotation> TARGET_EMBEDDABLE = new OrmAnnotationDescriptor<>(
			TargetEmbeddable.class,
			TargetEmbeddableAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.FIELD, Kind.METHOD ),
			false
	);
	SpecializedAnnotationDescriptor<TenantId, TenantIdAnnotation> TENANT_ID = new SpecializedAnnotationDescriptor<>(
			TenantId.class,
			TenantIdAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	OrmAnnotationDescriptor<TimeZoneColumn, TimeZoneColumnAnnotation> TIME_ZONE_COLUMN = new OrmAnnotationDescriptor<>(
			TimeZoneColumn.class,
			TimeZoneColumnAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<TimeZoneStorage, TimeZoneStorageAnnotation> TIME_ZONE_STORAGE = new OrmAnnotationDescriptor<>(
			TimeZoneStorage.class,
			TimeZoneStorageAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<Type, TypeAnnotation> TYPE = new OrmAnnotationDescriptor<>(
			Type.class,
			TypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	SpecializedAnnotationDescriptor<TypeBinderType, TypeBinderTypeAnnotation> TYPE_BINDER_TYPE = new SpecializedAnnotationDescriptor<>(
			TypeBinderType.class,
			TypeBinderTypeAnnotation.class,
			EnumSet.of( Kind.ANNOTATION ),
			false
	);
	OrmAnnotationDescriptor<TypeRegistrations, TypeRegistrationsAnnotation> TYPE_REGISTRATIONS = new OrmAnnotationDescriptor<>(
			TypeRegistrations.class,
			TypeRegistrationsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false
	);
	OrmAnnotationDescriptor<TypeRegistration, TypeRegistrationAnnotation> TYPE_REGISTRATION = new OrmAnnotationDescriptor<>(
			TypeRegistration.class,
			TypeRegistrationAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false,
			TYPE_REGISTRATIONS
	);
	OrmAnnotationDescriptor<UpdateTimestamp, UpdateTimestampAnnotation> UPDATE_TIMESTAMP = new OrmAnnotationDescriptor<>(
			UpdateTimestamp.class,
			UpdateTimestampAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<UuidGenerator, UuidGeneratorAnnotation> UUID_GENERATOR = new OrmAnnotationDescriptor<>(
			UuidGenerator.class,
			UuidGeneratorAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	OrmAnnotationDescriptor<ValueGenerationType, ValueGenerationTypeAnnotation> VALUE_GENERATION_TYPE = new OrmAnnotationDescriptor<>(
			ValueGenerationType.class,
			ValueGenerationTypeAnnotation.class,
			EnumSet.of( Kind.ANNOTATION ),
			false
	);
	OrmAnnotationDescriptor<View, ViewAnnotation> VIEW = new OrmAnnotationDescriptor<>(
			View.class,
			ViewAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	static void forEachAnnotation(Consumer<AnnotationDescriptor<? extends Annotation>> consumer) {
		OrmAnnotationHelper.forEachOrmAnnotation( HibernateAnnotations.class, consumer );
	}
}
