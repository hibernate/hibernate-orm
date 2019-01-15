/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import java.util.List;
import java.util.Map;
import javax.persistence.SharedCacheMode;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.boot.AttributeConverterInfo;
import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.boot.model.IdGeneratorStrategyInterpreter;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.MetadataSourceType;
import org.hibernate.dialect.function.SQLFunction;

import org.jboss.jandex.IndexView;

/**
 * Describes the options used while building the Metadata object (during
 * {@link org.hibernate.boot.MetadataBuilder#build()} processing).
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
public interface MetadataBuildingOptions {
	/**
	 * Access to the service registry.
	 *
	 * @return The service registry
	 */
	StandardServiceRegistry getServiceRegistry();

	/**
	 * Access to the mapping defaults.
	 *
	 * @return The mapping defaults
	 */
	MappingDefaults getMappingDefaults();

	/**
	 * Access the list of BasicType registrations.  These are the BasicTypes explicitly
	 * registered via calls to:<ul>
	 *     <li>{@link org.hibernate.boot.MetadataBuilder#applyBasicType(org.hibernate.type.BasicType)}</li>
	 *     <li>{@link org.hibernate.boot.MetadataBuilder#applyBasicType(org.hibernate.type.BasicType, String[])}</li>
	 *     <li>{@link org.hibernate.boot.MetadataBuilder#applyBasicType(org.hibernate.usertype.UserType, java.lang.String[])}</li>
	 *     <li>{@link org.hibernate.boot.MetadataBuilder#applyBasicType(org.hibernate.usertype.CompositeUserType, java.lang.String[])}</li>
	 * </ul>
	 *
	 * @return The BasicType registrations
	 */
	List<BasicTypeRegistration> getBasicTypeRegistrations();

	/**
	 * Retrieve the Hibernate Commons Annotations ReflectionManager to use.
	 *
	 * @return The Hibernate Commons Annotations ReflectionManager to use.
	 *
	 * @deprecated Use {@link BootstrapContext#getReflectionManager()} instead,
	 * The plan is to remove first {@link MetadataBuildingOptions#getReflectionManager()}
	 * keeping {@link BootstrapContext#getReflectionManager()} till the migration from
	 * Hibernate Commons Annotations to Jandex.
	 *
	 */
	@Deprecated
	ReflectionManager getReflectionManager();

	/**
	 * Access to the Jandex index passed by call to
	 * {@link org.hibernate.boot.MetadataBuilder#applyIndexView(org.jboss.jandex.IndexView)}, if any.
	 *
	 * @return The Jandex index
	 *
	 * @deprecated  Use {@link BootstrapContext#getJandexView()} instead.
	 */
	@Deprecated
	IndexView getJandexView();

	/**
	 * Access to the options to be used for scanning
	 *
	 * @return The scan options
	 *
	 * @deprecated  Use {@link BootstrapContext#getScanOptions()} instead.
	 */
	@Deprecated
	ScanOptions getScanOptions();

	/**
	 * Access to the environment for scanning.  Consider this temporary; see discussion on
	 * {@link ScanEnvironment}
	 *
	 * @return The scan environment
	 *
	 * @deprecated  Use {@link BootstrapContext#getScanEnvironment()} instead.
	 */
	@Deprecated
	ScanEnvironment getScanEnvironment();

	/**
	 * Access to the Scanner to be used for scanning.  Can be:<ul>
	 *     <li>A Scanner instance</li>
	 *     <li>A Class reference to the Scanner implementor</li>
	 *     <li>A String naming the Scanner implementor</li>
	 * </ul>
	 *
	 * @return The scanner
	 *
	 *  @deprecated  Use {@link BootstrapContext#getScanner()} instead.
	 */
	@Deprecated
	Object getScanner();

	/**
	 * Access to the ArchiveDescriptorFactory to be used for scanning
	 *
	 * @return The ArchiveDescriptorFactory
	 *
	 * @deprecated Use {@link BootstrapContext#getArchiveDescriptorFactory()} instead.
	 */
	@Deprecated
	ArchiveDescriptorFactory getArchiveDescriptorFactory();

	/**
	 * Access the temporary ClassLoader passed to us as defined by
	 * {@link javax.persistence.spi.PersistenceUnitInfo#getNewTempClassLoader()}, if any.
	 *
	 * @return The tempo ClassLoader
	 *
	 *  @deprecated  Use {@link BootstrapContext#getJpaTempClassLoader()} instead.
	 */
	@Deprecated
	ClassLoader getTempClassLoader();

	ImplicitNamingStrategy getImplicitNamingStrategy();

	PhysicalNamingStrategy getPhysicalNamingStrategy();

	/**
	 * Access to the SharedCacheMode for determining whether we should perform second level
	 * caching or not.
	 *
	 * @return The SharedCacheMode
	 */
	SharedCacheMode getSharedCacheMode();

	/**
	 * Access to any implicit cache AccessType.
	 *
	 * @return The implicit cache AccessType
	 */
	AccessType getImplicitCacheAccessType();

	/**
	 * Access to the MultiTenancyStrategy for this environment.
	 *
	 * @return The MultiTenancyStrategy
	 */
	MultiTenancyStrategy getMultiTenancyStrategy();

	IdGeneratorStrategyInterpreter getIdGenerationTypeInterpreter();

	/**
	 * Access to all explicit cache region mappings.
	 *
	 * @return Explicit cache region mappings.
	 *
	 *  @deprecated  Use {@link BootstrapContext#getClassmateContext()} instead.
	 */
	@Deprecated
	List<CacheRegionDefinition> getCacheRegionDefinitions();

	/**
	 * Whether explicit discriminator declarations should be ignored for joined
	 * subclass style inheritance.
	 *
	 * @return {@code true} indicates they should be ignored; {@code false}
	 * indicates they should not be ignored.
	 *
	 * @see org.hibernate.boot.MetadataBuilder#enableExplicitDiscriminatorsForJoinedSubclassSupport
	 * @see org.hibernate.cfg.AvailableSettings#IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
	 */
	boolean ignoreExplicitDiscriminatorsForJoinedInheritance();

	/**
	 * Whether we should do discrimination implicitly joined subclass style inheritance when no
	 * discriminator info is provided.
	 *
	 * @return {@code true} indicates we should do discrimination; {@code false} we should not.
	 *
	 * @see org.hibernate.boot.MetadataBuilder#enableImplicitDiscriminatorsForJoinedSubclassSupport
	 * @see org.hibernate.cfg.AvailableSettings#IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS
	 */
	boolean createImplicitDiscriminatorsForJoinedInheritance();

	/**
	 * Whether we should implicitly force discriminators into SQL selects.  By default,
	 * Hibernate will not.  This can be specified per discriminator in the mapping as well.
	 *
	 * @return {@code true} indicates we should force the discriminator in selects for any mappings
	 * which do not say explicitly.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#FORCE_DISCRIMINATOR_IN_SELECTS_BY_DEFAULT
	 */
	boolean shouldImplicitlyForceDiscriminatorInSelect();

	/**
	 * Should we use nationalized variants of character data (e.g. NVARCHAR rather than VARCHAR)
	 * by default?
	 *
	 * @see org.hibernate.boot.MetadataBuilder#enableGlobalNationalizedCharacterDataSupport
	 * @see org.hibernate.cfg.AvailableSettings#USE_NATIONALIZED_CHARACTER_DATA
	 *
	 * @return {@code true} if nationalized character data should be used by default; {@code false} otherwise.
	 */
	boolean useNationalizedCharacterData();

	boolean isSpecjProprietarySyntaxEnabled();

	/**
	 * Retrieve the ordering in which sources should be processed.
	 *
	 * @return The order in which sources should be processed.
	 */
	List<MetadataSourceType> getSourceProcessOrdering();

	default String getSchemaCharset() {
		return null;
	}

	default boolean isXmlMappingEnabled() {
		return true;
	}

	/**
	 * Access to any SQL functions explicitly registered with the MetadataBuilder.  This
	 * does not include Dialect defined functions, etc.
	 *
	 * @return The SQLFunctions registered through MetadataBuilder
	 *
	 *  @deprecated  Use {@link BootstrapContext#getSqlFunctions()} instead.
	 */
	@Deprecated
	Map<String,SQLFunction> getSqlFunctions();

	/**
	 * Access to any AuxiliaryDatabaseObject explicitly registered with the MetadataBuilder.  This
	 * does not include AuxiliaryDatabaseObject defined in mappings.
	 *
	 * @return The AuxiliaryDatabaseObject registered through MetadataBuilder
	 *
	 * @deprecated Use {@link BootstrapContext#getAuxiliaryDatabaseObjectList()} instead.
	 */
	@Deprecated
	List<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjectList();

	/**
	 * Access to collected AttributeConverter definitions.
	 *
	 * @return The AttributeConverterInfo registered through MetadataBuilder
	 *
	 * @deprecated Use {@link BootstrapContext#getAttributeConverters()} instead
	 */
	@Deprecated
	List<AttributeConverterInfo> getAttributeConverters();
}
