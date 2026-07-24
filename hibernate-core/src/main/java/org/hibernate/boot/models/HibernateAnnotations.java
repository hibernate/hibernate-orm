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
import org.hibernate.models.Creator;
import org.hibernate.models.spi.MutableAnnotationDescriptor;
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
	MutableAnnotationDescriptor<Any, AnyAnnotation> ANY = Creator.createCompleteAnnotationDescriptor(
			Any.class,
			AnyAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<AnyDiscriminator, AnyDiscriminatorAnnotation> ANY_DISCRIMINATOR = Creator.createCompleteAnnotationDescriptor(
			AnyDiscriminator.class,
			AnyDiscriminatorAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<AnyDiscriminatorImplicitValues, AnyDiscriminatorImplicitValuesAnnotation> ANY_DISCRIMINATOR_IMPLICIT_VALUES = Creator.createCompleteAnnotationDescriptor(
			AnyDiscriminatorImplicitValues.class,
			AnyDiscriminatorImplicitValuesAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<AnyDiscriminatorValues, AnyDiscriminatorValuesAnnotation> ANY_DISCRIMINATOR_VALUES = Creator.createCompleteAnnotationDescriptor(
			AnyDiscriminatorValues.class,
			AnyDiscriminatorValuesAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<AnyDiscriminatorValue, AnyDiscriminatorValueAnnotation> ANY_DISCRIMINATOR_VALUE = Creator.createCompleteAnnotationDescriptor(
			AnyDiscriminatorValue.class,
			AnyDiscriminatorValueAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false,
			ANY_DISCRIMINATOR_VALUES
	);
	MutableAnnotationDescriptor<AnyKeyJavaClass, AnyKeyJavaClassAnnotation> ANY_KEY_JAVA_CLASS = Creator.createCompleteAnnotationDescriptor(
			AnyKeyJavaClass.class,
			AnyKeyJavaClassAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<AnyKeyJavaType, AnyKeyJavaTypeAnnotation> ANY_KEY_JAVA_TYPE = Creator.createCompleteAnnotationDescriptor(
			AnyKeyJavaType.class,
			AnyKeyJavaTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<AnyKeyJdbcType, AnyKeyJdbcTypeAnnotation> ANY_KEY_JDBC_TYPE = Creator.createCompleteAnnotationDescriptor(
			AnyKeyJdbcType.class,
			AnyKeyJdbcTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<AnyKeyJdbcTypeCode, AnyKeyJdbcTypeCodeAnnotation> ANY_KEY_JDBC_TYPE_CODE = Creator.createCompleteAnnotationDescriptor(
			AnyKeyJdbcTypeCode.class,
			AnyKeyJdbcTypeCodeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<AnyKeyType, AnyKeTypeAnnotation> ANY_KEY_TYPE = Creator.createCompleteAnnotationDescriptor(
			AnyKeyType.class,
			AnyKeTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<Array, ArrayAnnotation> ARRAY = Creator.createCompleteAnnotationDescriptor(
			Array.class,
			ArrayAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<AttributeAccessor, AttributeAccessorAnnotation> ATTRIBUTE_ACCESSOR = Creator.createCompleteAnnotationDescriptor(
			AttributeAccessor.class,
			AttributeAccessorAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	MutableAnnotationDescriptor<AttributeBinderType, AttributeBinderTypeAnnotation> ATTRIBUTE_BINDER_TYPE = Creator.createCompleteAnnotationDescriptor(
			AttributeBinderType.class,
			AttributeBinderTypeAnnotation.class,
			EnumSet.of( Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<Bag, BagAnnotation> BAG = Creator.createCompleteAnnotationDescriptor(
			Bag.class,
			BagAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<BatchSize, BatchSizeAnnotation> BATCH_SIZE = Creator.createCompleteAnnotationDescriptor(
			BatchSize.class,
			BatchSizeAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.FIELD, Kind.METHOD ),
			false
	);
	MutableAnnotationDescriptor<Cache, CacheAnnotation> CACHE = Creator.createCompleteAnnotationDescriptor(
			Cache.class,
			CacheAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<Checks, ChecksAnnotation> CHECKS = Creator.createCompleteAnnotationDescriptor(
			Checks.class,
			ChecksAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<Check, CheckAnnotation> CHECK = Creator.createCompleteAnnotationDescriptor(
			Check.class,
			CheckAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false,
			CHECKS
	);
	MutableAnnotationDescriptor<Collate, CollateAnnotation> COLLATE = Creator.createCompleteAnnotationDescriptor(
			Collate.class,
			CollateAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	MutableAnnotationDescriptor<CollectionId, CollectionIdAnnotation> COLLECTION_ID = Creator.createCompleteAnnotationDescriptor(
			CollectionId.class,
			CollectionIdAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<CollectionIdJavaClass, CollectionIdJavaClassAnnotation> COLLECTION_ID_JAVA_CLASS = Creator.createCompleteAnnotationDescriptor(
			CollectionIdJavaClass.class,
			CollectionIdJavaClassAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<CollectionIdJavaType, CollectionIdJavaTypeAnnotation> COLLECTION_ID_JAVA_TYPE = Creator.createCompleteAnnotationDescriptor(
			CollectionIdJavaType.class,
			CollectionIdJavaTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	MutableAnnotationDescriptor<CollectionIdJdbcType, CollectionIdJdbcTypeAnnotation> COLLECTION_ID_JDBC_TYPE = Creator.createCompleteAnnotationDescriptor(
			CollectionIdJdbcType.class,
			CollectionIdJdbcTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	MutableAnnotationDescriptor<CollectionIdJdbcTypeCode, CollectionIdJdbcTypeCodeAnnotation> COLLECTION_ID_JDBC_TYPE_CODE = Creator.createCompleteAnnotationDescriptor(
			CollectionIdJdbcTypeCode.class,
			CollectionIdJdbcTypeCodeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	MutableAnnotationDescriptor<CollectionIdMutability, CollectionIdMutabilityAnnotation> COLLECTION_ID_MUTABILITY = Creator.createCompleteAnnotationDescriptor(
			CollectionIdMutability.class,
			CollectionIdMutabilityAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	MutableAnnotationDescriptor<CollectionIdType, CollectionIdTypeAnnotation> COLLECTION_ID_TYPE = Creator.createCompleteAnnotationDescriptor(
			CollectionIdType.class,
			CollectionIdTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<CollectionType, CollectionTypeAnnotation> COLLECTION_TYPE = Creator.createCompleteAnnotationDescriptor(
			CollectionType.class,
			CollectionTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<CollectionClassification, CollectionClassificationXmlAnnotation> COLLECTION_CLASSIFICATION = Creator.createCompleteAnnotationDescriptor(
			CollectionClassification.class,
			CollectionClassificationXmlAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<CollectionTypeRegistrations, CollectionTypeRegistrationsAnnotation> COLLECTION_TYPE_REGISTRATIONS = Creator.createCompleteAnnotationDescriptor(
			CollectionTypeRegistrations.class,
			CollectionTypeRegistrationsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<CollectionTypeRegistration, CollectionTypeRegistrationAnnotation> COLLECTION_TYPE_REGISTRATION = Creator.createCompleteAnnotationDescriptor(
			CollectionTypeRegistration.class,
			CollectionTypeRegistrationAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false,
			COLLECTION_TYPE_REGISTRATIONS
	);
	MutableAnnotationDescriptor<ColumnDefault, ColumnDefaultAnnotation> COLUMN_DEFAULT = Creator.createCompleteAnnotationDescriptor(
			ColumnDefault.class,
			ColumnDefaultAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<ColumnTransformers, ColumnTransformersAnnotation> COLUMN_TRANSFORMERS = Creator.createCompleteAnnotationDescriptor(
			ColumnTransformers.class,
			ColumnTransformersAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<ColumnTransformer, ColumnTransformerAnnotation> COLUMN_TRANSFORMER = Creator.createCompleteAnnotationDescriptor(
			ColumnTransformer.class,
			ColumnTransformerAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false,
			COLUMN_TRANSFORMERS
	);
	MutableAnnotationDescriptor<CompositeType, CompositeTypeAnnotation> COMPOSITE_TYPE = Creator.createCompleteAnnotationDescriptor(
			CompositeType.class,
			CompositeTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<CompositeTypeRegistrations, CompositeTypeRegistrationsAnnotation> COMPOSITE_TYPE_REGISTRATIONS = Creator.createCompleteAnnotationDescriptor(
			CompositeTypeRegistrations.class,
			CompositeTypeRegistrationsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false
	);
	MutableAnnotationDescriptor<CompositeTypeRegistration, CompositeTypeRegistrationAnnotation> COMPOSITE_TYPE_REGISTRATION = Creator.createCompleteAnnotationDescriptor(
			CompositeTypeRegistration.class,
			CompositeTypeRegistrationAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false,
			COMPOSITE_TYPE_REGISTRATIONS
	);
	MutableAnnotationDescriptor<ConcreteProxy, ConcreteProxyAnnotation> CONCRETE_PROXY = Creator.createCompleteAnnotationDescriptor(
			ConcreteProxy.class,
			ConcreteProxyAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<ConverterRegistrations, ConverterRegistrationsAnnotation> CONVERTER_REGISTRATIONS = Creator.createCompleteAnnotationDescriptor(
			ConverterRegistrations.class,
			ConverterRegistrationsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false
	);
	MutableAnnotationDescriptor<ConverterRegistration, ConverterRegistrationAnnotation> CONVERTER_REGISTRATION = Creator.createCompleteAnnotationDescriptor(
			ConverterRegistration.class,
			ConverterRegistrationAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false,
			CONVERTER_REGISTRATIONS
	);
	MutableAnnotationDescriptor<CreationTimestamp, CreationTimestampAnnotation> CREATION_TIMESTAMP = Creator.createCompleteAnnotationDescriptor(
			CreationTimestamp.class,
			CreationTimestampAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<CurrentTimestamp, CurrentTimestampAnnotation> CURRENT_TIMESTAMP = Creator.createCompleteAnnotationDescriptor(
			CurrentTimestamp.class,
			CurrentTimestampAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<DiscriminatorFormula, DiscriminatorFormulaAnnotation> DISCRIMINATOR_FORMULA = Creator.createCompleteAnnotationDescriptor(
			DiscriminatorFormula.class,
			DiscriminatorFormulaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<DiscriminatorOptions, DiscriminatorOptionsAnnotation> DISCRIMINATOR_OPTIONS = Creator.createCompleteAnnotationDescriptor(
			DiscriminatorOptions.class,
			DiscriminatorOptionsAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<DynamicInsert, DynamicInsertAnnotation> DYNAMIC_INSERT = Creator.createCompleteAnnotationDescriptor(
			DynamicInsert.class,
			DynamicInsertAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<DynamicUpdate, DynamicUpdateAnnotation> DYNAMIC_UPDATE = Creator.createCompleteAnnotationDescriptor(
			DynamicUpdate.class,
			DynamicUpdateAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<EmbeddableInstantiator, EmbeddableInstantiatorAnnotation> EMBEDDABLE_INSTANTIATOR = Creator.createCompleteAnnotationDescriptor(
			EmbeddableInstantiator.class,
			EmbeddableInstantiatorAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS, Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<EmbeddableInstantiatorRegistrations, EmbeddableInstantiatorRegistrationsAnnotation> EMBEDDABLE_INSTANTIATOR_REGISTRATIONS = Creator.createCompleteAnnotationDescriptor(
			EmbeddableInstantiatorRegistrations.class,
			EmbeddableInstantiatorRegistrationsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false
	);
	MutableAnnotationDescriptor<EmbeddableInstantiatorRegistration, EmbeddableInstantiatorRegistrationAnnotation> EMBEDDABLE_INSTANTIATOR_REGISTRATION = Creator.createCompleteAnnotationDescriptor(
			EmbeddableInstantiatorRegistration.class,
			EmbeddableInstantiatorRegistrationAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false,
			EMBEDDABLE_INSTANTIATOR_REGISTRATIONS
	);
	MutableAnnotationDescriptor<EmbeddedColumnNaming, EmbeddedColumnNamingAnnotation> EMBEDDED_COLUMN_NAMING = Creator.createCompleteAnnotationDescriptor(
			EmbeddedColumnNaming.class,
			EmbeddedColumnNamingAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<EmbeddedTable, EmbeddedTableAnnotation> EMBEDDED_TABLE = Creator.createCompleteAnnotationDescriptor(
			EmbeddedTable.class,
			EmbeddedTableAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<Fetch, FetchAnnotation> FETCH = Creator.createCompleteAnnotationDescriptor(
			Fetch.class,
			FetchAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<FetchProfiles, FetchProfilesAnnotation> FETCH_PROFILES = Creator.createCompleteAnnotationDescriptor(
			FetchProfiles.class,
			FetchProfilesAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			false
	);
	MutableAnnotationDescriptor<FetchProfile, FetchProfileAnnotation> FETCH_PROFILE = Creator.createCompleteAnnotationDescriptor(
			FetchProfile.class,
			FetchProfileAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			false,
			FETCH_PROFILES
	);
	MutableAnnotationDescriptor<Filters, FiltersAnnotation> FILTERS = Creator.createCompleteAnnotationDescriptor(
			Filters.class,
			FiltersAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<Filter, FilterAnnotation> FILTER = Creator.createCompleteAnnotationDescriptor(
			Filter.class,
			FilterAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false,
			FILTERS
	);
	MutableAnnotationDescriptor<FilterDefs, FilterDefsAnnotation> FILTER_DEFS = Creator.createCompleteAnnotationDescriptor(
			FilterDefs.class,
			FilterDefsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			false
	);
	MutableAnnotationDescriptor<FilterDef, FilterDefAnnotation> FILTER_DEF = Creator.createCompleteAnnotationDescriptor(
			FilterDef.class,
			FilterDefAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			false,
			FILTER_DEFS
	);
	MutableAnnotationDescriptor<FilterJoinTables, FilterJoinTablesAnnotation> FILTER_JOIN_TABLES = Creator.createCompleteAnnotationDescriptor(
			FilterJoinTables.class,
			FilterJoinTablesAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<FilterJoinTable, FilterJoinTableAnnotation> FILTER_JOIN_TABLE = Creator.createCompleteAnnotationDescriptor(
			FilterJoinTable.class,
			FilterJoinTableAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false,
			FILTER_JOIN_TABLES
	);
	MutableAnnotationDescriptor<Formula, FormulaAnnotation> FORMULA = Creator.createCompleteAnnotationDescriptor(
			Formula.class,
			FormulaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<FractionalSeconds, FractionalSecondsAnnotation> FRACTIONAL_SECONDS = Creator.createCompleteAnnotationDescriptor(
			FractionalSeconds.class,
			FractionalSecondsAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<Generated, GeneratedAnnotation> GENERATED = Creator.createCompleteAnnotationDescriptor(
			Generated.class,
			GeneratedAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<GeneratedColumn, GeneratedColumnAnnotation> GENERATED_COLUMN = Creator.createCompleteAnnotationDescriptor(
			GeneratedColumn.class,
			GeneratedColumnAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<GenericGenerator, GenericGeneratorAnnotation> GENERIC_GENERATOR = Creator.createCompleteAnnotationDescriptor(
			GenericGenerator.class,
			GenericGeneratorAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS, Kind.PACKAGE ),
			false
	);
	MutableAnnotationDescriptor<HQLSelect, HQLSelectAnnotation> HQL_SELECT = Creator.createCompleteAnnotationDescriptor(
			HQLSelect.class,
			HQLSelectAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<IdGeneratorType, IdGeneratorTypeAnnotation> ID_GENERATOR_TYPE = Creator.createCompleteAnnotationDescriptor(
			IdGeneratorType.class,
			IdGeneratorTypeAnnotation.class,
			EnumSet.of( Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<Immutable, ImmutableAnnotation> IMMUTABLE = Creator.createCompleteAnnotationDescriptor(
			Immutable.class,
			ImmutableAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<Imported, ImportedAnnotation> IMPORTED = Creator.createCompleteAnnotationDescriptor(
			Imported.class,
			ImportedAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<Instantiator, InstantiatorAnnotation> INSTANTIATOR = Creator.createCompleteAnnotationDescriptor(
			Instantiator.class,
			InstantiatorAnnotation.class
	);
	MutableAnnotationDescriptor<JavaType, JavaTypeAnnotation> JAVA_TYPE = Creator.createCompleteAnnotationDescriptor(
			JavaType.class,
			JavaTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	MutableAnnotationDescriptor<JavaTypeRegistrations, JavaTypeRegistrationsAnnotation> JAVA_TYPE_REGISTRATIONS = Creator.createCompleteAnnotationDescriptor(
			JavaTypeRegistrations.class,
			JavaTypeRegistrationsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			true
	);
	MutableAnnotationDescriptor<JavaTypeRegistration, JavaTypeRegistrationAnnotation> JAVA_TYPE_REGISTRATION = Creator.createCompleteAnnotationDescriptor(
			JavaTypeRegistration.class,
			JavaTypeRegistrationAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			true,
			JAVA_TYPE_REGISTRATIONS
	);
	MutableAnnotationDescriptor<JdbcType, JdbcTypeAnnotation> JDBC_TYPE = Creator.createCompleteAnnotationDescriptor(
			JdbcType.class,
			JdbcTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	MutableAnnotationDescriptor<JdbcTypeCode, JdbcTypeCodeAnnotation> JDBC_TYPE_CODE = Creator.createCompleteAnnotationDescriptor(
			JdbcTypeCode.class,
			JdbcTypeCodeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	MutableAnnotationDescriptor<JdbcTypeRegistrations, JdbcTypeRegistrationsAnnotation> JDBC_TYPE_REGISTRATIONS = Creator.createCompleteAnnotationDescriptor(
			JdbcTypeRegistrations.class,
			JdbcTypeRegistrationsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			true
	);
	MutableAnnotationDescriptor<JdbcTypeRegistration, JdbcTypeRegistrationAnnotation> JDBC_TYPE_REGISTRATION = Creator.createCompleteAnnotationDescriptor(
			JdbcTypeRegistration.class,
			JdbcTypeRegistrationAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			true,
			JDBC_TYPE_REGISTRATIONS
	);
	MutableAnnotationDescriptor<JoinColumnsOrFormulas, JoinColumnsOrFormulasAnnotation> JOIN_COLUMNS_OR_FORMULAS = Creator.createCompleteAnnotationDescriptor(
			JoinColumnsOrFormulas.class,
			JoinColumnsOrFormulasAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<JoinColumnOrFormula, JoinColumnOrFormulaAnnotation> JOIN_COLUMN_OR_FORMULA = Creator.createCompleteAnnotationDescriptor(
			JoinColumnOrFormula.class,
			JoinColumnOrFormulaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false,
			JOIN_COLUMNS_OR_FORMULAS
	);
	MutableAnnotationDescriptor<JoinFormula, JoinFormulaAnnotation> JOIN_FORMULA = Creator.createCompleteAnnotationDescriptor(
			JoinFormula.class,
			JoinFormulaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<LazyGroup, LazyGroupAnnotation> LAZY_GROUP = Creator.createCompleteAnnotationDescriptor(
			LazyGroup.class,
			LazyGroupAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<ListIndexBase, ListIndexBaseAnnotation> LIST_INDEX_BASE = Creator.createCompleteAnnotationDescriptor(
			ListIndexBase.class,
			ListIndexBaseAnnotation.class,
			EnumSet.allOf( Kind.class ),
			false
	);
	MutableAnnotationDescriptor<ListIndexJavaType, ListIndexJavaTypeAnnotation> LIST_INDEX_JAVA_TYPE = Creator.createCompleteAnnotationDescriptor(
			ListIndexJavaType.class,
			ListIndexJavaTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	MutableAnnotationDescriptor<ListIndexJdbcType, ListIndexJdbcTypeAnnotation> LIST_INDEX_JDBC_TYPE = Creator.createCompleteAnnotationDescriptor(
			ListIndexJdbcType.class,
			ListIndexJdbcTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	MutableAnnotationDescriptor<ListIndexJdbcTypeCode, ListIndexJdbcTypeCodeAnnotation> LIST_INDEX_JDBC_TYPE_CODE = Creator.createCompleteAnnotationDescriptor(
			ListIndexJdbcTypeCode.class,
			ListIndexJdbcTypeCodeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	MutableAnnotationDescriptor<ManyToAny, ManyToAnyAnnotation> MANY_TO_ANY = Creator.createCompleteAnnotationDescriptor(
			ManyToAny.class,
			ManyToAnyAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<MapKeyCompositeType, MapKeyCompositeTypeAnnotation> MAP_KEY_COMPOSITE_TYPE = Creator.createCompleteAnnotationDescriptor(
			MapKeyCompositeType.class,
			MapKeyCompositeTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<MapKeyJavaType, MapKeyJavaTypeAnnotation> MAP_KEY_JAVA_TYPE = Creator.createCompleteAnnotationDescriptor(
			MapKeyJavaType.class,
			MapKeyJavaTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	MutableAnnotationDescriptor<MapKeyJdbcType, MapKeyJdbcTypeAnnotation> MAP_KEY_JDBC_TYPE = Creator.createCompleteAnnotationDescriptor(
			MapKeyJdbcType.class,
			MapKeyJdbcTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	MutableAnnotationDescriptor<MapKeyJdbcTypeCode, MapKeyJdbcTypeCodeAnnotation> MAP_KEY_JDBC_TYPE_CODE = Creator.createCompleteAnnotationDescriptor(
			MapKeyJdbcTypeCode.class,
			MapKeyJdbcTypeCodeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	MutableAnnotationDescriptor<MapKeyMutability, MapKeyMutabilityAnnotation> MAP_KEY_MUTABILITY = Creator.createCompleteAnnotationDescriptor(
			MapKeyMutability.class,
			MapKeyMutabilityAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	MutableAnnotationDescriptor<MapKeyType, MapKeyTypeAnnotation> MAP_KEY_TYPE = Creator.createCompleteAnnotationDescriptor(
			MapKeyType.class,
			MapKeyTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	MutableAnnotationDescriptor<Mutability, MutabilityAnnotation> MUTABILITY = Creator.createCompleteAnnotationDescriptor(
			Mutability.class,
			MutabilityAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS, Kind.ANNOTATION ),
			true
	);
	MutableAnnotationDescriptor<NamedEntityGraphs, NamedEntityGraphsAnnotation> NAMED_ENTITY_GRAPHS = Creator.createCompleteAnnotationDescriptor(
			NamedEntityGraphs.class,
			NamedEntityGraphsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false
	);
	MutableAnnotationDescriptor<NamedEntityGraph, NamedEntityGraphAnnotation> NAMED_ENTITY_GRAPH = Creator.createCompleteAnnotationDescriptor(
			NamedEntityGraph.class,
			NamedEntityGraphAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			false,
			NAMED_ENTITY_GRAPHS
	);
	MutableAnnotationDescriptor<NamedNativeQueries, NamedNativeQueriesAnnotation> NAMED_NATIVE_QUERIES = Creator.createCompleteAnnotationDescriptor(
			NamedNativeQueries.class,
			NamedNativeQueriesAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			false
	);
	MutableAnnotationDescriptor<NamedNativeQuery, NamedNativeQueryAnnotation> NAMED_NATIVE_QUERY = Creator.createCompleteAnnotationDescriptor(
			NamedNativeQuery.class,
			NamedNativeQueryAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			false,
			NAMED_NATIVE_QUERIES
	);
	MutableAnnotationDescriptor<NamedQueries, NamedQueriesAnnotation> NAMED_QUERIES = Creator.createCompleteAnnotationDescriptor(
			NamedQueries.class,
			NamedQueriesAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			false
	);
	MutableAnnotationDescriptor<NamedQuery, NamedQueryAnnotation> NAMED_QUERY = Creator.createCompleteAnnotationDescriptor(
			NamedQuery.class,
			NamedQueryAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE ),
			false,
			NAMED_QUERIES
	);
	MutableAnnotationDescriptor<Nationalized, NationalizedAnnotation> NATIONALIZED = Creator.createCompleteAnnotationDescriptor(
			Nationalized.class,
			NationalizedAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION, Kind.PACKAGE ),
			false
	);
	MutableAnnotationDescriptor<NativeGenerator, NativeGeneratorAnnotation> NATIVE_GENERATOR = Creator.createCompleteAnnotationDescriptor(
			NativeGenerator.class,
			NativeGeneratorAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS, Kind.PACKAGE ),
			false
	);
	MutableAnnotationDescriptor<NaturalId, NaturalIdAnnotation> NATURAL_ID = Creator.createCompleteAnnotationDescriptor(
			NaturalId.class,
			NaturalIdAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<NaturalIdCache, NaturalIdCacheAnnotation> NATURAL_ID_CACHE = Creator.createCompleteAnnotationDescriptor(
			NaturalIdCache.class,
			NaturalIdCacheAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<NaturalIdClass, NaturalIdClassAnnotation> NATURAL_ID_CLASS = Creator.createCompleteAnnotationDescriptor(
			NaturalIdClass.class,
			NaturalIdClassAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<NotFound, NotFoundAnnotation> NOT_FOUND = Creator.createCompleteAnnotationDescriptor(
			NotFound.class,
			NotFoundAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<OnDelete, OnDeleteAnnotation> ON_DELETE = Creator.createCompleteAnnotationDescriptor(
			OnDelete.class,
			OnDeleteAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<OptimisticLock, OptimisticLockAnnotation> OPTIMISTIC_LOCK = Creator.createCompleteAnnotationDescriptor(
			OptimisticLock.class,
			OptimisticLockAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<OptimisticLocking, OptimisticLockingAnnotation> OPTIMISTIC_LOCKING = Creator.createCompleteAnnotationDescriptor(
			OptimisticLocking.class,
			OptimisticLockingAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	// @ParamDef has @Target({}) - used as nested annotation, not directly on code elements
	MutableAnnotationDescriptor<ParamDef, ParamDefAnnotation> PARAM_DEF = Creator.createCompleteAnnotationDescriptor(
			ParamDef.class,
			ParamDefAnnotation.class,
			EnumSet.noneOf( Kind.class ), // @Target({})
			false
	);
	// @Parameter has @Target({}) - used as nested annotation, not directly on code elements
	MutableAnnotationDescriptor<Parameter, ParameterAnnotation> PARAMETER = Creator.createCompleteAnnotationDescriptor(
			Parameter.class,
			ParameterAnnotation.class,
			EnumSet.noneOf( Kind.class ), // @Target({})
			false
	);
	MutableAnnotationDescriptor<Parent, ParentAnnotation> PARENT = Creator.createCompleteAnnotationDescriptor(
			Parent.class,
			ParentAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<PartitionKey, PartitionKeyAnnotation> PARTITION_KEY = Creator.createCompleteAnnotationDescriptor(
			PartitionKey.class,
			PartitionKeyAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<PropertyRef, PropertyRefAnnotation> PROPERTY_REF = Creator.createCompleteAnnotationDescriptor(
			PropertyRef.class,
			PropertyRefAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<QueryCacheLayout, QueryCacheLayoutAnnotation> QUERY_CACHE_LAYOUT = Creator.createCompleteAnnotationDescriptor(
			QueryCacheLayout.class,
			QueryCacheLayoutAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<RowId, RowIdAnnotation> ROW_ID = Creator.createCompleteAnnotationDescriptor(
			RowId.class,
			RowIdAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<SecondaryRows, SecondaryRowsAnnotation> SECONDARY_ROWS = Creator.createCompleteAnnotationDescriptor(
			SecondaryRows.class,
			SecondaryRowsAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<SecondaryRow, SecondaryRowAnnotation> SECONDARY_ROW = Creator.createCompleteAnnotationDescriptor(
			SecondaryRow.class,
			SecondaryRowAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false,
			SECONDARY_ROWS
	);
	MutableAnnotationDescriptor<SoftDelete, SoftDeleteAnnotation> SOFT_DELETE = Creator.createCompleteAnnotationDescriptor(
			SoftDelete.class,
			SoftDeleteAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false
	);
	MutableAnnotationDescriptor<SortComparator, SortComparatorAnnotation> SORT_COMPARATOR = Creator.createCompleteAnnotationDescriptor(
			SortComparator.class,
			SortComparatorAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<SortNatural, SortNaturalAnnotation> SORT_NATURAL = Creator.createCompleteAnnotationDescriptor(
			SortNatural.class,
			SortNaturalAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<SQLDeletes, SQLDeletesAnnotation> SQL_DELETES = Creator.createCompleteAnnotationDescriptor(
			SQLDeletes.class,
			SQLDeletesAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<SQLDelete, SQLDeleteAnnotation> SQL_DELETE = Creator.createCompleteAnnotationDescriptor(
			SQLDelete.class,
			SQLDeleteAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false,
			SQL_DELETES
	);
	MutableAnnotationDescriptor<SQLDeleteAll, SQLDeleteAllAnnotation> SQL_DELETE_ALL = Creator.createCompleteAnnotationDescriptor(
			SQLDeleteAll.class,
			SQLDeleteAllAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	// @SqlFragmentAlias has @Target({}) - used as nested annotation, not directly on code elements
	MutableAnnotationDescriptor<SqlFragmentAlias, SqlFragmentAliasAnnotation> SQL_FRAGMENT_ALIAS = Creator.createCompleteAnnotationDescriptor(
			SqlFragmentAlias.class,
			SqlFragmentAliasAnnotation.class,
			EnumSet.noneOf( Kind.class ), // @Target({})
			false
	);
	MutableAnnotationDescriptor<SQLInserts, SQLInsertsAnnotation> SQL_INSERTS = Creator.createCompleteAnnotationDescriptor(
			SQLInserts.class,
			SQLInsertsAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<SQLInsert, SQLInsertAnnotation> SQL_INSERT = Creator.createCompleteAnnotationDescriptor(
			SQLInsert.class,
			SQLInsertAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false,
			SQL_INSERTS
	);
	MutableAnnotationDescriptor<SQLOrder, SQLOrderAnnotation> SQL_ORDER = Creator.createCompleteAnnotationDescriptor(
			SQLOrder.class,
			SQLOrderAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<SQLRestriction, SQLRestrictionAnnotation> SQL_RESTRICTION = Creator.createCompleteAnnotationDescriptor(
			SQLRestriction.class,
			SQLRestrictionAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<SQLSelect, SQLSelectAnnotation> SQL_SELECT = Creator.createCompleteAnnotationDescriptor(
			SQLSelect.class,
			SQLSelectAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<SQLJoinTableRestriction, SQLJoinTableRestrictionAnnotation> SQL_JOIN_TABLE_RESTRICTION = Creator.createCompleteAnnotationDescriptor(
			SQLJoinTableRestriction.class,
			SQLJoinTableRestrictionAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<SQLUpdates, SQLUpdatesAnnotation> SQL_UPDATES = Creator.createCompleteAnnotationDescriptor(
			SQLUpdates.class,
			SQLUpdatesAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<SQLUpdate, SQLUpdateAnnotation> SQL_UPDATE = Creator.createCompleteAnnotationDescriptor(
			SQLUpdate.class,
			SQLUpdateAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false,
			SQL_UPDATES
	);
	MutableAnnotationDescriptor<Struct, StructAnnotation> STRUCT = Creator.createCompleteAnnotationDescriptor(
			Struct.class,
			StructAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<Subselect, SubselectAnnotation> SUBSELECT = Creator.createCompleteAnnotationDescriptor(
			Subselect.class,
			SubselectAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<Synchronize, SynchronizeAnnotation> SYNCHRONIZE = Creator.createCompleteAnnotationDescriptor(
			Synchronize.class,
			SynchronizeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	MutableAnnotationDescriptor<TargetEmbeddable, TargetEmbeddableAnnotation> TARGET_EMBEDDABLE = Creator.createCompleteAnnotationDescriptor(
			TargetEmbeddable.class,
			TargetEmbeddableAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.FIELD, Kind.METHOD ),
			false
	);
	MutableAnnotationDescriptor<Temporal,TemporalAnnotation> TEMPORAL = Creator.createCompleteAnnotationDescriptor(
			Temporal.class,
			TemporalAnnotation.class,
			EnumSet.of( Kind.ANNOTATION, Kind.CLASS, Kind.FIELD, Kind.METHOD, Kind.PACKAGE ),
			false
	);
	MutableAnnotationDescriptor<Temporal.HistoryPartitioning,HistoryPartitioningAnnotation> TEMPORAL_HISTORY_PARTITIONING =
			Creator.createCompleteAnnotationDescriptor(
					Temporal.HistoryPartitioning.class,
					HistoryPartitioningAnnotation.class,
					EnumSet.of( Kind.CLASS, Kind.FIELD, Kind.METHOD ),
					false
			);
	MutableAnnotationDescriptor<Temporal.HistoryTable,HistoryTableAnnotation> TEMPORAL_HISTORY_TABLE =
			Creator.createCompleteAnnotationDescriptor(
					Temporal.HistoryTable.class,
					HistoryTableAnnotation.class,
					EnumSet.of( Kind.CLASS, Kind.FIELD, Kind.METHOD ),
					false
			);
	MutableAnnotationDescriptor<Temporal.Excluded,ExcludedAnnotation> TEMPORAL_EXCLUDED =
			Creator.createCompleteAnnotationDescriptor(
					Temporal.Excluded.class,
					ExcludedAnnotation.class,
					EnumSet.of( Kind.FIELD, Kind.METHOD ),
					false
			);
	MutableAnnotationDescriptor<TenantId, TenantIdAnnotation> TENANT_ID = Creator.createCompleteAnnotationDescriptor(
			TenantId.class,
			TenantIdAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	MutableAnnotationDescriptor<TimeZoneColumn, TimeZoneColumnAnnotation> TIME_ZONE_COLUMN = Creator.createCompleteAnnotationDescriptor(
			TimeZoneColumn.class,
			TimeZoneColumnAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<TimeZoneStorage, TimeZoneStorageAnnotation> TIME_ZONE_STORAGE = Creator.createCompleteAnnotationDescriptor(
			TimeZoneStorage.class,
			TimeZoneStorageAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<Type, TypeAnnotation> TYPE = Creator.createCompleteAnnotationDescriptor(
			Type.class,
			TypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<TypeBinderType, TypeBinderTypeAnnotation> TYPE_BINDER_TYPE = Creator.createCompleteAnnotationDescriptor(
			TypeBinderType.class,
			TypeBinderTypeAnnotation.class,
			EnumSet.of( Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<TypeRegistrations, TypeRegistrationsAnnotation> TYPE_REGISTRATIONS = Creator.createCompleteAnnotationDescriptor(
			TypeRegistrations.class,
			TypeRegistrationsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false
	);
	MutableAnnotationDescriptor<TypeRegistration, TypeRegistrationAnnotation> TYPE_REGISTRATION = Creator.createCompleteAnnotationDescriptor(
			TypeRegistration.class,
			TypeRegistrationAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false,
			TYPE_REGISTRATIONS
	);
	MutableAnnotationDescriptor<UpdateTimestamp, UpdateTimestampAnnotation> UPDATE_TIMESTAMP = Creator.createCompleteAnnotationDescriptor(
			UpdateTimestamp.class,
			UpdateTimestampAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<UuidGenerator, UuidGeneratorAnnotation> UUID_GENERATOR = Creator.createCompleteAnnotationDescriptor(
			UuidGenerator.class,
			UuidGeneratorAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	MutableAnnotationDescriptor<ValueGenerationType, ValueGenerationTypeAnnotation> VALUE_GENERATION_TYPE = Creator.createCompleteAnnotationDescriptor(
			ValueGenerationType.class,
			ValueGenerationTypeAnnotation.class,
			EnumSet.of( Kind.ANNOTATION ),
			false
	);
	MutableAnnotationDescriptor<View, ViewAnnotation> VIEW = Creator.createCompleteAnnotationDescriptor(
			View.class,
			ViewAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	static void forEachAnnotation(Consumer<AnnotationDescriptor<? extends Annotation>> consumer) {
		OrmAnnotationHelper.forEachOrmAnnotation( HibernateAnnotations.class, consumer );
	}
}
