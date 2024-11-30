/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot;

import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.ColumnOrderingStrategy;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.type.BasicType;
import org.hibernate.usertype.UserType;


import jakarta.persistence.AttributeConverter;
import jakarta.persistence.SharedCacheMode;

/**
 * Contract for specifying various overrides to be used in metamodel building.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 *
 * @since 5.0
 */
public interface MetadataBuilder {
	/**
	 * Specify the implicit catalog name to apply to any unqualified database names.
	 * <p>
	 * Its default is defined by the {@value org.hibernate.cfg.AvailableSettings#DEFAULT_CATALOG}
	 * setting if using property-based configuration.
	 *
	 * @param implicitCatalogName The implicit catalog name
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#DEFAULT_CATALOG
	 */
	MetadataBuilder applyImplicitCatalogName(String implicitCatalogName);

	/**
	 * Specify the implicit schema name to apply to any unqualified database names.
	 * <p>
	 * Its default is defined by the {@value org.hibernate.cfg.AvailableSettings#DEFAULT_SCHEMA}
	 * setting if using property-based configuration.
	 *
	 * @param implicitSchemaName The implicit schema name
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#DEFAULT_SCHEMA
	 */
	MetadataBuilder applyImplicitSchemaName(String implicitSchemaName);

	/**
	 * Specify the {@link ImplicitNamingStrategy}.
	 * <p>
	 * Its default is defined by the {@value org.hibernate.cfg.AvailableSettings#IMPLICIT_NAMING_STRATEGY}
	 * setting if using property-based configuration.
	 *
	 * @param namingStrategy The {@link ImplicitNamingStrategy}
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#IMPLICIT_NAMING_STRATEGY
	 */
	MetadataBuilder applyImplicitNamingStrategy(ImplicitNamingStrategy namingStrategy);

	/**
	 * Specify the {@link PhysicalNamingStrategy}.
	 * <p>
	 * Its default is defined by the {@value org.hibernate.cfg.AvailableSettings#PHYSICAL_NAMING_STRATEGY}
	 * setting if using property-based configuration.
	 *
	 * @param namingStrategy The {@link PhysicalNamingStrategy}
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#PHYSICAL_NAMING_STRATEGY
	 */
	MetadataBuilder applyPhysicalNamingStrategy(PhysicalNamingStrategy namingStrategy);

	/**
	 * Specify the {@link ColumnOrderingStrategy}.
	 * <p>
	 * Its default is defined by the {@value org.hibernate.cfg.AvailableSettings#COLUMN_ORDERING_STRATEGY}
	 * setting if using property-based configuration.
	 *
	 * @param columnOrderingStrategy The {@link ColumnOrderingStrategy}
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#IMPLICIT_NAMING_STRATEGY
	 */
	MetadataBuilder applyColumnOrderingStrategy(ColumnOrderingStrategy columnOrderingStrategy);

	/**
	 * Specify the second-level cache mode.
	 * <p>
	 * Its default is defined by the {@code javax.persistence.sharedCache.mode} setting if using
	 * property-based configuration.
	 *
	 * @param cacheMode The cache mode.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see #applyAccessType
	 */
	MetadataBuilder applySharedCacheMode(SharedCacheMode cacheMode);

	/**
	 * Specify the second-level access-type to be used by default for entities and collections that define second-level
	 * caching, but do not specify a granular access-type.
	 * <p>
	 * Its default is defined by the {@value org.hibernate.cfg.AvailableSettings#DEFAULT_CACHE_CONCURRENCY_STRATEGY}
	 * setting if using property-based configuration.
	 *
	 * @param accessType The access-type to use as default.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#DEFAULT_CACHE_CONCURRENCY_STRATEGY
	 * @see #applySharedCacheMode(jakarta.persistence.SharedCacheMode)
	 */
	MetadataBuilder applyAccessType(AccessType accessType);

	/**
	 * Allows specifying a specific Jandex index to use for reading annotation information.
	 * <p>
	 * It's important to understand that if a Jandex index is passed in, it is expected that
	 * this Jandex index already contains all entries for all classes. No additional indexing
	 * will be done in this case.
	 *
	 * @apiNote Here for future expansion. At the moment the passed Jandex index is not used.
	 *
	 * @param jandexView The Jandex index to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	MetadataBuilder applyIndexView(Object jandexView);

	/**
	 * Specify the options to be used in performing scanning.
	 *
	 * @param scanOptions The scan options.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#SCANNER_DISCOVERY
	 */
	MetadataBuilder applyScanOptions(ScanOptions scanOptions);

	/**
	 * Consider this temporary as discussed on {@link ScanEnvironment}
	 *
	 * @param scanEnvironment The environment for scanning
	 *
	 * @return {@code this}, for method chaining
	 */
	MetadataBuilder applyScanEnvironment(ScanEnvironment scanEnvironment);

	/**
	 * Specify a particular Scanner instance to use.
	 * <p>
	 * Its default is defined by the {@value org.hibernate.cfg.AvailableSettings#SCANNER}
	 * setting if using property-based configuration.
	 *
	 * @param scanner The scanner to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#SCANNER
	 */
	MetadataBuilder applyScanner(Scanner scanner);

	/**
	 * Specify a particular ArchiveDescriptorFactory instance to use in scanning.
	 * <p>
	 * Its default is defined by the {@value org.hibernate.cfg.AvailableSettings#SCANNER_ARCHIVE_INTERPRETER}
	 * setting if using property-based configuration.
	 *
	 * @param factory The ArchiveDescriptorFactory to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#SCANNER_ARCHIVE_INTERPRETER
	 */
	MetadataBuilder applyArchiveDescriptorFactory(ArchiveDescriptorFactory factory);

	MetadataBuilder applyImplicitListSemantics(CollectionClassification classification);

	/**
	 * Should we process or ignore explicitly defined discriminators in the case
	 * of joined subclasses? The legacy behavior of Hibernate was to ignore the
	 * discriminator annotations because Hibernate (unlike some providers) does
	 * not need discriminators to determine the concrete type when it comes to
	 * joined inheritance.  However, for portability reasons we do now allow using
	 * explicit discriminators along with joined inheritance.  It is configurable
	 * though to support legacy apps.
	 * <p>
	 * Its default is defined by the {@value org.hibernate.cfg.AvailableSettings#IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS}
	 * setting if using property-based configuration.
	 *
	 * @param enabled Should processing (not ignoring) explicit discriminators be
	 * enabled?
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
	 */
	MetadataBuilder enableExplicitDiscriminatorsForJoinedSubclassSupport(boolean enabled);

	/**
	 * Similarly to {@link #enableExplicitDiscriminatorsForJoinedSubclassSupport},
	 * but here how should we treat joined inheritance when there is no explicitly
	 * defined discriminator annotations?  If enabled, we will handle joined
	 * inheritance with no explicit discriminator annotations by implicitly
	 * creating one (following the JPA implicit naming rules).
	 * <p>
	 * Again the premise here is JPA portability, bearing in mind that some
	 * JPA provider need these discriminators.
	 * <p>
	 * Its default is defined by the
	 * {@value org.hibernate.cfg.AvailableSettings#IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS}
	 * setting if using property-based configuration.
	 *
	 * @param enabled Should we implicitly create discriminator for joined
	 * inheritance if one is not explicitly mentioned?
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
	 */
	MetadataBuilder enableImplicitDiscriminatorsForJoinedSubclassSupport(boolean enabled);

	/**
	 * For entities which do not explicitly say, should we force discriminators into
	 * SQL selects?  The (historical) default is {@code false}
	 * <p>
	 * Its default is defined by the {@value org.hibernate.cfg.AvailableSettings#FORCE_DISCRIMINATOR_IN_SELECTS_BY_DEFAULT}
	 * setting if using property-based configuration.
	 *
	 * @param supported {@code true} indicates we will force the discriminator into the select;
	 * {@code false} indicates we will not.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#FORCE_DISCRIMINATOR_IN_SELECTS_BY_DEFAULT
	 */
	MetadataBuilder enableImplicitForcingOfDiscriminatorsInSelect(boolean supported);

	/**
	 * Should nationalized variants of character data be used in the database types?  For example, should
	 * {@code NVARCHAR} be used instead of {@code VARCHAR}?  {@code NCLOB} instead of {@code CLOB}?
	 * <p>
	 * Its default is defined by the {@value org.hibernate.cfg.AvailableSettings#USE_NATIONALIZED_CHARACTER_DATA}
	 * setting if using property-based configuration.
	 *
	 * @param enabled {@code true} says to use nationalized variants; {@code false}
	 * says to use the non-nationalized variants.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#USE_NATIONALIZED_CHARACTER_DATA
	 */
	MetadataBuilder enableGlobalNationalizedCharacterDataSupport(boolean enabled);

	/**
	 * Specify an additional or overridden basic type mapping.
	 *
	 * @param type The type addition or override.
	 *
	 * @return {@code this}, for method chaining
	 */
	MetadataBuilder applyBasicType(BasicType<?> type);

	/**
	 * Specify an additional or overridden basic type mapping supplying specific
	 * registration keys.
	 *
	 * @param type The type addition or override.
	 * @param keys The keys under which to register the basic type.
	 *
	 * @return {@code this}, for method chaining
	 */
	MetadataBuilder applyBasicType(BasicType<?> type, String... keys);

	/**
	 * Register an additional or overridden custom type mapping.
	 *
	 * @param type The custom type
	 * @param keys The keys under which to register the custom type.
	 *
	 * @return {@code this}, for method chaining
	 */
	MetadataBuilder applyBasicType(UserType<?> type, String... keys);

	/**
	 * Apply an explicit {@link TypeContributor}
	 * (implicit application via {@link java.util.ServiceLoader} will still happen too)
	 *
	 * @param typeContributor The contributor to apply
	 *
	 * @return {@code this}, for method chaining
	 */
	MetadataBuilder applyTypes(TypeContributor typeContributor);

	/**
	 * Apply a {@link CacheRegionDefinition} to be applied to an entity, collection,
	 * or query while building the {@link Metadata} object.
	 *
	 * @param cacheRegionDefinition The cache region definition to apply
	 *
	 * @return {@code this}, for method chaining
	 */
	MetadataBuilder applyCacheRegionDefinition(CacheRegionDefinition cacheRegionDefinition);

	/**
	 * Apply a {@link ClassLoader} for use while building the {@link Metadata}.
	 * <p>
	 * Ideally we should avoid accessing {@code ClassLoader}s when perform 1st phase of bootstrap.
	 * This is a {@code ClassLoader} that can be used in cases where we absolutely must.
	 * <p>
	 * In EE managed environments, this is the {@code ClassLoader} mandated by
	 * {@link jakarta.persistence.spi.PersistenceUnitInfo#getNewTempClassLoader()}.
	 * This {@code ClassLoader} is discarded by the container afterward, the idea being that the
	 * {@link Class} can still be enhanced in the application {@code ClassLoader}.
	 * <p>
	 * In other environments, pass a {@code ClassLoader} that performs the same function, if desired.
	 *
	 * @param tempClassLoader {@code ClassLoader} for use while building the {@code Metadata}
	 *
	 * @return {@code this}, for method chaining
	 */
	MetadataBuilder applyTempClassLoader(ClassLoader tempClassLoader);

	/**
	 * Apply an explicit {@link FunctionContributor}
	 * (implicit application via {@link java.util.ServiceLoader} will still happen too)
	 *
	 * @param functionContributor The contributor to apply
	 *
	 * @return {@code this}, for method chaining
	 */
	MetadataBuilder applyFunctions(FunctionContributor functionContributor);

	/**
	 * Contribute a {@link SqmFunctionDescriptor} to HQL.
	 *
	 * @see org.hibernate.dialect.function.StandardSQLFunction
	 */
	MetadataBuilder applySqlFunction(String functionName, SqmFunctionDescriptor function);

	/**
	 * Contribute an {@link AuxiliaryDatabaseObject}.
	 */
	MetadataBuilder applyAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject);

	/**
	 * Adds an AttributeConverter by a {@link ConverterDescriptor}
	 *
	 * @param descriptor The descriptor
	 *
	 * @return {@code this} for method chaining
	 *
	 */
	MetadataBuilder applyAttributeConverter(ConverterDescriptor descriptor);

	/**
	 * Adds an AttributeConverter by its Class.
	 *
	 * @param attributeConverterClass The AttributeConverter class.
	 *
	 * @return {@code this} for method chaining
	 */
	<O,R> MetadataBuilder applyAttributeConverter(Class<? extends AttributeConverter<O,R>> attributeConverterClass);

	/**
	 * Adds an {@link AttributeConverter} by {@code Class},
	 * explicitly indicating whether to auto-apply it.
	 *
	 * @param attributeConverterClass The AttributeConverter class.
	 * @param autoApply Should the AttributeConverter be auto applied to property types as specified
	 * by its "entity attribute" parameterized type?
	 *
	 * @return {@code this} for method chaining
	 */
	<O,R> MetadataBuilder applyAttributeConverter(Class<? extends AttributeConverter<O,R>> attributeConverterClass, boolean autoApply);

	/**
	 * Adds an AttributeConverter instance.
	 *
	 * @param attributeConverter The AttributeConverter instance.
	 *
	 * @return {@code this} for method chaining
	 */
	<O,R> MetadataBuilder applyAttributeConverter(AttributeConverter<O,R> attributeConverter);

	/**
	 * Adds an {@link AttributeConverter} instance,
	 * explicitly indicating whether to auto-apply it.
	 *
	 * @param attributeConverter The AttributeConverter instance.
	 * @param autoApply Should the AttributeConverter be auto applied to property types as specified
	 * by its "entity attribute" parameterized type?
	 *
	 * @return {@code this} for method chaining
	 */
	MetadataBuilder applyAttributeConverter(AttributeConverter<?,?> attributeConverter, boolean autoApply);

	/**
	 * Actually build the metamodel
	 *
	 * @return The built metadata.
	 */
	Metadata build();
}
