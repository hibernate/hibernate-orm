/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.hibernate.SessionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.CacheSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.JpaComplianceSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.cfg.StatisticsSettings;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.tool.schema.Action;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;

/**
 * Hibernate extension to the Jakarta Persistence PersistenceConfiguration contract.
 *
 * @author Steve Ebersole
 */
public class HibernatePersistenceConfiguration extends PersistenceConfiguration {
	/**
	 * Create a new empty configuration. An empty configuration does not
	 * typically hold enough information for successful invocation of
	 * {@link #createEntityManagerFactory()}.
	 *
	 * @param name the name of the persistence unit, which may be used by
	 * the persistence provider for logging and error reporting
	 */
	public HibernatePersistenceConfiguration(String name) {
		super( name );
	}

	@Override
	public SessionFactory createEntityManagerFactory() {
		return (SessionFactory) super.createEntityManagerFactory();
	}

	/**
	 * Name of the JDBC driver to use for non-Datasource connection
	 *
	 * @see #JDBC_DRIVER
	 */
	public HibernatePersistenceConfiguration jdbcDriver(String driverName) {
		property( JDBC_DRIVER, driverName );
		return this;
	}

	/**
	 * URL to use for non-Datasource JDBC connection
	 *
	 * @see #JDBC_URL
	 */
	public HibernatePersistenceConfiguration jdbcUrl(String url) {
		property( JDBC_URL, url );
		return this;
	}

	/**
	 * User-name to use for non-Datasource JDBC connection
	 *
	 * @see #JDBC_USER
	 * @see #jdbcPassword
	 */
	public HibernatePersistenceConfiguration jdbcUsername(String username) {
		property( JDBC_USER, username );
		return this;
	}

	/**
	 * User-name to use for non-Datasource JDBC connection
	 *
	 * @see #JDBC_PASSWORD
	 * @see #jdbcUsername
	 */
	public HibernatePersistenceConfiguration jdbcPassword(String password) {
		property( JDBC_PASSWORD, password );
		return this;
	}

	/**
	 * Defines whether Hibernate will strictly adhere to compliance with Jakarta Persistence for
	 * all aspects of {@linkplain jakarta.persistence.Query} handling.
	 *
	 * @see JpaComplianceSettings#JPA_QUERY_COMPLIANCE
	 */
	public HibernatePersistenceConfiguration queryCompliance(boolean enabled) {
		property( JpaComplianceSettings.JPA_QUERY_COMPLIANCE, enabled );
		return this;
	}

	/**
	 * Defines whether Hibernate will strictly adhere to compliance with Jakarta Persistence for
	 * all aspects of transaction handling.
	 *
	 * @see JpaComplianceSettings#JPA_TRANSACTION_COMPLIANCE
	 */
	public HibernatePersistenceConfiguration transactionCompliance(boolean enabled) {
		property( JpaComplianceSettings.JPA_TRANSACTION_COMPLIANCE, enabled );
		return this;
	}

	/**
	 * Defines whether Hibernate will strictly adhere to compliance with Jakarta Persistence for
	 * handling around calls to {@linkplain EntityManager#close()},
	 * {@linkplain EntityManager#isOpen()},
	 * {@linkplain EntityManagerFactory#close()} and
	 * {@linkplain EntityManagerFactory#isOpen()}
	 *
	 * @see JpaComplianceSettings#JPA_CLOSED_COMPLIANCE
	 */
	public HibernatePersistenceConfiguration closedCompliance(boolean enabled) {
		property( JpaComplianceSettings.JPA_CLOSED_COMPLIANCE, enabled );
		return this;
	}

	/**
	 * Defines whether Hibernate will strictly adhere to compliance with Jakarta Persistence for
	 * handling of proxies.
	 *
	 * @see JpaComplianceSettings#JPA_PROXY_COMPLIANCE
	 */
	public HibernatePersistenceConfiguration proxyCompliance(boolean enabled) {
		property( JpaComplianceSettings.JPA_PROXY_COMPLIANCE, enabled );
		return this;
	}

	/**
	 * Defines whether Hibernate will strictly adhere to compliance with Jakarta Persistence for
	 * handling of proxies.
	 *
	 * @see JpaComplianceSettings#JPA_PROXY_COMPLIANCE
	 */
	public HibernatePersistenceConfiguration cachingCompliance(boolean enabled) {
		property( JpaComplianceSettings.JPA_PROXY_COMPLIANCE, enabled );
		return this;
	}

	/**
	 * Defines whether Hibernate will strictly adhere to compliance with Jakarta Persistence for
	 * in terms of collecting all named value generators globally, regardless of location.
	 *
	 * @see JpaComplianceSettings#JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE
	 */
	public HibernatePersistenceConfiguration globalGeneratorCompliance(boolean enabled) {
		property( JpaComplianceSettings.JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE, enabled );
		return this;
	}

	/**
	 * Defines whether Hibernate will strictly adhere to compliance with Jakarta Persistence for
	 * the interpretation of {@link jakarta.persistence.OrderBy}.
	 *
	 * @see JpaComplianceSettings#JPA_ORDER_BY_MAPPING_COMPLIANCE
	 */
	public HibernatePersistenceConfiguration orderByMappingCompliance(boolean enabled) {
		property( JpaComplianceSettings.JPA_ORDER_BY_MAPPING_COMPLIANCE, enabled );
		return this;
	}

	/**
	 * Defines whether Hibernate will strictly adhere to compliance with Jakarta Persistence for
	 * the allowed type of identifier value passed to
	 * {@link jakarta.persistence.EntityManager#getReference} and
	 * {@link jakarta.persistence.EntityManager#find}
	 *
	 * @see JpaComplianceSettings#JPA_LOAD_BY_ID_COMPLIANCE
	 */
	public HibernatePersistenceConfiguration loadByIdCompliance(boolean enabled) {
		property( JpaComplianceSettings.JPA_LOAD_BY_ID_COMPLIANCE, enabled );
		return this;
	}

	/**
	 * Enable/disable Hibernate's caching support
	 */
	public HibernatePersistenceConfiguration caching(CachingType type) {
		assert Objects.nonNull( type );
		if ( type == CachingType.NONE || type == CachingType.AUTO ) {
			property( CacheSettings.USE_SECOND_LEVEL_CACHE, false );
			property( CacheSettings.USE_QUERY_CACHE, false );
		}
		else if ( type == CachingType.BOTH ) {
			property( CacheSettings.USE_SECOND_LEVEL_CACHE, true );
			property( CacheSettings.USE_QUERY_CACHE, true );
		}
		else if ( type == CachingType.DATA ) {
			property( CacheSettings.USE_SECOND_LEVEL_CACHE, true );
			property( CacheSettings.USE_QUERY_CACHE, false );
		}
		else if ( type == CachingType.QUERY ) {
			property( CacheSettings.USE_SECOND_LEVEL_CACHE, false );
			property( CacheSettings.USE_QUERY_CACHE, true );
		}

		return this;
	}

	/**
	 * If {@linkplain CachingType#DATA data caching} is enabled, configure the
	 * type of concurrency access that should be applied if not explicitly specified
	 * on a cache region.
	 *
	 * @see org.hibernate.annotations.Cache#usage
	 * @see CacheSettings#DEFAULT_CACHE_CONCURRENCY_STRATEGY
	 */
	public HibernatePersistenceConfiguration cachingAccessType(AccessType type) {
		// todo (7.0) : should this enable second-level cache if not?
		property( CacheSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY, type );
		return this;
	}

	/**
	 * Specify a {@linkplain StatementInspector} to be applied to all Sessions/EntityManagers
	 *
	 * @see JdbcSettings#STATEMENT_INSPECTOR
	 */
	public HibernatePersistenceConfiguration statementInspector(Class<? extends StatementInspector> inspectorImpl) {
		property( JdbcSettings.STATEMENT_INSPECTOR, inspectorImpl );
		return this;
	}

	/**
	 * Specify a {@linkplain StatementInspector} to be applied to all Sessions/EntityManagers
	 *
	 * @see JdbcSettings#STATEMENT_INSPECTOR
	 */
	public HibernatePersistenceConfiguration statementInspector(StatementInspector inspector) {
		property( JdbcSettings.STATEMENT_INSPECTOR, inspector );
		return this;
	}

	/**
	 * Configure a default catalog name to be used for database objects (tables, sequences, etc) which do not
	 * explicitly specify one.
	 *
	 * @see MappingSettings#DEFAULT_CATALOG
	 */
	public HibernatePersistenceConfiguration defaultCatalog(String catalogName) {
		property( MappingSettings.DEFAULT_CATALOG, catalogName );
		return this;
	}

	/**
	 * Configure a default schema name to be used for database objects (tables, sequences, etc) which do not
	 * explicitly specify one.
	 *
	 * @see MappingSettings#DEFAULT_SCHEMA
	 */
	public HibernatePersistenceConfiguration defaultSchema(String schemaName) {
		property( MappingSettings.DEFAULT_SCHEMA, schemaName );
		return this;
	}

	/**
	 * Configure a default schema name to be used for database objects (tables, sequences, etc) which do not
	 * explicitly specify one.
	 *
	 * @see MappingSettings#USE_NATIONALIZED_CHARACTER_DATA
	 */
	public HibernatePersistenceConfiguration nationalizedCharacterData(boolean enabled) {
		property( MappingSettings.USE_NATIONALIZED_CHARACTER_DATA, enabled );
		return this;
	}

	/**
	 * Configures whether Hibernate should process XML mappings ({@code orm.xml} files).
	 *
	 * @see MappingSettings#XML_MAPPING_ENABLED
	 */
	public HibernatePersistenceConfiguration xmlMappings(boolean enabled) {
		property( MappingSettings.XML_MAPPING_ENABLED, enabled );
		return this;
	}

	/**
	 * Configures whether Hibernate should validate (via schema descriptor) XML files.
	 *
	 * @see MappingSettings#VALIDATE_XML
	 */
	public HibernatePersistenceConfiguration xmlValidation(boolean enabled) {
		property( MappingSettings.VALIDATE_XML, enabled );
		return this;
	}

	/**
	 * Configures whether Hibernate should collect {@linkplain org.hibernate.stat.Statistics}.
	 *
	 * @see StatisticsSettings#GENERATE_STATISTICS
	 */
	public HibernatePersistenceConfiguration collectStatistics(boolean enabled) {
		property( StatisticsSettings.GENERATE_STATISTICS, enabled );
		return this;
	}

	/**
	 * Add the specified classes as {@linkplain #managedClasses() managed classes}.
	 *
	 * @see #managedClass
	 */
	public HibernatePersistenceConfiguration managedClasses(Class<?>... managedClasses) {
		Collections.addAll( managedClasses(), managedClasses );
		return this;
	}

	/**
	 * Add the specified classes as {@linkplain #managedClasses() managed classes}.
	 *
	 * @see #managedClass
	 */
	public HibernatePersistenceConfiguration managedClasses(Collection<Class<?>> managedClasses) {
		managedClasses().addAll( managedClasses );
		return this;
	}

	/**
	 * Add the specified resource names as {@linkplain #mappingFiles() mapping files}.
	 *
	 * @see #mappingFiles
	 */
	public HibernatePersistenceConfiguration mappingFiles(String... names) {
		Collections.addAll( mappingFiles(), names );
		return this;
	}

	/**
	 * Add the specified resource names as {@linkplain #mappingFiles() mapping files}.
	 *
	 * @see #mappingFiles
	 */
	public HibernatePersistenceConfiguration mappingFiles(Collection<String> names) {
		mappingFiles().addAll( names );
		return this;
	}

	/**
	 * Specify the {@linkplain Action action} to take in terms of automatic
	 * database schema tooling.
	 *
	 * @apiNote This only controls tooling as exported directly to the database.  To
	 * output tooling commands to scripts, use {@linkplain #properties(Map) config properties}
	 * instead with appropriate {@linkplain SchemaToolingSettings settings}.
	 *
	 * @see SchemaToolingSettings#HBM2DDL_AUTO
	 */
	public HibernatePersistenceConfiguration schemaToolingAction(Action action) {
		property( SchemaToolingSettings.HBM2DDL_AUTO, action );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariant overrides

	@Override
	public HibernatePersistenceConfiguration provider(String providerClassName) {
		return (HibernatePersistenceConfiguration) super.provider( providerClassName );
	}

	@Override
	public HibernatePersistenceConfiguration jtaDataSource(String dataSourceJndiName) {
		return (HibernatePersistenceConfiguration) super.jtaDataSource( dataSourceJndiName );
	}

	@Override
	public HibernatePersistenceConfiguration nonJtaDataSource(String dataSourceJndiName) {
		return (HibernatePersistenceConfiguration) super.nonJtaDataSource( dataSourceJndiName );
	}

	@Override
	public HibernatePersistenceConfiguration managedClass(Class<?> managedClass) {
		return (HibernatePersistenceConfiguration) super.managedClass( managedClass );
	}

	@Override
	public HibernatePersistenceConfiguration mappingFile(String name) {
		return (HibernatePersistenceConfiguration) super.mappingFile( name );
	}

	@Override
	public HibernatePersistenceConfiguration transactionType(PersistenceUnitTransactionType transactionType) {
		return (HibernatePersistenceConfiguration) super.transactionType( transactionType );
	}

	@Override
	public HibernatePersistenceConfiguration sharedCacheMode(SharedCacheMode sharedCacheMode) {
		return (HibernatePersistenceConfiguration) super.sharedCacheMode( sharedCacheMode );
	}

	@Override
	public HibernatePersistenceConfiguration validationMode(ValidationMode validationMode) {
		return (HibernatePersistenceConfiguration) super.validationMode( validationMode );
	}

	@Override
	public HibernatePersistenceConfiguration property(String name, Object value) {
		return (HibernatePersistenceConfiguration) super.property( name, value );
	}

	@Override
	public HibernatePersistenceConfiguration properties(Map<String, ?> properties) {
		return (HibernatePersistenceConfiguration) super.properties( properties );
	}
}
