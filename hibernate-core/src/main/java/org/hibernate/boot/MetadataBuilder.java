/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot;

import javax.persistence.AttributeConverter;
import javax.persistence.SharedCacheMode;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.model.IdGeneratorStrategyInterpreter;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.AttributeConverterDefinition;
import org.hibernate.cfg.MetadataSourceType;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.type.BasicType;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;

import org.jboss.jandex.IndexView;

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
	 * <p/>
	 * Its default is defined by the {@link org.hibernate.cfg.AvailableSettings#DEFAULT_CATALOG}
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
	 * <p/>
	 * Its default is defined by the {@link org.hibernate.cfg.AvailableSettings#DEFAULT_SCHEMA}
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
	 * Specify the ImplicitNamingStrategy to use in building the Metadata.
	 * <p/>
	 * Its default is defined by the {@link org.hibernate.cfg.AvailableSettings#IMPLICIT_NAMING_STRATEGY}
	 * setting if using property-based configuration.
	 *
	 * @param namingStrategy The ImplicitNamingStrategy to apply
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#IMPLICIT_NAMING_STRATEGY
	 */
	MetadataBuilder applyImplicitNamingStrategy(ImplicitNamingStrategy namingStrategy);

	/**
	 * Specify the PhysicalNamingStrategy to use in building the Metadata.
	 * <p/>
	 * Its default is defined by the {@link org.hibernate.cfg.AvailableSettings#PHYSICAL_NAMING_STRATEGY}
	 * setting if using property-based configuration.
	 *
	 * @param namingStrategy The PhysicalNamingStrategy to apply
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#PHYSICAL_NAMING_STRATEGY
	 */
	MetadataBuilder applyPhysicalNamingStrategy(PhysicalNamingStrategy namingStrategy);

	/**
	 * Specify the second-level cache mode to be used.  This is the cache mode in terms of whether or
	 * not to cache.
	 * <p/>
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
	 * <p/>
	 * Its default is defined by the {@link org.hibernate.cfg.AvailableSettings#DEFAULT_CACHE_CONCURRENCY_STRATEGY}
	 * setting if using property-based configuration.
	 *
	 * @param accessType The access-type to use as default.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#DEFAULT_CACHE_CONCURRENCY_STRATEGY
	 * @see #applySharedCacheMode(javax.persistence.SharedCacheMode)
	 */
	MetadataBuilder applyAccessType(AccessType accessType);

	/**
	 * Allows specifying a specific Jandex index to use for reading annotation information.
	 * <p/>
	 * It is <i>important</i> to understand that if a Jandex index is passed in, it is expected that
	 * this Jandex index already contains all entries for all classes.  No additional indexing will be
	 * done in this case.
	 * <p/>
	 * NOTE : Here for future expansion.  At the moment the passed Jandex index is not used.
	 *
	 * @param jandexView The Jandex index to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	MetadataBuilder applyIndexView(IndexView jandexView);

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
	 * <p/>
	 * Its default is defined by the {@link org.hibernate.cfg.AvailableSettings#SCANNER}
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
	 * <p/>
	 * Its default is defined by the {@link org.hibernate.cfg.AvailableSettings#SCANNER_ARCHIVE_INTERPRETER}
	 * setting if using property-based configuration.
	 *
	 * @param factory The ArchiveDescriptorFactory to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#SCANNER_ARCHIVE_INTERPRETER
	 */
	MetadataBuilder applyArchiveDescriptorFactory(ArchiveDescriptorFactory factory);

	/**
	 * Should we enable support for the "new" (since 3.2) identifier generator mappings for
	 * handling:<ul>
	 *     <li>{@link javax.persistence.GenerationType#SEQUENCE}</li>
	 *     <li>{@link javax.persistence.GenerationType#IDENTITY}</li>
	 *     <li>{@link javax.persistence.GenerationType#TABLE}</li>
	 *     <li>{@link javax.persistence.GenerationType#AUTO}</li>
	 * </ul>
	 * <p/>
	 * Its default is defined by the {@link org.hibernate.cfg.AvailableSettings#USE_NEW_ID_GENERATOR_MAPPINGS}
	 * setting if using property-based configuration.
	 *
	 *
	 * @param enable {@code true} to enable; {@code false} to disable; don't call for
	 * default.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#USE_NEW_ID_GENERATOR_MAPPINGS
	 */
	MetadataBuilder enableNewIdentifierGeneratorSupport(boolean enable);

	/**
	 * Should we process or ignore explicitly defined discriminators in the case
	 * of joined-subclasses.  The legacy behavior of Hibernate was to ignore the
	 * discriminator annotations because Hibernate (unlike some providers) does
	 * not need discriminators to determine the concrete type when it comes to
	 * joined inheritance.  However, for portability reasons we do now allow using
	 * explicit discriminators along with joined inheritance.  It is configurable
	 * though to support legacy apps.
	 * <p/>
	 * Its default is defined by the {@link org.hibernate.cfg.AvailableSettings#IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS}
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
	 * <p/>
	 * Again the premise here is JPA portability, bearing in mind that some
	 * JPA provider need these discriminators.
	 * <p/>
	 * Its default is defined by the {@link org.hibernate.cfg.AvailableSettings#IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS}
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
	 * <p/>
	 * Its default is defined by the {@link org.hibernate.cfg.AvailableSettings#FORCE_DISCRIMINATOR_IN_SELECTS_BY_DEFAULT}
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
	 * <p/>
	 * Its default is defined by the {@link org.hibernate.cfg.AvailableSettings#USE_NATIONALIZED_CHARACTER_DATA}
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
	MetadataBuilder applyBasicType(BasicType type);

	/**
	 * Specify an additional or overridden basic type mapping supplying specific
	 * registration keys.
	 *
	 * @param type The type addition or override.
	 * @param keys The keys under which to register the basic type.
	 *
	 * @return {@code this}, for method chaining
	 */
	MetadataBuilder applyBasicType(BasicType type, String... keys);

	/**
	 * Register an additional or overridden custom type mapping.
	 *
	 * @param type The custom type
	 * @param keys The keys under which to register the custom type.
	 *
	 * @return {@code this}, for method chaining
	 */
	MetadataBuilder applyBasicType(UserType type, String... keys);

	/**
	 * Register an additional or overridden composite custom type mapping.
	 *
	 * @param type The composite custom type
	 * @param keys The keys under which to register the composite custom type.
	 *
	 * @return {@code this}, for method chaining
	 */
	MetadataBuilder applyBasicType(CompositeUserType type, String... keys);

	/**
	 * Apply an explicit TypeContributor (implicit application via ServiceLoader will still happen too)
	 *
	 * @param typeContributor The contributor to apply
	 *
	 * @return {@code this}, for method chaining
	 */
	MetadataBuilder applyTypes(TypeContributor typeContributor);

	/**
	 * Apply a CacheRegionDefinition to be applied to an entity, collection or query while building the
	 * Metadata object.
	 *
	 * @param cacheRegionDefinition The cache region definition to apply
	 *
	 * @return {@code this}, for method chaining
	 */
	MetadataBuilder applyCacheRegionDefinition(CacheRegionDefinition cacheRegionDefinition);

	/**
	 * Apply a ClassLoader for use while building the Metadata.
	 * <p/>
	 * Ideally we should avoid accessing ClassLoaders when perform 1st phase of bootstrap.  This
	 * is a ClassLoader that can be used in cases when we have to.  IN EE managed environments, this
	 * is the ClassLoader mandated by
	 * {@link javax.persistence.spi.PersistenceUnitInfo#getNewTempClassLoader()}.  This ClassLoader
	 * is thrown away by the container afterwards.  The idea being that the Class can still be enhanced
	 * in the application ClassLoader.  In other environments, pass a ClassLoader that performs the
	 * same function if desired.
	 *
	 * @param tempClassLoader ClassLoader for use during building the Metadata
	 *
	 * @return {@code this}, for method chaining
	 */
	MetadataBuilder applyTempClassLoader(ClassLoader tempClassLoader);

	/**
	 * Apply a specific ordering to the processing of sources.  Note that unlike most
	 * of the methods on this contract that deal with multiple values internally, this
	 * one *replaces* any already set (its more a setter) instead of adding to.
	 * <p/>
	 * Its default is defined by the {@link org.hibernate.cfg.AvailableSettings#ARTIFACT_PROCESSING_ORDER}
	 * setting if using property-based configuration.
	 *
	 * @param sourceTypes The types, in the order they should be processed
	 *
	 * @return {@code this} for method chaining
	 *
	 * @see org.hibernate.cfg.AvailableSettings#ARTIFACT_PROCESSING_ORDER
	 */
	MetadataBuilder applySourceProcessOrdering(MetadataSourceType... sourceTypes);

	MetadataBuilder applySqlFunction(String functionName, SQLFunction function);

	MetadataBuilder applyAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject);

	/**
	 * Adds an AttributeConverter by an AttributeConverterDefinition
	 *
	 * @param definition The definition
	 *
	 * @return {@code this} for method chaining
	 *
	 * @deprecated (since 5.3) AttributeConverterDefinition forces early
	 * access to the AttributeConverter instance which precludes the
	 * possibility to resolve the converter from CDI, etc.  Instead use
	 * one of:
	 *
	 * 		* {@link #applyAttributeConverter(Class)}
	 * 		* {@link #applyAttributeConverter(Class, boolean)}
	 * 		* {@link #applyAttributeConverter(AttributeConverter)}
	 * 		* {@link #applyAttributeConverter(AttributeConverter, boolean)}
	 */
	@Deprecated
	MetadataBuilder applyAttributeConverter(AttributeConverterDefinition definition);

	/**
	 * Adds an AttributeConverter by its Class.
	 *
	 * @param attributeConverterClass The AttributeConverter class.
	 *
	 * @return {@code this} for method chaining
	 */
	MetadataBuilder applyAttributeConverter(Class<? extends AttributeConverter> attributeConverterClass);

	/**
	 * Adds an AttributeConverter by its Class plus a boolean indicating whether to auto apply it.
	 *
	 * @param attributeConverterClass The AttributeConverter class.
	 * @param autoApply Should the AttributeConverter be auto applied to property types as specified
	 * by its "entity attribute" parameterized type?
	 *
	 * @return {@code this} for method chaining
	 *
	 * @see org.hibernate.cfg.AttributeConverterDefinition#from(Class, boolean)
	 */
	MetadataBuilder applyAttributeConverter(Class<? extends AttributeConverter> attributeConverterClass, boolean autoApply);

	/**
	 * Adds an AttributeConverter instance.
	 *
	 * @param attributeConverter The AttributeConverter instance.
	 *
	 * @return {@code this} for method chaining
	 *
	 * @see org.hibernate.cfg.AttributeConverterDefinition#from(AttributeConverter)
	 */
	MetadataBuilder applyAttributeConverter(AttributeConverter attributeConverter);

	/**
	 * Adds an AttributeConverter instance, explicitly indicating whether to auto-apply.
	 *
	 * @param attributeConverter The AttributeConverter instance.
	 * @param autoApply Should the AttributeConverter be auto applied to property types as specified
	 * by its "entity attribute" parameterized type?
	 *
	 * @return {@code this} for method chaining
	 *
	 * @see org.hibernate.cfg.AttributeConverterDefinition#from(AttributeConverter, boolean)
	 */
	MetadataBuilder applyAttributeConverter(AttributeConverter attributeConverter, boolean autoApply);

	MetadataBuilder applyIdGenerationTypeInterpreter(IdGeneratorStrategyInterpreter interpreter);


//	/**
//	 * Specify the resolve to be used in identifying the backing members of a
//	 * persistent attributes.
//	 *
//	 * @param resolver The resolver to use
//	 *
//	 * @return {@code this}, for method chaining
//	 */
//	public MetadataBuilder with(PersistentAttributeMemberResolver resolver);

	/**
	 * Allows unwrapping this builder as another, more specific type.
	 *
	 * @param type
	 * @param <T>
	 *
	 * @return The unwrapped builder.
	 */
	public <T extends MetadataBuilder> T unwrap(Class<T> type);

	/**
	 * Actually build the metamodel
	 *
	 * @return The built metadata.
	 */
	public Metadata build();
}
