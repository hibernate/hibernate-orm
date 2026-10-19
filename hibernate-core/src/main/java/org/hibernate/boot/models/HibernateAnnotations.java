package org.hibernate.boot.models;

import java.util.EnumSet;
import java.lang.annotation.Annotation;
import java.util.function.Consumer;

import org.hibernate.annotations.*;
import org.hibernate.boot.internal.AnyKeyType;
import org.hibernate.boot.internal.CollectionClassification;
import org.hibernate.boot.models.annotations.internal.*;
import org.hibernate.boot.models.internal.OrmAnnotationHelper;
import org.hibernate.models.CompleteAnnotationDescriptor;
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
	CompleteAnnotationDescriptor<Any, AnyAnnotation> ANY = new CompleteAnnotationDescriptor<>(
			Any.class,
			AnyAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<AnyDiscriminator, AnyDiscriminatorAnnotation> ANY_DISCRIMINATOR = new CompleteAnnotationDescriptor<>(
			AnyDiscriminator.class,
			AnyDiscriminatorAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	CompleteAnnotationDescriptor<AnyDiscriminatorImplicitValues, AnyDiscriminatorImplicitValuesAnnotation> ANY_DISCRIMINATOR_IMPLICIT_VALUES = new CompleteAnnotationDescriptor<>(
			AnyDiscriminatorImplicitValues.class,
			AnyDiscriminatorImplicitValuesAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	CompleteAnnotationDescriptor<AnyDiscriminatorValues, AnyDiscriminatorValuesAnnotation> ANY_DISCRIMINATOR_VALUES = new CompleteAnnotationDescriptor<>(
			AnyDiscriminatorValues.class,
			AnyDiscriminatorValuesAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	CompleteAnnotationDescriptor<AnyDiscriminatorValue, AnyDiscriminatorValueAnnotation> ANY_DISCRIMINATOR_VALUE = new CompleteAnnotationDescriptor<>(
			AnyDiscriminatorValue.class,
			AnyDiscriminatorValueAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false,
			ANY_DISCRIMINATOR_VALUES
	);
	CompleteAnnotationDescriptor<AnyKeyJavaClass, AnyKeyJavaClassAnnotation> ANY_KEY_JAVA_CLASS = new CompleteAnnotationDescriptor<>(
			AnyKeyJavaClass.class,
			AnyKeyJavaClassAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	CompleteAnnotationDescriptor<AnyKeyJavaType, AnyKeyJavaTypeAnnotation> ANY_KEY_JAVA_TYPE = new CompleteAnnotationDescriptor<>(
			AnyKeyJavaType.class,
			AnyKeyJavaTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	CompleteAnnotationDescriptor<AnyKeyJdbcType, AnyKeyJdbcTypeAnnotation> ANY_KEY_JDBC_TYPE = new CompleteAnnotationDescriptor<>(
			AnyKeyJdbcType.class,
			AnyKeyJdbcTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	CompleteAnnotationDescriptor<AnyKeyJdbcTypeCode, AnyKeyJdbcTypeCodeAnnotation> ANY_KEY_JDBC_TYPE_CODE = new CompleteAnnotationDescriptor<>(
			AnyKeyJdbcTypeCode.class,
			AnyKeyJdbcTypeCodeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	CompleteAnnotationDescriptor<AnyKeyType, AnyKeTypeAnnotation> ANY_KEY_TYPE = new CompleteAnnotationDescriptor<>(
			AnyKeyType.class,
			AnyKeTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<Array, ArrayAnnotation> ARRAY = new CompleteAnnotationDescriptor<>(
			Array.class,
			ArrayAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<AttributeAccessor, AttributeAccessorAnnotation> ATTRIBUTE_ACCESSOR = new CompleteAnnotationDescriptor<>(
			AttributeAccessor.class,
			AttributeAccessorAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	CompleteAnnotationDescriptor<AttributeBinderType, AttributeBinderTypeAnnotation> ATTRIBUTE_BINDER_TYPE = new CompleteAnnotationDescriptor<>(
			AttributeBinderType.class,
			AttributeBinderTypeAnnotation.class,
			EnumSet.of( Kind.ANNOTATION ),
			false
	);
	CompleteAnnotationDescriptor<Bag, BagAnnotation> BAG = new CompleteAnnotationDescriptor<>(
			Bag.class,
			BagAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	CompleteAnnotationDescriptor<BatchSize, BatchSizeAnnotation> BATCH_SIZE = new CompleteAnnotationDescriptor<>(
			BatchSize.class,
			BatchSizeAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.FIELD, Kind.METHOD ),
			false
	);
	CompleteAnnotationDescriptor<Cache, CacheAnnotation> CACHE = new CompleteAnnotationDescriptor<>(
			Cache.class,
			CacheAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<Checks, ChecksAnnotation> CHECKS = new CompleteAnnotationDescriptor<>(
			Checks.class,
			ChecksAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<Check, CheckAnnotation> CHECK = new CompleteAnnotationDescriptor<>(
			Check.class,
			CheckAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false,
			CHECKS
	);
	CompleteAnnotationDescriptor<Collate, CollateAnnotation> COLLATE = new CompleteAnnotationDescriptor<>(
			Collate.class,
			CollateAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	CompleteAnnotationDescriptor<CollectionId, CollectionIdAnnotation> COLLECTION_ID = new CompleteAnnotationDescriptor<>(
			CollectionId.class,
			CollectionIdAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<CollectionIdJavaClass, CollectionIdJavaClassAnnotation> COLLECTION_ID_JAVA_CLASS = new CompleteAnnotationDescriptor<>(
			CollectionIdJavaClass.class,
			CollectionIdJavaClassAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	CompleteAnnotationDescriptor<CollectionIdJavaType, CollectionIdJavaTypeAnnotation> COLLECTION_ID_JAVA_TYPE = new CompleteAnnotationDescriptor<>(
			CollectionIdJavaType.class,
			CollectionIdJavaTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	CompleteAnnotationDescriptor<CollectionIdJdbcType, CollectionIdJdbcTypeAnnotation> COLLECTION_ID_JDBC_TYPE = new CompleteAnnotationDescriptor<>(
			CollectionIdJdbcType.class,
			CollectionIdJdbcTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	CompleteAnnotationDescriptor<CollectionIdJdbcTypeCode, CollectionIdJdbcTypeCodeAnnotation> COLLECTION_ID_JDBC_TYPE_CODE = new CompleteAnnotationDescriptor<>(
			CollectionIdJdbcTypeCode.class,
			CollectionIdJdbcTypeCodeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	CompleteAnnotationDescriptor<CollectionIdMutability, CollectionIdMutabilityAnnotation> COLLECTION_ID_MUTABILITY = new CompleteAnnotationDescriptor<>(
			CollectionIdMutability.class,
			CollectionIdMutabilityAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	CompleteAnnotationDescriptor<CollectionIdType, CollectionIdTypeAnnotation> COLLECTION_ID_TYPE = new CompleteAnnotationDescriptor<>(
			CollectionIdType.class,
			CollectionIdTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	CompleteAnnotationDescriptor<CollectionType, CollectionTypeAnnotation> COLLECTION_TYPE = new CompleteAnnotationDescriptor<>(
			CollectionType.class,
			CollectionTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	CompleteAnnotationDescriptor<CollectionClassification, CollectionClassificationXmlAnnotation> COLLECTION_CLASSIFICATION = new CompleteAnnotationDescriptor<>(
			CollectionClassification.class,
			CollectionClassificationXmlAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<CollectionTypeRegistrations, CollectionTypeRegistrationsAnnotation> COLLECTION_TYPE_REGISTRATIONS = new CompleteAnnotationDescriptor<>(
			CollectionTypeRegistrations.class,
			CollectionTypeRegistrationsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE, Kind.MODULE ),
			false
	);
	CompleteAnnotationDescriptor<CollectionTypeRegistration, CollectionTypeRegistrationAnnotation> COLLECTION_TYPE_REGISTRATION = new CompleteAnnotationDescriptor<>(
			CollectionTypeRegistration.class,
			CollectionTypeRegistrationAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE, Kind.MODULE ),
			false,
			COLLECTION_TYPE_REGISTRATIONS
	);
	CompleteAnnotationDescriptor<ColumnDefault, ColumnDefaultAnnotation> COLUMN_DEFAULT = new CompleteAnnotationDescriptor<>(
			ColumnDefault.class,
			ColumnDefaultAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<ColumnTransformers, ColumnTransformersAnnotation> COLUMN_TRANSFORMERS = new CompleteAnnotationDescriptor<>(
			ColumnTransformers.class,
			ColumnTransformersAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<ColumnTransformer, ColumnTransformerAnnotation> COLUMN_TRANSFORMER = new CompleteAnnotationDescriptor<>(
			ColumnTransformer.class,
			ColumnTransformerAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false,
			COLUMN_TRANSFORMERS
	);
	CompleteAnnotationDescriptor<CompositeType, CompositeTypeAnnotation> COMPOSITE_TYPE = new CompleteAnnotationDescriptor<>(
			CompositeType.class,
			CompositeTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	CompleteAnnotationDescriptor<CompositeTypeRegistrations, CompositeTypeRegistrationsAnnotation> COMPOSITE_TYPE_REGISTRATIONS = new CompleteAnnotationDescriptor<>(
			CompositeTypeRegistrations.class,
			CompositeTypeRegistrationsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE, Kind.MODULE ),
			false
	);
	CompleteAnnotationDescriptor<CompositeTypeRegistration, CompositeTypeRegistrationAnnotation> COMPOSITE_TYPE_REGISTRATION = new CompleteAnnotationDescriptor<>(
			CompositeTypeRegistration.class,
			CompositeTypeRegistrationAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE, Kind.MODULE ),
			false,
			COMPOSITE_TYPE_REGISTRATIONS
	);
	CompleteAnnotationDescriptor<ConcreteProxy, ConcreteProxyAnnotation> CONCRETE_PROXY = new CompleteAnnotationDescriptor<>(
			ConcreteProxy.class,
			ConcreteProxyAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<ConverterRegistrations, ConverterRegistrationsAnnotation> CONVERTER_REGISTRATIONS = new CompleteAnnotationDescriptor<>(
			ConverterRegistrations.class,
			ConverterRegistrationsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE, Kind.MODULE ),
			false
	);
	CompleteAnnotationDescriptor<ConverterRegistration, ConverterRegistrationAnnotation> CONVERTER_REGISTRATION = new CompleteAnnotationDescriptor<>(
			ConverterRegistration.class,
			ConverterRegistrationAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE, Kind.MODULE ),
			false,
			CONVERTER_REGISTRATIONS
	);
	CompleteAnnotationDescriptor<CreationTimestamp, CreationTimestampAnnotation> CREATION_TIMESTAMP = new CompleteAnnotationDescriptor<>(
			CreationTimestamp.class,
			CreationTimestampAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<CurrentTimestamp, CurrentTimestampAnnotation> CURRENT_TIMESTAMP = new CompleteAnnotationDescriptor<>(
			CurrentTimestamp.class,
			CurrentTimestampAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	CompleteAnnotationDescriptor<DefaultListSemantics, DefaultListSemanticsAnnotation> DEFAULT_LIST_SEMANTICS = new CompleteAnnotationDescriptor<>(
			DefaultListSemantics.class,
			DefaultListSemanticsAnnotation.class,
			EnumSet.of( Kind.PACKAGE, Kind.MODULE ),
			false
	);
	CompleteAnnotationDescriptor<DiscriminatorFormula, DiscriminatorFormulaAnnotation> DISCRIMINATOR_FORMULA = new CompleteAnnotationDescriptor<>(
			DiscriminatorFormula.class,
			DiscriminatorFormulaAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<DiscriminatorOptions, DiscriminatorOptionsAnnotation> DISCRIMINATOR_OPTIONS = new CompleteAnnotationDescriptor<>(
			DiscriminatorOptions.class,
			DiscriminatorOptionsAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<DynamicInsert, DynamicInsertAnnotation> DYNAMIC_INSERT = new CompleteAnnotationDescriptor<>(
			DynamicInsert.class,
			DynamicInsertAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<DynamicUpdate, DynamicUpdateAnnotation> DYNAMIC_UPDATE = new CompleteAnnotationDescriptor<>(
			DynamicUpdate.class,
			DynamicUpdateAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<EmbeddableInstantiator, EmbeddableInstantiatorAnnotation> EMBEDDABLE_INSTANTIATOR = new CompleteAnnotationDescriptor<>(
			EmbeddableInstantiator.class,
			EmbeddableInstantiatorAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS, Kind.ANNOTATION ),
			false
	);
	CompleteAnnotationDescriptor<EmbeddableInstantiatorRegistrations, EmbeddableInstantiatorRegistrationsAnnotation> EMBEDDABLE_INSTANTIATOR_REGISTRATIONS = new CompleteAnnotationDescriptor<>(
			EmbeddableInstantiatorRegistrations.class,
			EmbeddableInstantiatorRegistrationsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE, Kind.MODULE ),
			false
	);
	CompleteAnnotationDescriptor<EmbeddableInstantiatorRegistration, EmbeddableInstantiatorRegistrationAnnotation> EMBEDDABLE_INSTANTIATOR_REGISTRATION = new CompleteAnnotationDescriptor<>(
			EmbeddableInstantiatorRegistration.class,
			EmbeddableInstantiatorRegistrationAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE, Kind.MODULE ),
			false,
			EMBEDDABLE_INSTANTIATOR_REGISTRATIONS
	);
	CompleteAnnotationDescriptor<EmbeddedColumnNaming, EmbeddedColumnNamingAnnotation> EMBEDDED_COLUMN_NAMING = new CompleteAnnotationDescriptor<>(
			EmbeddedColumnNaming.class,
			EmbeddedColumnNamingAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<EmbeddedTable, EmbeddedTableAnnotation> EMBEDDED_TABLE = new CompleteAnnotationDescriptor<>(
			EmbeddedTable.class,
			EmbeddedTableAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<Fetch, FetchAnnotation> FETCH = new CompleteAnnotationDescriptor<>(
			Fetch.class,
			FetchAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<FetchProfiles, FetchProfilesAnnotation> FETCH_PROFILES = new CompleteAnnotationDescriptor<>(
			FetchProfiles.class,
			FetchProfilesAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false
	);
	CompleteAnnotationDescriptor<FetchProfile, FetchProfileAnnotation> FETCH_PROFILE = new CompleteAnnotationDescriptor<>(
			FetchProfile.class,
			FetchProfileAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false,
			FETCH_PROFILES
	);
	CompleteAnnotationDescriptor<Filters, FiltersAnnotation> FILTERS = new CompleteAnnotationDescriptor<>(
			Filters.class,
			FiltersAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<Filter, FilterAnnotation> FILTER = new CompleteAnnotationDescriptor<>(
			Filter.class,
			FilterAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false,
			FILTERS
	);
	CompleteAnnotationDescriptor<FilterDefs, FilterDefsAnnotation> FILTER_DEFS = new CompleteAnnotationDescriptor<>(
			FilterDefs.class,
			FilterDefsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false
	);
	CompleteAnnotationDescriptor<FilterDef, FilterDefAnnotation> FILTER_DEF = new CompleteAnnotationDescriptor<>(
			FilterDef.class,
			FilterDefAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false,
			FILTER_DEFS
	);
	CompleteAnnotationDescriptor<FilterJoinTables, FilterJoinTablesAnnotation> FILTER_JOIN_TABLES = new CompleteAnnotationDescriptor<>(
			FilterJoinTables.class,
			FilterJoinTablesAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<FilterJoinTable, FilterJoinTableAnnotation> FILTER_JOIN_TABLE = new CompleteAnnotationDescriptor<>(
			FilterJoinTable.class,
			FilterJoinTableAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false,
			FILTER_JOIN_TABLES
	);
	CompleteAnnotationDescriptor<Formula, FormulaAnnotation> FORMULA = new CompleteAnnotationDescriptor<>(
			Formula.class,
			FormulaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<FractionalSeconds, FractionalSecondsAnnotation> FRACTIONAL_SECONDS = new CompleteAnnotationDescriptor<>(
			FractionalSeconds.class,
			FractionalSecondsAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<Generated, GeneratedAnnotation> GENERATED = new CompleteAnnotationDescriptor<>(
			Generated.class,
			GeneratedAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<GeneratedColumn, GeneratedColumnAnnotation> GENERATED_COLUMN = new CompleteAnnotationDescriptor<>(
			GeneratedColumn.class,
			GeneratedColumnAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<GenericGenerator, GenericGeneratorAnnotation> GENERIC_GENERATOR = new CompleteAnnotationDescriptor<>(
			GenericGenerator.class,
			GenericGeneratorAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false
	);
	CompleteAnnotationDescriptor<HQLSelect, HQLSelectAnnotation> HQL_SELECT = new CompleteAnnotationDescriptor<>(
			HQLSelect.class,
			HQLSelectAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<IdGeneratorType, IdGeneratorTypeAnnotation> ID_GENERATOR_TYPE = new CompleteAnnotationDescriptor<>(
			IdGeneratorType.class,
			IdGeneratorTypeAnnotation.class,
			EnumSet.of( Kind.ANNOTATION ),
			false
	);
	CompleteAnnotationDescriptor<Immutable, ImmutableAnnotation> IMMUTABLE = new CompleteAnnotationDescriptor<>(
			Immutable.class,
			ImmutableAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<Imported, ImportedAnnotation> IMPORTED = new CompleteAnnotationDescriptor<>(
			Imported.class,
			ImportedAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<Instantiator, InstantiatorAnnotation> INSTANTIATOR = new CompleteAnnotationDescriptor<>(
			Instantiator.class,
			InstantiatorAnnotation.class,
			EnumSet.of( Kind.CONSTRUCTOR ),
			false
	);
	CompleteAnnotationDescriptor<JavaType, JavaTypeAnnotation> JAVA_TYPE = new CompleteAnnotationDescriptor<>(
			JavaType.class,
			JavaTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	CompleteAnnotationDescriptor<JavaTypeRegistrations, JavaTypeRegistrationsAnnotation> JAVA_TYPE_REGISTRATIONS = new CompleteAnnotationDescriptor<>(
			JavaTypeRegistrations.class,
			JavaTypeRegistrationsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE, Kind.MODULE ),
			true
	);
	CompleteAnnotationDescriptor<JavaTypeRegistration, JavaTypeRegistrationAnnotation> JAVA_TYPE_REGISTRATION = new CompleteAnnotationDescriptor<>(
			JavaTypeRegistration.class,
			JavaTypeRegistrationAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE, Kind.MODULE ),
			true,
			JAVA_TYPE_REGISTRATIONS
	);
	CompleteAnnotationDescriptor<JdbcType, JdbcTypeAnnotation> JDBC_TYPE = new CompleteAnnotationDescriptor<>(
			JdbcType.class,
			JdbcTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	CompleteAnnotationDescriptor<JdbcTypeCode, JdbcTypeCodeAnnotation> JDBC_TYPE_CODE = new CompleteAnnotationDescriptor<>(
			JdbcTypeCode.class,
			JdbcTypeCodeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	CompleteAnnotationDescriptor<JdbcTypeRegistrations, JdbcTypeRegistrationsAnnotation> JDBC_TYPE_REGISTRATIONS = new CompleteAnnotationDescriptor<>(
			JdbcTypeRegistrations.class,
			JdbcTypeRegistrationsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			true
	);
	CompleteAnnotationDescriptor<JdbcTypeRegistration, JdbcTypeRegistrationAnnotation> JDBC_TYPE_REGISTRATION = new CompleteAnnotationDescriptor<>(
			JdbcTypeRegistration.class,
			JdbcTypeRegistrationAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			true,
			JDBC_TYPE_REGISTRATIONS
	);
	CompleteAnnotationDescriptor<JoinColumnsOrFormulas, JoinColumnsOrFormulasAnnotation> JOIN_COLUMNS_OR_FORMULAS = new CompleteAnnotationDescriptor<>(
			JoinColumnsOrFormulas.class,
			JoinColumnsOrFormulasAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<JoinColumnOrFormula, JoinColumnOrFormulaAnnotation> JOIN_COLUMN_OR_FORMULA = new CompleteAnnotationDescriptor<>(
			JoinColumnOrFormula.class,
			JoinColumnOrFormulaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false,
			JOIN_COLUMNS_OR_FORMULAS
	);
	CompleteAnnotationDescriptor<JoinFormula, JoinFormulaAnnotation> JOIN_FORMULA = new CompleteAnnotationDescriptor<>(
			JoinFormula.class,
			JoinFormulaAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<LazyGroup, LazyGroupAnnotation> LAZY_GROUP = new CompleteAnnotationDescriptor<>(
			LazyGroup.class,
			LazyGroupAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<ListIndexBase, ListIndexBaseAnnotation> LIST_INDEX_BASE = new CompleteAnnotationDescriptor<>(
			ListIndexBase.class,
			ListIndexBaseAnnotation.class,
			EnumSet.allOf( Kind.class ),
			false
	);
	CompleteAnnotationDescriptor<ListIndexJavaType, ListIndexJavaTypeAnnotation> LIST_INDEX_JAVA_TYPE = new CompleteAnnotationDescriptor<>(
			ListIndexJavaType.class,
			ListIndexJavaTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	CompleteAnnotationDescriptor<ListIndexJdbcType, ListIndexJdbcTypeAnnotation> LIST_INDEX_JDBC_TYPE = new CompleteAnnotationDescriptor<>(
			ListIndexJdbcType.class,
			ListIndexJdbcTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	CompleteAnnotationDescriptor<ListIndexJdbcTypeCode, ListIndexJdbcTypeCodeAnnotation> LIST_INDEX_JDBC_TYPE_CODE = new CompleteAnnotationDescriptor<>(
			ListIndexJdbcTypeCode.class,
			ListIndexJdbcTypeCodeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	CompleteAnnotationDescriptor<ManyToAny, ManyToAnyAnnotation> MANY_TO_ANY = new CompleteAnnotationDescriptor<>(
			ManyToAny.class,
			ManyToAnyAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<MapKeyCompositeType, MapKeyCompositeTypeAnnotation> MAP_KEY_COMPOSITE_TYPE = new CompleteAnnotationDescriptor<>(
			MapKeyCompositeType.class,
			MapKeyCompositeTypeAnnotation.class,
			EnumSet.allOf( Kind.class ),
			false
	);
	CompleteAnnotationDescriptor<MapKeyJavaType, MapKeyJavaTypeAnnotation> MAP_KEY_JAVA_TYPE = new CompleteAnnotationDescriptor<>(
			MapKeyJavaType.class,
			MapKeyJavaTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	CompleteAnnotationDescriptor<MapKeyJdbcType, MapKeyJdbcTypeAnnotation> MAP_KEY_JDBC_TYPE = new CompleteAnnotationDescriptor<>(
			MapKeyJdbcType.class,
			MapKeyJdbcTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	CompleteAnnotationDescriptor<MapKeyJdbcTypeCode, MapKeyJdbcTypeCodeAnnotation> MAP_KEY_JDBC_TYPE_CODE = new CompleteAnnotationDescriptor<>(
			MapKeyJdbcTypeCode.class,
			MapKeyJdbcTypeCodeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	CompleteAnnotationDescriptor<MapKeyMutability, MapKeyMutabilityAnnotation> MAP_KEY_MUTABILITY = new CompleteAnnotationDescriptor<>(
			MapKeyMutability.class,
			MapKeyMutabilityAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	CompleteAnnotationDescriptor<MapKeyType, MapKeyTypeAnnotation> MAP_KEY_TYPE = new CompleteAnnotationDescriptor<>(
			MapKeyType.class,
			MapKeyTypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			true
	);
	CompleteAnnotationDescriptor<Mutability, MutabilityAnnotation> MUTABILITY = new CompleteAnnotationDescriptor<>(
			Mutability.class,
			MutabilityAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS, Kind.ANNOTATION ),
			true
	);
	CompleteAnnotationDescriptor<NamedEntityGraphs, NamedEntityGraphsAnnotation> NAMED_ENTITY_GRAPHS = new CompleteAnnotationDescriptor<>(
			NamedEntityGraphs.class,
			NamedEntityGraphsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE, Kind.MODULE ),
			false
	);
	CompleteAnnotationDescriptor<NamedEntityGraph, NamedEntityGraphAnnotation> NAMED_ENTITY_GRAPH = new CompleteAnnotationDescriptor<>(
			NamedEntityGraph.class,
			NamedEntityGraphAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false,
			NAMED_ENTITY_GRAPHS
	);
	CompleteAnnotationDescriptor<NamedNativeQueries, NamedNativeQueriesAnnotation> NAMED_NATIVE_QUERIES = new CompleteAnnotationDescriptor<>(
			NamedNativeQueries.class,
			NamedNativeQueriesAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false
	);
	CompleteAnnotationDescriptor<NamedNativeQuery, NamedNativeQueryAnnotation> NAMED_NATIVE_QUERY = new CompleteAnnotationDescriptor<>(
			NamedNativeQuery.class,
			NamedNativeQueryAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false,
			NAMED_NATIVE_QUERIES
	);
	CompleteAnnotationDescriptor<NamedQueries, NamedQueriesAnnotation> NAMED_QUERIES = new CompleteAnnotationDescriptor<>(
			NamedQueries.class,
			NamedQueriesAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false
	);
	CompleteAnnotationDescriptor<NamedQuery, NamedQueryAnnotation> NAMED_QUERY = new CompleteAnnotationDescriptor<>(
			NamedQuery.class,
			NamedQueryAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false,
			NAMED_QUERIES
	);
	CompleteAnnotationDescriptor<Nationalized, NationalizedAnnotation> NATIONALIZED = new CompleteAnnotationDescriptor<>(
			Nationalized.class,
			NationalizedAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION, Kind.PACKAGE ),
			false
	);
	CompleteAnnotationDescriptor<NativeGenerator, NativeGeneratorAnnotation> NATIVE_GENERATOR = new CompleteAnnotationDescriptor<>(
			NativeGenerator.class,
			NativeGeneratorAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS, Kind.PACKAGE, Kind.MODULE ),
			false
	);
	CompleteAnnotationDescriptor<NaturalId, NaturalIdAnnotation> NATURAL_ID = new CompleteAnnotationDescriptor<>(
			NaturalId.class,
			NaturalIdAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<NaturalIdCache, NaturalIdCacheAnnotation> NATURAL_ID_CACHE = new CompleteAnnotationDescriptor<>(
			NaturalIdCache.class,
			NaturalIdCacheAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<NaturalIdClass, NaturalIdClassAnnotation> NATURAL_ID_CLASS = new CompleteAnnotationDescriptor<>(
			NaturalIdClass.class,
			NaturalIdClassAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<NotFound, NotFoundAnnotation> NOT_FOUND = new CompleteAnnotationDescriptor<>(
			NotFound.class,
			NotFoundAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<OnDelete, OnDeleteAnnotation> ON_DELETE = new CompleteAnnotationDescriptor<>(
			OnDelete.class,
			OnDeleteAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<GeneratorOptimizer, GeneratorOptimizerAnnotation> OPTIMIZER = new CompleteAnnotationDescriptor<>(
			GeneratorOptimizer.class,
			GeneratorOptimizerAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<OptimisticLock, OptimisticLockAnnotation> OPTIMISTIC_LOCK = new CompleteAnnotationDescriptor<>(
			OptimisticLock.class,
			OptimisticLockAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<OptimisticLocking, OptimisticLockingAnnotation> OPTIMISTIC_LOCKING = new CompleteAnnotationDescriptor<>(
			OptimisticLocking.class,
			OptimisticLockingAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	// @ParamDef has @Target({}) - used as nested annotation, not directly on code elements
	CompleteAnnotationDescriptor<ParamDef, ParamDefAnnotation> PARAM_DEF = new CompleteAnnotationDescriptor<>(
			ParamDef.class,
			ParamDefAnnotation.class,
			EnumSet.noneOf( Kind.class ), // @Target({})
			false
	);
	// @Parameter has @Target({}) - used as nested annotation, not directly on code elements
	CompleteAnnotationDescriptor<Parameter, ParameterAnnotation> PARAMETER = new CompleteAnnotationDescriptor<>(
			Parameter.class,
			ParameterAnnotation.class,
			EnumSet.noneOf( Kind.class ), // @Target({})
			false
	);
	CompleteAnnotationDescriptor<Parent, ParentAnnotation> PARENT = new CompleteAnnotationDescriptor<>(
			Parent.class,
			ParentAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<PartitionKey, PartitionKeyAnnotation> PARTITION_KEY = new CompleteAnnotationDescriptor<>(
			PartitionKey.class,
			PartitionKeyAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<PropertyRef, PropertyRefAnnotation> PROPERTY_REF = new CompleteAnnotationDescriptor<>(
			PropertyRef.class,
			PropertyRefAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	CompleteAnnotationDescriptor<QueryCacheLayout, QueryCacheLayoutAnnotation> QUERY_CACHE_LAYOUT = new CompleteAnnotationDescriptor<>(
			QueryCacheLayout.class,
			QueryCacheLayoutAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<RowId, RowIdAnnotation> ROW_ID = new CompleteAnnotationDescriptor<>(
			RowId.class,
			RowIdAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<SecondaryRows, SecondaryRowsAnnotation> SECONDARY_ROWS = new CompleteAnnotationDescriptor<>(
			SecondaryRows.class,
			SecondaryRowsAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<SecondaryRow, SecondaryRowAnnotation> SECONDARY_ROW = new CompleteAnnotationDescriptor<>(
			SecondaryRow.class,
			SecondaryRowAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false,
			SECONDARY_ROWS
	);
	CompleteAnnotationDescriptor<SoftDelete, SoftDeleteAnnotation> SOFT_DELETE = new CompleteAnnotationDescriptor<>(
			SoftDelete.class,
			SoftDeleteAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE ),
			false
	);
	CompleteAnnotationDescriptor<SortComparator, SortComparatorAnnotation> SORT_COMPARATOR = new CompleteAnnotationDescriptor<>(
			SortComparator.class,
			SortComparatorAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<SortNatural, SortNaturalAnnotation> SORT_NATURAL = new CompleteAnnotationDescriptor<>(
			SortNatural.class,
			SortNaturalAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<SQLDeletes, SQLDeletesAnnotation> SQL_DELETES = new CompleteAnnotationDescriptor<>(
			SQLDeletes.class,
			SQLDeletesAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<SQLDelete, SQLDeleteAnnotation> SQL_DELETE = new CompleteAnnotationDescriptor<>(
			SQLDelete.class,
			SQLDeleteAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false,
			SQL_DELETES
	);
	CompleteAnnotationDescriptor<SQLDeleteAll, SQLDeleteAllAnnotation> SQL_DELETE_ALL = new CompleteAnnotationDescriptor<>(
			SQLDeleteAll.class,
			SQLDeleteAllAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	// @SqlFragmentAlias has @Target({}) - used as nested annotation, not directly on code elements
	CompleteAnnotationDescriptor<SqlFragmentAlias, SqlFragmentAliasAnnotation> SQL_FRAGMENT_ALIAS = new CompleteAnnotationDescriptor<>(
			SqlFragmentAlias.class,
			SqlFragmentAliasAnnotation.class,
			EnumSet.noneOf( Kind.class ), // @Target({})
			false
	);
	CompleteAnnotationDescriptor<SQLInserts, SQLInsertsAnnotation> SQL_INSERTS = new CompleteAnnotationDescriptor<>(
			SQLInserts.class,
			SQLInsertsAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<SQLInsert, SQLInsertAnnotation> SQL_INSERT = new CompleteAnnotationDescriptor<>(
			SQLInsert.class,
			SQLInsertAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false,
			SQL_INSERTS
	);
	CompleteAnnotationDescriptor<SQLOrder, SQLOrderAnnotation> SQL_ORDER = new CompleteAnnotationDescriptor<>(
			SQLOrder.class,
			SQLOrderAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<SQLRestriction, SQLRestrictionAnnotation> SQL_RESTRICTION = new CompleteAnnotationDescriptor<>(
			SQLRestriction.class,
			SQLRestrictionAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<SQLSelect, SQLSelectAnnotation> SQL_SELECT = new CompleteAnnotationDescriptor<>(
			SQLSelect.class,
			SQLSelectAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<SQLJoinTableRestriction, SQLJoinTableRestrictionAnnotation> SQL_JOIN_TABLE_RESTRICTION = new CompleteAnnotationDescriptor<>(
			SQLJoinTableRestriction.class,
			SQLJoinTableRestrictionAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<SQLUpdates, SQLUpdatesAnnotation> SQL_UPDATES = new CompleteAnnotationDescriptor<>(
			SQLUpdates.class,
			SQLUpdatesAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<SQLUpdate, SQLUpdateAnnotation> SQL_UPDATE = new CompleteAnnotationDescriptor<>(
			SQLUpdate.class,
			SQLUpdateAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false,
			SQL_UPDATES
	);
	CompleteAnnotationDescriptor<Struct, StructAnnotation> STRUCT = new CompleteAnnotationDescriptor<>(
			Struct.class,
			StructAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<Subselect, SubselectAnnotation> SUBSELECT = new CompleteAnnotationDescriptor<>(
			Subselect.class,
			SubselectAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<Synchronize, SynchronizeAnnotation> SYNCHRONIZE = new CompleteAnnotationDescriptor<>(
			Synchronize.class,
			SynchronizeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.CLASS ),
			false
	);
	CompleteAnnotationDescriptor<TargetEmbeddable, TargetEmbeddableAnnotation> TARGET_EMBEDDABLE = new CompleteAnnotationDescriptor<>(
			TargetEmbeddable.class,
			TargetEmbeddableAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.FIELD, Kind.METHOD ),
			false
	);
	CompleteAnnotationDescriptor<Temporal,TemporalAnnotation> TEMPORAL = new CompleteAnnotationDescriptor<>(
			Temporal.class,
			TemporalAnnotation.class,
			EnumSet.of( Kind.ANNOTATION, Kind.CLASS, Kind.FIELD, Kind.METHOD, Kind.PACKAGE ),
			false
	);
	CompleteAnnotationDescriptor<Temporal.HistoryPartitioning,HistoryPartitioningAnnotation> TEMPORAL_HISTORY_PARTITIONING =
			new CompleteAnnotationDescriptor<>(
					Temporal.HistoryPartitioning.class,
					HistoryPartitioningAnnotation.class,
					EnumSet.of( Kind.CLASS, Kind.FIELD, Kind.METHOD ),
					false
			);
	CompleteAnnotationDescriptor<Temporal.HistoryTable,HistoryTableAnnotation> TEMPORAL_HISTORY_TABLE =
			new CompleteAnnotationDescriptor<>(
					Temporal.HistoryTable.class,
					HistoryTableAnnotation.class,
					EnumSet.of( Kind.CLASS, Kind.FIELD, Kind.METHOD ),
					false
			);
	CompleteAnnotationDescriptor<Temporal.Excluded,ExcludedAnnotation> TEMPORAL_EXCLUDED =
			new CompleteAnnotationDescriptor<>(
					Temporal.Excluded.class,
					ExcludedAnnotation.class,
					EnumSet.of( Kind.FIELD, Kind.METHOD ),
					false
			);
	CompleteAnnotationDescriptor<TenantId, TenantIdAnnotation> TENANT_ID = new CompleteAnnotationDescriptor<>(
			TenantId.class,
			TenantIdAnnotation.class,
			EnumSet.of( Kind.FIELD, Kind.METHOD ),
			false
	);
	CompleteAnnotationDescriptor<TimeZoneColumn, TimeZoneColumnAnnotation> TIME_ZONE_COLUMN = new CompleteAnnotationDescriptor<>(
			TimeZoneColumn.class,
			TimeZoneColumnAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<TimeZoneStorage, TimeZoneStorageAnnotation> TIME_ZONE_STORAGE = new CompleteAnnotationDescriptor<>(
			TimeZoneStorage.class,
			TimeZoneStorageAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<Type, TypeAnnotation> TYPE = new CompleteAnnotationDescriptor<>(
			Type.class,
			TypeAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD, Kind.ANNOTATION ),
			false
	);
	CompleteAnnotationDescriptor<TypeBinderType, TypeBinderTypeAnnotation> TYPE_BINDER_TYPE = new CompleteAnnotationDescriptor<>(
			TypeBinderType.class,
			TypeBinderTypeAnnotation.class,
			EnumSet.of( Kind.ANNOTATION ),
			false
	);
	CompleteAnnotationDescriptor<TypeRegistrations, TypeRegistrationsAnnotation> TYPE_REGISTRATIONS = new CompleteAnnotationDescriptor<>(
			TypeRegistrations.class,
			TypeRegistrationsAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE, Kind.MODULE ),
			false
	);
	CompleteAnnotationDescriptor<TypeRegistration, TypeRegistrationAnnotation> TYPE_REGISTRATION = new CompleteAnnotationDescriptor<>(
			TypeRegistration.class,
			TypeRegistrationAnnotation.class,
			EnumSet.of( Kind.CLASS, Kind.ANNOTATION, Kind.PACKAGE, Kind.MODULE ),
			false,
			TYPE_REGISTRATIONS
	);
	CompleteAnnotationDescriptor<UpdateTimestamp, UpdateTimestampAnnotation> UPDATE_TIMESTAMP = new CompleteAnnotationDescriptor<>(
			UpdateTimestamp.class,
			UpdateTimestampAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<UuidGenerator, UuidGeneratorAnnotation> UUID_GENERATOR = new CompleteAnnotationDescriptor<>(
			UuidGenerator.class,
			UuidGeneratorAnnotation.class,
			EnumSet.of( Kind.METHOD, Kind.FIELD ),
			false
	);
	CompleteAnnotationDescriptor<ValueGenerationType, ValueGenerationTypeAnnotation> VALUE_GENERATION_TYPE = new CompleteAnnotationDescriptor<>(
			ValueGenerationType.class,
			ValueGenerationTypeAnnotation.class,
			EnumSet.of( Kind.ANNOTATION ),
			false
	);
	CompleteAnnotationDescriptor<View, ViewAnnotation> VIEW = new CompleteAnnotationDescriptor<>(
			View.class,
			ViewAnnotation.class,
			EnumSet.of( Kind.CLASS ),
			false
	);

	static void forEachAnnotation(Consumer<AnnotationDescriptor<? extends Annotation>> consumer) {
		OrmAnnotationHelper.forEachOrmAnnotation( HibernateAnnotations.class, consumer );
	}
}
