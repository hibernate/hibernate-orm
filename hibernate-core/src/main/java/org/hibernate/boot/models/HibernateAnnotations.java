/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models;

import java.lang.annotation.Annotation;
import java.util.function.Consumer;

import org.hibernate.annotations.*;
import org.hibernate.boot.internal.Abstract;
import org.hibernate.boot.internal.AnyKeyType;
import org.hibernate.boot.internal.CollectionClassification;
import org.hibernate.boot.internal.Extends;
import org.hibernate.boot.internal.Target;
import org.hibernate.boot.models.categorize.internal.OrmAnnotationHelper;
import org.hibernate.models.spi.AnnotationDescriptor;

import static org.hibernate.models.internal.AnnotationHelper.createOrmDescriptor;

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
	AnnotationDescriptor<Any> ANY = createOrmDescriptor( Any.class );
	AnnotationDescriptor<AnyDiscriminator> ANY_DISCRIMINATOR = createOrmDescriptor( AnyDiscriminator.class );
	AnnotationDescriptor<AnyDiscriminatorValues> ANY_DISCRIMINATOR_VALUES = createOrmDescriptor( AnyDiscriminatorValues.class );
	AnnotationDescriptor<AnyDiscriminatorValue> ANY_DISCRIMINATOR_VALUE = createOrmDescriptor( AnyDiscriminatorValue.class, ANY_DISCRIMINATOR_VALUES );
	AnnotationDescriptor<AnyKeyJavaClass> ANY_KEY_JAVA_CLASS = createOrmDescriptor( AnyKeyJavaClass.class );
	AnnotationDescriptor<AnyKeyJavaType> ANY_KEY_JAVA_TYPE = createOrmDescriptor( AnyKeyJavaType.class );
	AnnotationDescriptor<AnyKeyJdbcType> ANY_KEY_JDBC_TYPE = createOrmDescriptor( AnyKeyJdbcType.class );
	AnnotationDescriptor<AnyKeyJdbcTypeCode> ANY_KEY_JDBC_TYPE_CODE = createOrmDescriptor( AnyKeyJdbcTypeCode.class );
	AnnotationDescriptor<AttributeAccessor> ATTRIBUTE_ACCESSOR = createOrmDescriptor( AttributeAccessor.class );
	AnnotationDescriptor<AttributeBinderType> ATTRIBUTE_BINDER_TYPE = createOrmDescriptor( AttributeBinderType.class );
	AnnotationDescriptor<Bag> BAG = createOrmDescriptor( Bag.class );
	AnnotationDescriptor<BatchSize> BATCH_SIZE = createOrmDescriptor( BatchSize.class );
	AnnotationDescriptor<Cache> CACHE = createOrmDescriptor( Cache.class );
	AnnotationDescriptor<Cascade> CASCADE = createOrmDescriptor( Cascade.class );
	AnnotationDescriptor<Checks> CHECKS = createOrmDescriptor( Checks.class );
	AnnotationDescriptor<Check> CHECK = createOrmDescriptor( Check.class, CHECKS );
	AnnotationDescriptor<CollectionId> COLLECTION_ID = createOrmDescriptor( CollectionId.class );
	AnnotationDescriptor<CollectionIdJavaType> COLLECTION_ID_JAVA_TYPE = createOrmDescriptor( CollectionIdJavaType.class );
	AnnotationDescriptor<CollectionIdJdbcType> COLLECTION_ID_JDBC_TYPE = createOrmDescriptor( CollectionIdJdbcType.class );
	AnnotationDescriptor<CollectionIdJdbcTypeCode> COLLECTION_ID_JDBC_TYPE_CODE = createOrmDescriptor( CollectionIdJdbcTypeCode.class );
	AnnotationDescriptor<CollectionIdMutability> COLLECTION_ID_MUTABILITY = createOrmDescriptor( CollectionIdMutability.class );
	AnnotationDescriptor<CollectionIdType> COLLECTION_ID_TYPE = createOrmDescriptor( CollectionIdType.class );
	AnnotationDescriptor<CollectionType> COLLECTION_TYPE = createOrmDescriptor( CollectionType.class );
	AnnotationDescriptor<CollectionTypeRegistrations> COLLECTION_TYPE_REGS = createOrmDescriptor( CollectionTypeRegistrations.class );
	AnnotationDescriptor<CollectionTypeRegistration> COLLECTION_TYPE_REG = createOrmDescriptor( CollectionTypeRegistration.class, COLLECTION_TYPE_REGS );
	AnnotationDescriptor<ColumnDefault> COLUMN_DEFAULT = createOrmDescriptor( ColumnDefault.class );
	AnnotationDescriptor<ColumnTransformers> COLUMN_TRANSFORMERS = createOrmDescriptor( ColumnTransformers.class );
	AnnotationDescriptor<ColumnTransformer> COLUMN_TRANSFORMER = createOrmDescriptor( ColumnTransformer.class, COLUMN_TRANSFORMERS );
	AnnotationDescriptor<Comment> COMMENT = createOrmDescriptor( Comment.class );
	AnnotationDescriptor<CompositeType> COMPOSITE_TYPE = createOrmDescriptor( CompositeType.class );
	AnnotationDescriptor<CompositeTypeRegistrations> COMPOSITE_TYPE_REGS = createOrmDescriptor( CompositeTypeRegistrations.class );
	AnnotationDescriptor<CompositeTypeRegistration> COMPOSITE_TYPE_REG = createOrmDescriptor( CompositeTypeRegistration.class, COMPOSITE_TYPE_REGS );
	AnnotationDescriptor<ConverterRegistrations> CONVERTER_REGS = createOrmDescriptor( ConverterRegistrations.class );
	AnnotationDescriptor<ConverterRegistration> CONVERTER_REG = createOrmDescriptor( ConverterRegistration.class, CONVERTER_REGS );
	AnnotationDescriptor<CreationTimestamp> CREATION_TIMESTAMP = createOrmDescriptor( CreationTimestamp.class );
	AnnotationDescriptor<CurrentTimestamp> CURRENT_TIMESTAMP = createOrmDescriptor( CurrentTimestamp.class );
	AnnotationDescriptor<DiscriminatorFormula> DISCRIMINATOR_FORMULA = createOrmDescriptor( DiscriminatorFormula.class );
	AnnotationDescriptor<DiscriminatorOptions> DISCRIMINATOR_OPTIONS = createOrmDescriptor( DiscriminatorOptions.class );
	AnnotationDescriptor<DynamicInsert> DYNAMIC_INSERT = createOrmDescriptor( DynamicInsert.class );
	AnnotationDescriptor<DynamicUpdate> DYNAMIC_UPDATE = createOrmDescriptor( DynamicUpdate.class );
	AnnotationDescriptor<EmbeddableInstantiator> EMBEDDABLE_INSTANTIATOR = createOrmDescriptor( EmbeddableInstantiator.class );
	AnnotationDescriptor<EmbeddableInstantiatorRegistrations> EMBEDDABLE_INSTANTIATOR_REGS = createOrmDescriptor( EmbeddableInstantiatorRegistrations.class );
	AnnotationDescriptor<EmbeddableInstantiatorRegistration> EMBEDDABLE_INSTANTIATOR_REG = createOrmDescriptor( EmbeddableInstantiatorRegistration.class, EMBEDDABLE_INSTANTIATOR_REGS );
	AnnotationDescriptor<Fetch> FETCH = createOrmDescriptor( Fetch.class );
	AnnotationDescriptor<FetchProfiles> FETCH_PROFILES = createOrmDescriptor( FetchProfiles.class );
	AnnotationDescriptor<FetchProfile> FETCH_PROFILE = createOrmDescriptor( FetchProfile.class, FETCH_PROFILES );
	AnnotationDescriptor<Filters> FILTERS = createOrmDescriptor( Filters.class );
	AnnotationDescriptor<Filter> FILTER = createOrmDescriptor( Filter.class, FILTERS );
	AnnotationDescriptor<FilterDefs> FILTER_DEFS = createOrmDescriptor( FilterDefs.class );
	AnnotationDescriptor<FilterDef> FILTER_DEF = createOrmDescriptor( FilterDef.class, FILTER_DEFS );
	AnnotationDescriptor<FilterJoinTables> FILTER_JOIN_TABLES = createOrmDescriptor( FilterJoinTables.class );
	AnnotationDescriptor<FilterJoinTable> FILTER_JOIN_TABLE = createOrmDescriptor( FilterJoinTable.class, FILTER_JOIN_TABLES );
	AnnotationDescriptor<ForeignKey> FOREIGN_KEY = createOrmDescriptor( ForeignKey.class );
	AnnotationDescriptor<Formula> FORMULA = createOrmDescriptor( Formula.class );
	AnnotationDescriptor<Generated> GENERATED = createOrmDescriptor( Generated.class );
	AnnotationDescriptor<GeneratedColumn> GENERATED_COLUMN = createOrmDescriptor( GeneratedColumn.class );
	AnnotationDescriptor<GeneratorType> GENERATOR_TYPE = createOrmDescriptor( GeneratorType.class );
	AnnotationDescriptor<GenericGenerators> GENERIC_GENERATORS = createOrmDescriptor( GenericGenerators.class );
	AnnotationDescriptor<GenericGenerator> GENERIC_GENERATOR = createOrmDescriptor( GenericGenerator.class, GENERIC_GENERATORS );
	AnnotationDescriptor<IdGeneratorType> ID_GENERATOR_TYPE = createOrmDescriptor( IdGeneratorType.class );
	AnnotationDescriptor<Immutable> IMMUTABLE = createOrmDescriptor( Immutable.class );
	AnnotationDescriptor<Imported> IMPORTED = createOrmDescriptor( Imported.class );
	AnnotationDescriptor<Index> INDEX = createOrmDescriptor( Index.class );
	AnnotationDescriptor<IndexColumn> INDEX_COLUMN = createOrmDescriptor( IndexColumn.class );
	AnnotationDescriptor<Instantiator> INSTANTIATOR = createOrmDescriptor( Instantiator.class );
	AnnotationDescriptor<JavaType> JAVA_TYPE = createOrmDescriptor( JavaType.class );
	AnnotationDescriptor<JavaTypeRegistrations> JAVA_TYPE_REGS = createOrmDescriptor( JavaTypeRegistrations.class );
	AnnotationDescriptor<JavaTypeRegistration> JAVA_TYPE_REG = createOrmDescriptor( JavaTypeRegistration.class, JAVA_TYPE_REGS );
	AnnotationDescriptor<JdbcType> JDBC_TYPE = createOrmDescriptor( JdbcType.class );
	AnnotationDescriptor<JdbcTypeCode> JDBC_TYPE_CODE = createOrmDescriptor( JdbcTypeCode.class );
	AnnotationDescriptor<JdbcTypeRegistrations> JDBC_TYPE_REGS = createOrmDescriptor( JdbcTypeRegistrations.class );
	AnnotationDescriptor<JdbcTypeRegistration> JDBC_TYPE_REG = createOrmDescriptor( JdbcTypeRegistration.class, JDBC_TYPE_REGS );
	AnnotationDescriptor<JoinColumnsOrFormulas> JOIN_COLUMNS_OR_FORMULAS = createOrmDescriptor( JoinColumnsOrFormulas.class );
	AnnotationDescriptor<JoinColumnOrFormula> JOIN_COLUMN_OR_FORMULA = createOrmDescriptor( JoinColumnOrFormula.class, JOIN_COLUMNS_OR_FORMULAS );
	AnnotationDescriptor<JoinFormula> JOIN_FORMULA = createOrmDescriptor( JoinFormula.class );
	AnnotationDescriptor<LazyCollection> LAZY_COLLECTION = createOrmDescriptor( LazyCollection.class );
	AnnotationDescriptor<LazyGroup> LAZY_GROUP = createOrmDescriptor( LazyGroup.class );
	AnnotationDescriptor<LazyToOne> LAZY_TO_ONE = createOrmDescriptor( LazyToOne.class );
	AnnotationDescriptor<ListIndexBase> LIST_INDEX_BASE = createOrmDescriptor( ListIndexBase.class );
	AnnotationDescriptor<ListIndexJavaType> LIST_INDEX_JAVA_TYPE = createOrmDescriptor( ListIndexJavaType.class );
	AnnotationDescriptor<ListIndexJdbcType> LIST_INDEX_JDBC_TYPE = createOrmDescriptor( ListIndexJdbcType.class );
	AnnotationDescriptor<ListIndexJdbcTypeCode> LIST_INDEX_JDBC_TYPE_CODE = createOrmDescriptor( ListIndexJdbcTypeCode.class );
	AnnotationDescriptor<Loader> LOADER = createOrmDescriptor( Loader.class );
	AnnotationDescriptor<ManyToAny> MANY_TO_ANY = createOrmDescriptor( ManyToAny.class );
	AnnotationDescriptor<MapKeyCompositeType> MAP_KEY_COMPOSITE_TYPE = createOrmDescriptor( MapKeyCompositeType.class );
	AnnotationDescriptor<MapKeyJavaType> MAP_KEY_JAVA_TYPE = createOrmDescriptor( MapKeyJavaType.class );
	AnnotationDescriptor<MapKeyJdbcType> MAP_KEY_JDBC_TYPE = createOrmDescriptor( MapKeyJdbcType.class );
	AnnotationDescriptor<MapKeyJdbcTypeCode> MAP_KEY_JDBC_TYPE_CODE = createOrmDescriptor( MapKeyJdbcTypeCode.class );
	AnnotationDescriptor<MapKeyMutability> MAP_KEY_MUTABILITY = createOrmDescriptor( MapKeyMutability.class );
	AnnotationDescriptor<MapKeyType> MAP_KEY_TYPE = createOrmDescriptor( MapKeyType.class );
	AnnotationDescriptor<Mutability> MUTABILITY = createOrmDescriptor( Mutability.class );
	AnnotationDescriptor<NamedNativeQueries> NAMED_NATIVE_QUERIES = createOrmDescriptor( NamedNativeQueries.class );
	AnnotationDescriptor<NamedNativeQuery> NAMED_NATIVE_QUERY = createOrmDescriptor( NamedNativeQuery.class, NAMED_NATIVE_QUERIES );
	AnnotationDescriptor<NamedQueries> NAMED_QUERIES = createOrmDescriptor( NamedQueries.class );
	AnnotationDescriptor<NamedQuery> NAMED_QUERY = createOrmDescriptor( NamedQuery.class, NAMED_QUERIES );
	AnnotationDescriptor<Nationalized> NATIONALIZED = createOrmDescriptor( Nationalized.class );
	AnnotationDescriptor<NaturalId> NATURAL_ID = createOrmDescriptor( NaturalId.class );
	AnnotationDescriptor<NaturalIdCache> NATURAL_ID_CACHE = createOrmDescriptor( NaturalIdCache.class );
	AnnotationDescriptor<NotFound> NOT_FOUND = createOrmDescriptor( NotFound.class );
	AnnotationDescriptor<OnDelete> ON_DELETE = createOrmDescriptor( OnDelete.class );
	AnnotationDescriptor<OptimisticLock> OPTIMISTIC_LOCK = createOrmDescriptor( OptimisticLock.class );
	AnnotationDescriptor<OptimisticLocking> OPTIMISTIC_LOCKING = createOrmDescriptor( OptimisticLocking.class );
	AnnotationDescriptor<OrderBy> ORDER_BY = createOrmDescriptor( OrderBy.class );
	AnnotationDescriptor<ParamDef> PARAM_DEF = createOrmDescriptor( ParamDef.class );
	AnnotationDescriptor<Parameter> PARAMETER = createOrmDescriptor( Parameter.class );
	AnnotationDescriptor<Parent> PARENT = createOrmDescriptor( Parent.class );
	AnnotationDescriptor<PartitionKey> PARTITION_KEY = createOrmDescriptor( PartitionKey.class );
	AnnotationDescriptor<Polymorphism> POLYMORPHISM = createOrmDescriptor( Polymorphism.class );
	AnnotationDescriptor<Proxy> PROXY = createOrmDescriptor( Proxy.class );
	AnnotationDescriptor<RowId> ROW_ID = createOrmDescriptor( RowId.class );
	AnnotationDescriptor<SecondaryRows> SECONDARY_ROWS = createOrmDescriptor( SecondaryRows.class );
	AnnotationDescriptor<SecondaryRow> SECONDARY_ROW = createOrmDescriptor( SecondaryRow.class, SECONDARY_ROWS );
	AnnotationDescriptor<SelectBeforeUpdate> SELECT_BEFORE_UPDATE = createOrmDescriptor( SelectBeforeUpdate.class );
	AnnotationDescriptor<SortComparator> SORT_COMPARATOR = createOrmDescriptor( SortComparator.class );
	AnnotationDescriptor<SortNatural> SORT_NATURAL = createOrmDescriptor( SortNatural.class );
	AnnotationDescriptor<Source> SOURCE = createOrmDescriptor( Source.class );
	AnnotationDescriptor<SQLDeletes> SQL_DELETES = createOrmDescriptor( SQLDeletes.class );
	AnnotationDescriptor<SQLDelete> SQL_DELETE = createOrmDescriptor( SQLDelete.class, SQL_DELETES );
	AnnotationDescriptor<SQLDeleteAll> SQL_DELETE_ALL = createOrmDescriptor( SQLDeleteAll.class );
	AnnotationDescriptor<SqlFragmentAlias> SQL_FRAGMENT_ALIAS = createOrmDescriptor( SqlFragmentAlias.class );
	AnnotationDescriptor<SQLInserts> SQL_INSERTS = createOrmDescriptor( SQLInserts.class );
	AnnotationDescriptor<SQLInsert> SQL_INSERT = createOrmDescriptor( SQLInsert.class, SQL_INSERTS );
	AnnotationDescriptor<SQLRestriction> SQL_RESTRICTION = createOrmDescriptor( SQLRestriction.class, SQL_INSERTS );
	AnnotationDescriptor<SQLJoinTableRestriction> SQL_RESTRICTION_JOIN_TABLE = createOrmDescriptor( SQLJoinTableRestriction.class, SQL_INSERTS );
	AnnotationDescriptor<SQLUpdates> SQL_UPDATES = createOrmDescriptor( SQLUpdates.class );
	AnnotationDescriptor<SQLUpdate> SQL_UPDATE = createOrmDescriptor( SQLUpdate.class, SQL_UPDATES );
	AnnotationDescriptor<Struct> STRUCT = createOrmDescriptor( Struct.class );
	AnnotationDescriptor<Subselect> SUBSELECT = createOrmDescriptor( Subselect.class );
	AnnotationDescriptor<Synchronize> SYNCHRONIZE = createOrmDescriptor( Synchronize.class );
	AnnotationDescriptor<Tables> TABLES = createOrmDescriptor( Tables.class );
	AnnotationDescriptor<Table> TABLE = createOrmDescriptor( Table.class, TABLES );
	AnnotationDescriptor<TenantId> TENANT_ID = createOrmDescriptor( TenantId.class );
	AnnotationDescriptor<TimeZoneColumn> TZ_COLUMN = createOrmDescriptor( TimeZoneColumn.class );
	AnnotationDescriptor<TimeZoneStorage> TZ_STORAGE = createOrmDescriptor( TimeZoneStorage.class );
	AnnotationDescriptor<Type> TYPE = createOrmDescriptor( Type.class );
	AnnotationDescriptor<TypeBinderType> TYPE_BINDER_TYPE = createOrmDescriptor( TypeBinderType.class );
	AnnotationDescriptor<TypeRegistrations> TYPE_REGS = createOrmDescriptor( TypeRegistrations.class );
	AnnotationDescriptor<TypeRegistration> TYPE_REG = createOrmDescriptor( TypeRegistration.class, TYPE_REGS );
	AnnotationDescriptor<UpdateTimestamp> UPDATE_TIMESTAMP = createOrmDescriptor( UpdateTimestamp.class );
	AnnotationDescriptor<UuidGenerator> UUID_GENERATOR = createOrmDescriptor( UuidGenerator.class );
	AnnotationDescriptor<ValueGenerationType> VALUE_GENERATION_TYPE = createOrmDescriptor( ValueGenerationType.class );
	AnnotationDescriptor<Where> WHERE = createOrmDescriptor( Where.class );
	AnnotationDescriptor<WhereJoinTable> WHERE_JOIN_TABLE = createOrmDescriptor( WhereJoinTable.class );

	AnnotationDescriptor<Abstract> ABSTRACT = createOrmDescriptor( Abstract.class );
	AnnotationDescriptor<AnyKeyType> ANY_KEY_TYPE = createOrmDescriptor( AnyKeyType.class );
	AnnotationDescriptor<CollectionClassification> COLLECTION_CLASSIFICATION = createOrmDescriptor( CollectionClassification.class );
	AnnotationDescriptor<Extends> EXTENDS = createOrmDescriptor( Extends.class );
	AnnotationDescriptor<Target> TARGET = createOrmDescriptor( Target.class );

	AnnotationDescriptor<DialectOverride.Checks> DIALECT_OVERRIDE_CHECKS = createOrmDescriptor( DialectOverride.Checks.class );
	AnnotationDescriptor<DialectOverride.Check> DIALECT_OVERRIDE_CHECK = createOrmDescriptor( DialectOverride.Check.class, DIALECT_OVERRIDE_CHECKS );
	AnnotationDescriptor<DialectOverride.OrderBys> DIALECT_OVERRIDE_ORDER_BYS = createOrmDescriptor( DialectOverride.OrderBys.class );
	AnnotationDescriptor<DialectOverride.OrderBy> DIALECT_OVERRIDE_ORDER_BY = createOrmDescriptor( DialectOverride.OrderBy.class, DIALECT_OVERRIDE_ORDER_BYS );
	AnnotationDescriptor<DialectOverride.ColumnDefaults> DIALECT_OVERRIDE_COLUMN_DEFAULTS = createOrmDescriptor( DialectOverride.ColumnDefaults.class );
	AnnotationDescriptor<DialectOverride.ColumnDefault> DIALECT_OVERRIDE_COLUMN_DEFAULT = createOrmDescriptor( DialectOverride.ColumnDefault.class, DIALECT_OVERRIDE_COLUMN_DEFAULTS );
	AnnotationDescriptor<DialectOverride.GeneratedColumns> DIALECT_OVERRIDE_GENERATED_COLUMNS = createOrmDescriptor( DialectOverride.GeneratedColumns.class );
	AnnotationDescriptor<DialectOverride.GeneratedColumn> DIALECT_OVERRIDE_GENERATED_COLUMN = createOrmDescriptor( DialectOverride.GeneratedColumn.class, DIALECT_OVERRIDE_GENERATED_COLUMNS );
	AnnotationDescriptor<DialectOverride.DiscriminatorFormulas> DIALECT_OVERRIDE_DISCRIMINATOR_FORMULAS = createOrmDescriptor( DialectOverride.DiscriminatorFormulas.class );
	AnnotationDescriptor<DialectOverride.DiscriminatorFormula> DIALECT_OVERRIDE_DISCRIMINATOR_FORMULA = createOrmDescriptor( DialectOverride.DiscriminatorFormula.class, DIALECT_OVERRIDE_DISCRIMINATOR_FORMULAS );
	AnnotationDescriptor<DialectOverride.Formulas> DIALECT_OVERRIDE_FORMULAS = createOrmDescriptor( DialectOverride.Formulas.class );
	AnnotationDescriptor<DialectOverride.Formula> DIALECT_OVERRIDE_FORMULA = createOrmDescriptor( DialectOverride.Formula.class, DIALECT_OVERRIDE_FORMULAS );
	AnnotationDescriptor<DialectOverride.JoinFormulas> DIALECT_OVERRIDE_JOIN_FORMULAS = createOrmDescriptor( DialectOverride.JoinFormulas.class );
	AnnotationDescriptor<DialectOverride.JoinFormula> DIALECT_OVERRIDE_JOIN_FORMULA = createOrmDescriptor( DialectOverride.JoinFormula.class, DIALECT_OVERRIDE_JOIN_FORMULAS );
	AnnotationDescriptor<DialectOverride.Wheres> DIALECT_OVERRIDE_WHERES = createOrmDescriptor( DialectOverride.Wheres.class );
	AnnotationDescriptor<DialectOverride.Where> DIALECT_OVERRIDE_WHERE = createOrmDescriptor( DialectOverride.Where.class, DIALECT_OVERRIDE_WHERES );
	AnnotationDescriptor<DialectOverride.FilterOverrides> DIALECT_OVERRIDE_FILTER_OVERRIDES = createOrmDescriptor( DialectOverride.FilterOverrides.class );
	AnnotationDescriptor<DialectOverride.Filters> DIALECT_OVERRIDE_FILTERS = createOrmDescriptor( DialectOverride.Filters.class, DIALECT_OVERRIDE_FILTER_OVERRIDES );
	AnnotationDescriptor<DialectOverride.FilterDefOverrides> DIALECT_OVERRIDE_FILTER_DEF_OVERRIDES = createOrmDescriptor( DialectOverride.FilterDefOverrides.class );
	AnnotationDescriptor<DialectOverride.FilterDefs> DIALECT_OVERRIDE_FILTER_DEFS = createOrmDescriptor( DialectOverride.FilterDefs.class, DIALECT_OVERRIDE_FILTER_DEF_OVERRIDES );
	AnnotationDescriptor<DialectOverride.Version> DIALECT_OVERRIDE_VERSION = createOrmDescriptor( DialectOverride.Version.class );

	AnnotationDescriptor<DialectOverride.SQLInserts> DIALECT_OVERRIDE_SQL_INSERTS = createOrmDescriptor( DialectOverride.SQLInserts.class );
	AnnotationDescriptor<DialectOverride.SQLInsert> DIALECT_OVERRIDE_SQL_INSERT = createOrmDescriptor( DialectOverride.SQLInsert.class, DIALECT_OVERRIDE_SQL_INSERTS );
	AnnotationDescriptor<DialectOverride.SQLUpdates> DIALECT_OVERRIDE_SQL_UPDATES = createOrmDescriptor( DialectOverride.SQLUpdates.class );
	AnnotationDescriptor<DialectOverride.SQLUpdate> DIALECT_OVERRIDE_SQL_UPDATE = createOrmDescriptor( DialectOverride.SQLUpdate.class, DIALECT_OVERRIDE_SQL_UPDATES );
	AnnotationDescriptor<DialectOverride.SQLDeletes> DIALECT_OVERRIDE_SQL_DELETES = createOrmDescriptor( DialectOverride.SQLDeletes.class );
	AnnotationDescriptor<DialectOverride.SQLDelete> DIALECT_OVERRIDE_SQL_DELETE = createOrmDescriptor( DialectOverride.SQLDelete.class, DIALECT_OVERRIDE_SQL_DELETES );
	AnnotationDescriptor<DialectOverride.SQLDeleteAlls> DIALECT_OVERRIDE_SQL_DELETE_ALLS = createOrmDescriptor( DialectOverride.SQLDeleteAlls.class );
	AnnotationDescriptor<DialectOverride.SQLDeleteAll> DIALECT_OVERRIDE_SQL_DELETE_ALL = createOrmDescriptor( DialectOverride.SQLDeleteAll.class, DIALECT_OVERRIDE_SQL_DELETE_ALLS );

	static void forEachAnnotation(Consumer<AnnotationDescriptor<? extends Annotation>> consumer) {
		OrmAnnotationHelper.forEachOrmAnnotation( HibernateAnnotations.class, consumer );
	}
}
