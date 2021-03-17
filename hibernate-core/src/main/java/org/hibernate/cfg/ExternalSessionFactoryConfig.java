/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * Defines support for various externally configurable SessionFactory(s), for
 * example, {@link org.hibernate.jmx.HibernateService JMX} or the JCA
 * adapter.
 *
 * @author Steve Ebersole
 */
public abstract class ExternalSessionFactoryConfig {
	private String mapResources;
	private String dialect;
	private String defaultSchema;
	private String defaultCatalog;
	private String maximumFetchDepth;
	private String jdbcFetchSize;
	private String jdbcBatchSize;
	private String batchVersionedDataEnabled;
	private String jdbcScrollableResultSetEnabled;
	private String getGeneratedKeysEnabled;
	private String streamsForBinaryEnabled;
	private String reflectionOptimizationEnabled;
	private String querySubstitutions;
	private String showSqlEnabled;
	private String commentsEnabled;
	private String cacheRegionFactory;
	private String cacheProviderConfig;
	private String cacheRegionPrefix;
	private String secondLevelCacheEnabled;
	private String minimalPutsEnabled;
	private String queryCacheEnabled;

	private Map additionalProperties;
	private Set excludedPropertyNames = new HashSet();

	protected Set getExcludedPropertyNames() {
		return excludedPropertyNames;
	}

	public final String getMapResources() {
		return mapResources;
	}

	public final void setMapResources(String mapResources) {
		this.mapResources = mapResources;
	}

	public void addMapResource(String mapResource) {
		if ( mapResources==null || mapResources.length()==0 ) {
			mapResources = mapResource.trim();
		}
		else {
			mapResources += ", " + mapResource.trim();
		}
	}

	public final String getDialect() {
		return dialect;
	}

	public final void setDialect(String dialect) {
		this.dialect = dialect;
	}

	public final String getDefaultSchema() {
		return defaultSchema;
	}

	public final void setDefaultSchema(String defaultSchema) {
		this.defaultSchema = defaultSchema;
	}

	public final String getDefaultCatalog() {
		return defaultCatalog;
	}

	public final void setDefaultCatalog(String defaultCatalog) {
		this.defaultCatalog = defaultCatalog;
	}

	public final String getMaximumFetchDepth() {
		return maximumFetchDepth;
	}

	public final void setMaximumFetchDepth(String maximumFetchDepth) {
		verifyInt( maximumFetchDepth );
		this.maximumFetchDepth = maximumFetchDepth;
	}

	public final String getJdbcFetchSize() {
		return jdbcFetchSize;
	}

	public final void setJdbcFetchSize(String jdbcFetchSize) {
		verifyInt( jdbcFetchSize );
		this.jdbcFetchSize = jdbcFetchSize;
	}

	public final String getJdbcBatchSize() {
		return jdbcBatchSize;
	}

	public final void setJdbcBatchSize(String jdbcBatchSize) {
		verifyInt( jdbcBatchSize );
		this.jdbcBatchSize = jdbcBatchSize;
	}

	public final String getBatchVersionedDataEnabled() {
		return batchVersionedDataEnabled;
	}

	public final void setBatchVersionedDataEnabled(String batchVersionedDataEnabled) {
		this.batchVersionedDataEnabled = batchVersionedDataEnabled;
	}

	public final String getJdbcScrollableResultSetEnabled() {
		return jdbcScrollableResultSetEnabled;
	}

	public final void setJdbcScrollableResultSetEnabled(String jdbcScrollableResultSetEnabled) {
		this.jdbcScrollableResultSetEnabled = jdbcScrollableResultSetEnabled;
	}

	public final String getGetGeneratedKeysEnabled() {
		return getGeneratedKeysEnabled;
	}

	public final void setGetGeneratedKeysEnabled(String getGeneratedKeysEnabled) {
		this.getGeneratedKeysEnabled = getGeneratedKeysEnabled;
	}

	public final String getStreamsForBinaryEnabled() {
		return streamsForBinaryEnabled;
	}

	public final void setStreamsForBinaryEnabled(String streamsForBinaryEnabled) {
		this.streamsForBinaryEnabled = streamsForBinaryEnabled;
	}

	public final String getReflectionOptimizationEnabled() {
		return reflectionOptimizationEnabled;
	}

	public final void setReflectionOptimizationEnabled(String reflectionOptimizationEnabled) {
		this.reflectionOptimizationEnabled = reflectionOptimizationEnabled;
	}

	public final String getQuerySubstitutions() {
		return querySubstitutions;
	}

	public final void setQuerySubstitutions(String querySubstitutions) {
		this.querySubstitutions = querySubstitutions;
	}

	public final String getShowSqlEnabled() {
		return showSqlEnabled;
	}

	public final void setShowSqlEnabled(String showSqlEnabled) {
		this.showSqlEnabled = showSqlEnabled;
	}

	public final String getCommentsEnabled() {
		return commentsEnabled;
	}

	public final void setCommentsEnabled(String commentsEnabled) {
		this.commentsEnabled = commentsEnabled;
	}

	public final String getSecondLevelCacheEnabled() {
		return secondLevelCacheEnabled;
	}

	public final void setSecondLevelCacheEnabled(String secondLevelCacheEnabled) {
		this.secondLevelCacheEnabled = secondLevelCacheEnabled;
	}

	public final String getCacheRegionFactory() {
		return cacheRegionFactory;
	}

	public final void setCacheRegionFactory(String cacheRegionFactory) {
		this.cacheRegionFactory = cacheRegionFactory;
	}

	public String getCacheProviderConfig() {
		return cacheProviderConfig;
	}

	public void setCacheProviderConfig(String cacheProviderConfig) {
		this.cacheProviderConfig = cacheProviderConfig;
	}

	public final String getCacheRegionPrefix() {
		return cacheRegionPrefix;
	}

	public final void setCacheRegionPrefix(String cacheRegionPrefix) {
		this.cacheRegionPrefix = cacheRegionPrefix;
	}

	public final String getMinimalPutsEnabled() {
		return minimalPutsEnabled;
	}

	public final void setMinimalPutsEnabled(String minimalPutsEnabled) {
		this.minimalPutsEnabled = minimalPutsEnabled;
	}

	public final String getQueryCacheEnabled() {
		return queryCacheEnabled;
	}

	public final void setQueryCacheEnabled(String queryCacheEnabled) {
		this.queryCacheEnabled = queryCacheEnabled;
	}

	public final void addAdditionalProperty(String name, String value) {
		if ( !getExcludedPropertyNames().contains( name ) ) {
			if ( additionalProperties == null ) {
				additionalProperties = new HashMap();
			}
			additionalProperties.put( name, value );
		}
	}

	protected final Configuration buildConfiguration() {

		Configuration cfg = new Configuration().setProperties( buildProperties() );


		String[] mappingFiles = ConfigurationHelper.toStringArray( mapResources, " ,\n\t\r\f" );
		for ( String mappingFile : mappingFiles ) {
			cfg.addResource( mappingFile );
		}

		return cfg;
	}

	protected final Properties buildProperties() {
		Properties props = new Properties();
		setUnlessNull( props, Environment.DIALECT, dialect );
		setUnlessNull( props, Environment.DEFAULT_SCHEMA, defaultSchema );
		setUnlessNull( props, Environment.DEFAULT_CATALOG, defaultCatalog );
		setUnlessNull( props, Environment.MAX_FETCH_DEPTH, maximumFetchDepth );
		setUnlessNull( props, Environment.STATEMENT_FETCH_SIZE, jdbcFetchSize );
		setUnlessNull( props, Environment.STATEMENT_BATCH_SIZE, jdbcBatchSize );
		setUnlessNull( props, Environment.BATCH_VERSIONED_DATA, batchVersionedDataEnabled );
		setUnlessNull( props, Environment.USE_SCROLLABLE_RESULTSET, jdbcScrollableResultSetEnabled );
		setUnlessNull( props, Environment.USE_GET_GENERATED_KEYS, getGeneratedKeysEnabled );
		setUnlessNull( props, Environment.USE_STREAMS_FOR_BINARY, streamsForBinaryEnabled );
		setUnlessNull( props, Environment.USE_REFLECTION_OPTIMIZER, reflectionOptimizationEnabled );
		setUnlessNull( props, Environment.QUERY_SUBSTITUTIONS, querySubstitutions );
		setUnlessNull( props, Environment.SHOW_SQL, showSqlEnabled );
		setUnlessNull( props, Environment.USE_SQL_COMMENTS, commentsEnabled );
		setUnlessNull( props, Environment.CACHE_REGION_FACTORY, cacheRegionFactory );
		setUnlessNull( props, Environment.CACHE_PROVIDER_CONFIG, cacheProviderConfig );
		setUnlessNull( props, Environment.CACHE_REGION_PREFIX, cacheRegionPrefix );
		setUnlessNull( props, Environment.USE_MINIMAL_PUTS, minimalPutsEnabled );
		setUnlessNull( props, Environment.USE_SECOND_LEVEL_CACHE, secondLevelCacheEnabled );
		setUnlessNull( props, Environment.USE_QUERY_CACHE, queryCacheEnabled );

		Map extraProperties = getExtraProperties();
		if ( extraProperties != null ) {
			addAll( props, extraProperties );
		}

		if ( additionalProperties != null ) {
			addAll( props, additionalProperties );
		}

		return props;
	}

	protected void addAll( Properties target, Map source ) {
		Iterator itr = source.entrySet().iterator();
		while ( itr.hasNext() ) {
			final Map.Entry entry = ( Map.Entry ) itr.next();
			final String propertyName = ( String ) entry.getKey();
			final String propertyValue = ( String ) entry.getValue();
			if ( propertyName != null && propertyValue != null ) {
				// Make sure we don't override previous set values
				if ( !target.keySet().contains( propertyName ) ) {
					if ( !getExcludedPropertyNames().contains( propertyName) ) {
						target.put( propertyName, propertyValue );
					}
				}
			}
		}
	}

	protected Map getExtraProperties() {
		return null;
	}

	private void setUnlessNull(Properties props, String key, String value) {
		if ( value != null ) {
			props.setProperty( key, value );
		}
	}

	private void verifyInt(String value) {
		if ( value != null ) {
			Integer.parseInt( value );
		}
	}
}
