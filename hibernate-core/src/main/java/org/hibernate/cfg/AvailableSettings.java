/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

import org.hibernate.jpa.LegacySpecHints;
import org.hibernate.jpa.SpecHints;

/**
 * Enumerates the configuration properties supported by Hibernate, including
 * properties defined by the JPA specification.
 * <p>
 * The settings defined here may be specified at configuration time:
 * <ul>
 *     <li>in a configuration file, for example, in {@code persistence.xml} or
 *         {@code hibernate.cfg.xml},
 *     <li>via {@link Configuration#setProperty(String, String)}, or
 *     <li>via {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder#applySetting(String, Object)}.
 * </ul>
 * <p>
 * Note that Hibernate does not distinguish between JPA-defined configuration
 * properties and "native" configuration properties. Any property listed here
 * may be used to configure Hibernate no matter what configuration mechanism
 * or bootstrap API is used.
 *
 * @author Steve Ebersole
 */
public interface AvailableSettings
		extends BatchSettings, BytecodeSettings, CacheSettings, EnvironmentSettings, FetchSettings,
		JdbcSettings, JpaComplianceSettings, ManagedBeanSettings, MappingSettings, MultiTenancySettings,
		PersistenceSettings, QuerySettings, SchemaToolingSettings, SessionEventSettings, StatisticsSettings,
		TransactionSettings, ValidationSettings {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Set a default value for the hint {@link SpecHints#HINT_SPEC_LOCK_SCOPE},
	 * used when the hint is not explicitly specified.
	 * <p>
	 * See JPA 2 sections 8.2.1.9 and 3.4.4.3
	 *
	 * @see SpecHints#HINT_SPEC_LOCK_SCOPE
	 */
	String JAKARTA_LOCK_SCOPE = SpecHints.HINT_SPEC_LOCK_SCOPE;

	/**
	 * Set a default value for the hint {@link SpecHints#HINT_SPEC_LOCK_TIMEOUT},
	 * used when the hint is not explicitly specified.
	 * <p>
	 * See JPA 2 sections 8.2.1.9 and 3.4.4.3
	 *
	 * @see SpecHints#HINT_SPEC_LOCK_TIMEOUT
	 */
	String JAKARTA_LOCK_TIMEOUT = SpecHints.HINT_SPEC_LOCK_TIMEOUT;

	/**
	 * @deprecated Use {@link #JAKARTA_LOCK_SCOPE} instead
	 */
	@Deprecated(since="6.3")
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_LOCK_SCOPE = LegacySpecHints.HINT_JAVAEE_LOCK_SCOPE;

	/**
	 * @deprecated Use {@link #JAKARTA_LOCK_TIMEOUT} instead
	 */
	@Deprecated(since="6.3")
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_LOCK_TIMEOUT = LegacySpecHints.HINT_JAVAEE_LOCK_TIMEOUT;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Hibernate settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	String CFG_XML_FILE = "hibernate.cfg_xml_file";
	String ORM_XML_FILES = "hibernate.orm_xml_files";
	String HBM_XML_FILES = "hibernate.hbm_xml_files";
	String LOADED_CLASSES = "hibernate.loaded_classes";

	/**
	 * Specifies a {@link org.hibernate.context.spi.CurrentSessionContext} for
	 * scoping the {@linkplain org.hibernate.SessionFactory#getCurrentSession()
	 * current session}, either:
	 * <ul>
	 *     <li>{@code jta}, {@code thread}, or {@code managed}, or
	 *     <li>the name of a class implementing
	 *     {@code org.hibernate.context.spi.CurrentSessionContext}.
	 * </ul>
	 * If this property is not set, but JTA support is enabled, then
	 * {@link org.hibernate.context.internal.JTASessionContext} is used
	 * by default.
	 *
	 * @see org.hibernate.SessionFactory#getCurrentSession()
	 * @see org.hibernate.context.spi.CurrentSessionContext
	 */
	String CURRENT_SESSION_CONTEXT_CLASS = "hibernate.current_session_context_class";

	/**
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyDelayedEntityLoaderCreations(boolean)
	 *
	 * @deprecated This setting no longer has any effect.
	 *
	 * @since 5.3
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	String DELAY_ENTITY_LOADER_CREATIONS = "hibernate.loader.delay_entity_loader_creations";

	/**
	 * Specifies how Hibernate should behave when multiple representations of the same
	 * persistent entity instance, that is, multiple detached objects with the same
	 * persistent identity, are encountered while cascading a
	 * {@link org.hibernate.Session#merge(Object) merge()} operation.
	 * <p>
	 * The possible values are:
	 * <ul>
	 *     <li>{@code disallow} (the default): throw {@link IllegalStateException} if
	 *         multiple copies of the same entity are encountered
	 *     <li>{@code allow}: perform the merge operation for every copy encountered,
	 *         making no attempt to reconcile conflicts (this may result in lost updates)
	 *     <li>{@code log}: (provided for testing only) perform the merge operation for
	 *         every copy encountered and log information about the copies. This setting
	 *         requires that {@code DEBUG} logging be enabled for
	 *         {@link org.hibernate.event.internal.EntityCopyAllowedLoggedObserver}.
	 * </ul>
	 * <p>
	 * Alternatively, the application may customize the behavior by providing a custom
	 * implementation of {@link org.hibernate.event.spi.EntityCopyObserver} and setting
	 * the property {@value #MERGE_ENTITY_COPY_OBSERVER} to the class name. This, in
	 * principle, allows the application program to specify rules for reconciling
	 * conflicts.
	 * <p>
	 * When this property is set to {@code allow} or {@code log}, Hibernate will merge
	 * each entity copy detected while cascading the merge operation. In the process of
	 * merging each entity copy, Hibernate will cascade the merge operation from each
	 * entity copy to its associations with {@link jakarta.persistence.CascadeType#MERGE}
	 * or {@link jakarta.persistence.CascadeType#ALL}. The entity state resulting from
	 * merging an entity copy will be overwritten when another entity copy is merged.
	 *
	 * @since 4.3
	 *
	 * @see org.hibernate.event.spi.EntityCopyObserver
	 */
	@SuppressWarnings("JavaDoc")
	String MERGE_ENTITY_COPY_OBSERVER = "hibernate.event.merge.entity_copy_observer";

	/**
	 * When enabled, specifies that all transactional resources should be immediately
	 * released when {@link org.hibernate.Session#close()} is called.
	 *
	 * @settingDefault {@code false} (not released), as per the JPA specification.
	 *
	 * @apiNote The legacy name of this setting is extremely misleading;
	 *          it has little to do with persistence contexts.
	 * @deprecated This is no longer useful and will be removed.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	String DISCARD_PC_ON_CLOSE = "hibernate.discard_pc_on_close";

	/**
	 * When enabled, specifies that the generated identifier of an entity is unset
	 * when the entity is {@linkplain org.hibernate.Session#remove(Object) deleted}.
	 * If the entity is versioned, the version is also reset to its default value.
	 *
	 * @settingDefault {@code false} - generated identifiers are not unset
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyIdentifierRollbackSupport(boolean)
	 */
	String USE_IDENTIFIER_ROLLBACK = "hibernate.use_identifier_rollback";

	/**
	 * Setting to identify a {@link org.hibernate.CustomEntityDirtinessStrategy} to use.
	 * May specify either a class name or an instance.
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyCustomEntityDirtinessStrategy
	 */
	String CUSTOM_ENTITY_DIRTINESS_STRATEGY = "hibernate.entity_dirtiness_strategy";

	/**
	 * Event listener configuration properties follow the pattern
	 * {@code hibernate.event.listener.eventType packageName.ClassName1, packageName.ClassName2}
	 */
	String EVENT_LISTENER_PREFIX = "hibernate.event.listener";
}
