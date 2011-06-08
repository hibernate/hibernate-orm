//$Id: HibernateServiceMBean.java 10860 2006-11-22 00:02:55Z steve.ebersole@jboss.com $
package org.hibernate.jmx;
import org.hibernate.HibernateException;

/**
 * Hibernate JMX Management API
 * @see HibernateService
 * @author John Urberg, Gavin King
 * @deprecated See <a href="http://opensource.atlassian.com/projects/hibernate/browse/HHH-6190">HHH-6190</a> for details
 */
@Deprecated
public interface HibernateServiceMBean {

	/**
	 * The Hibernate mapping files (might be overridden by subclasses
	 * that want to specify the mapping files by some other mechanism)
	 * @return String
	 */
	public String getMapResources();
	/**
	 * Specify the Hibernate mapping files
	 * @param mappingFiles
	 */
	public void setMapResources(String mappingFiles);
	/**
	 * Add a mapping file
	 * @param mapResource
	 */
	public void addMapResource(String mapResource);

	/**
	 * Set a property
	 * @param property the property name
	 * @param value the property value
	 */
	public void setProperty(String property, String value);

	/**
	 * Get a property
	 * @param property the property name
	 * @return the property value
	 */
	public String getProperty(String property);

	/**
	 * Display the properties
	 * @return a list of property names and values
	 */
	public String getPropertyList();

	/**
	 * The JNDI name of the datasource to use in this <tt>SessionFactory</tt>
	 * @return String
	 */
	public String getDatasource();
	/**
	 * Set the JNDI name of the datasource to use in this <tt>SessionFactory</tt>
	 * @param datasource
	 */
	public void setDatasource(String datasource);

	/**
	 * Log into the database with this name
	 * @return String
	 */
	public String getUserName();
	/**
	 * Log into the database with this name
	 * @param userName
	 */
	public void setUserName(String userName);

	/**
	 * Log into the database with this password
	 * @return String
	 */
	public String getPassword();
	/**
	 * Log into the database with this password
	 * @param password
	 */
	public void setPassword(String password);

	/**
	 * The JNDI name of the dialect class to use in this <tt>SessionFactory</tt>
	 * @return String
	 */
	public String getDialect();
	/**
	 * The name of the dialect class to use in this <tt>SessionFactory</tt>
	 * @param dialect fully qualified class name of <tt>Dialect</tt> subclass
	 * @see org.hibernate.dialect.Dialect
	 */
	public void setDialect(String dialect);

	/**
	 * The JNDI name to bind to the <tt>SessionFactory</tt>
	 * @return String
	 */
	public String getJndiName();
	/**
	 * The JNDI name to bind to the <tt>SessionFactory</tt>
	 * @param jndiName
	 */
	public void setJndiName(String jndiName);

	/**
	 * The fully qualified class name of the Hibernate {@link org.hibernate.engine.transaction.spi.TransactionFactory}
	 * implementation to use
	 *
	 * @return the class name
	 */
	public String getTransactionStrategy();

	/**
	 * Set the fully qualified class name of the Hibernate {@link org.hibernate.engine.transaction.spi.TransactionFactory}
	 * implementation to use.
	 *
	 * @param txnStrategy the class name
	 */
	public void setTransactionStrategy(String txnStrategy);

	/**
	 * The JNDI name of the JTA UserTransaction object (used only be <tt>JtaTransaction</tt>).
	 * @return the JNDI name
	 * @see org.hibernate.engine.transaction.internal.jta.JtaTransaction
	 */
	public String getUserTransactionName();
	/**
	 * Set the JNDI name of the JTA UserTransaction object (used only by <tt>JtaTransaction</tt>).
	 * @param utName the JNDI name
	 * @see org.hibernate.engine.transaction.internal.jta.JtaTransaction
	 */
	public void setUserTransactionName(String utName);

	/**
	 * Get the name of the {@link org.hibernate.service.jta.platform.spi.JtaPlatform} implementation to use.
	 *
	 * @return The name of the {@link org.hibernate.service.jta.platform.spi.JtaPlatform} implementation to use.
	 */
	public String getJtaPlatformName();

	/**
	 * Sets the name of the {@link org.hibernate.service.jta.platform.spi.JtaPlatform} implementation to use.
	 *
	 * @param name The implementation class name.
	 */
	public void setJtaPlatformName(String name);

	/**
	 * Is SQL logging enabled?
	 */
	public String getShowSqlEnabled();
	/**
	 * Enable logging of SQL to console
	 */
	public void setShowSqlEnabled(String showSql);
	/**
	 * Get the maximum outer join fetch depth
	 */
	public String getMaximumFetchDepth();
	/**
	 * Set the maximum outer join fetch depth
	 */
	public void setMaximumFetchDepth(String fetchDepth);
	/**
	 * Get the maximum JDBC batch size
	 */
	public String getJdbcBatchSize();
	/**
	 * Set the maximum JDBC batch size
	 */
	public void setJdbcBatchSize(String batchSize);
	/**
	 * Get the JDBC fetch size
	 */
	public String getJdbcFetchSize();
	/**
	 * Set the JDBC fetch size
	 */
	public void setJdbcFetchSize(String fetchSize);
	/**
	 * Get the query language substitutions
	 */
	public String getQuerySubstitutions();
	/**
	 * Set the query language substitutions
	 */
	public void setQuerySubstitutions(String querySubstitutions);
	/**
	 * Get the default schema
	 */
	public String getDefaultSchema();
	/**
	 * Set the default schema
	 */
	public void setDefaultSchema(String schema);
	/**
	 * Get the default catalog
	 */
	public String getDefaultCatalog();
	/**
	 * Set the default catalog
	 */
	public void setDefaultCatalog(String catalog);
	/**
	 * Is use of scrollable resultsets enabled?
	 */
	public String getJdbcScrollableResultSetEnabled();
	/**
	 * Enable or disable the use of scrollable resultsets 
	 */
	public void setJdbcScrollableResultSetEnabled(String enabled);
	/**
	 * Is use of JDBC3 <tt>getGeneratedKeys()</tt> enabled?
	 */
	public String getGetGeneratedKeysEnabled();
	/**
	 * Enable or disable the use <tt>getGeneratedKeys()</tt> 
	 */
	public void setGetGeneratedKeysEnabled(String enabled);
	/**
	 * Get the second-level cache provider class name
	 */
	public String getCacheRegionFactory();
	/**
	 * Set the second-level cache provider class name
	 */
	public void setCacheRegionFactory(String cacheRegionFactory);
	/**
	 * For cache providers which support this setting, get the
	 * provider's specific configuration resource.
	 */
	public String getCacheProviderConfig();
	/**
	 * For cache providers which support this setting, specify the
	 * provider's specific configuration resource.
	 */
	public void setCacheProviderConfig(String cacheProviderConfig);
	/**
	 * Is the query cache enabled?
	 */
	public String getQueryCacheEnabled();
	/**
	 * Enable or disable the query cache
	 */
	public void setQueryCacheEnabled(String enabled);
	/**
	 * Is the second-level cache enabled?
	 */
	public String getSecondLevelCacheEnabled();
	/**
	 * Enable or disable the second-level cache
	 */
	public void setSecondLevelCacheEnabled(String enabled);
	/**
	 * Get the cache region prefix
	 */
	public String getCacheRegionPrefix();
	/**
	 * Set the cache region prefix
	 */
	public void setCacheRegionPrefix(String prefix);
	/**
	 * Is the second-level cache optimized for miminal puts?
	 */
	public String getMinimalPutsEnabled();
	/**
	 * Enable or disable optimization of second-level cache
	 * for minimal puts 
	 */
	public void setMinimalPutsEnabled(String enabled);
	/**
	 * Are SQL comments enabled?
	 */
	public String getCommentsEnabled();
	/**
	 * Enable or disable the inclusion of comments in
	 * generated SQL
	 */
	public void setCommentsEnabled(String enabled);
	/**
	 * Is JDBC batch update for versioned entities enabled?
	 */
	public String getBatchVersionedDataEnabled();
	/**
	 * Enable or disable the use of batch updates for
	 * versioned entities
	 */
	public void setBatchVersionedDataEnabled(String enabled);
	
	/**
	 * Enable automatic flushing of the Session when JTA transaction ends.
	 */
	public void setFlushBeforeCompletionEnabled(String enabled);
	/**
	 * Is automatic Session flusing enabled?
	 */
	public String getFlushBeforeCompletionEnabled();

	/**
	 * Enable automatic closing of Session when JTA transaction ends.
	 */
	public void setAutoCloseSessionEnabled(String enabled);
	/**
	 * Is automatic Session closing enabled?
	 */
	public String getAutoCloseSessionEnabled();

	/**
	 * Export the <tt>CREATE</tt> DDL to the database
	 * @throws HibernateException
	 */
	public void createSchema() throws HibernateException;
	/**
	 * Export the <tt>DROP</tt> DDL to the database
	 * @throws HibernateException
	 */
	public void dropSchema() throws HibernateException;


	/**
	 * Create the <tt>SessionFactory</tt> and bind to the jndi name on startup
	 */
	public void start() throws HibernateException;
	/**
	 * Unbind the <tt>SessionFactory</tt> or stub from JNDI
	 */
	public void stop();

}






