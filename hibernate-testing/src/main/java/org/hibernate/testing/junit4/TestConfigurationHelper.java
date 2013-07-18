package org.hibernate.testing.junit4;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.testing.cache.CachingRegionFactory;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class TestConfigurationHelper {
	private static final Dialect DEFAULT_DIALECT = Dialect.getDialect();
	public static final boolean DEFAULT_USE_NEW_METAMODEL = Boolean.valueOf(
			System.getProperty(
					MetadataSources.USE_NEW_METADATA_MAPPINGS, "true"
			)
	);

	protected Configuration configuration;
	protected MetadataImplementor metadata;
	protected StandardServiceRegistryImpl serviceRegistry;
	private Properties properties;


	private Dialect dialect = DEFAULT_DIALECT;
	private boolean isMetadataUsed = DEFAULT_USE_NEW_METAMODEL;
	private boolean createSchema = true;
	private String secondSchemaName = null;

	private String cacheConcurrencyStrategy;

	private boolean overrideCacheStrategy = true;

	public boolean isOverrideCacheStrategy() {
		return overrideCacheStrategy;
	}

	public void setOverrideCacheStrategy(final boolean overrideCacheStrategy) {
		this.overrideCacheStrategy = overrideCacheStrategy;
	}

	public String getCacheConcurrencyStrategy() {
		return cacheConcurrencyStrategy;
	}

	public void setCacheConcurrencyStrategy(final String cacheConcurrencyStrategy) {
		this.cacheConcurrencyStrategy = cacheConcurrencyStrategy;
	}

	public String getSecondSchemaName() {
		return secondSchemaName;
	}

	public void setSecondSchemaName(final String secondSchemaName) {
		this.secondSchemaName = secondSchemaName;
	}

	public boolean isCreateSchema() {
		return createSchema;
	}

	public void setCreateSchema(final boolean createSchema) {
		this.createSchema = createSchema;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(final Configuration configuration) {
		this.configuration = configuration;
	}

	public MetadataImplementor getMetadata() {
		return metadata;
	}

	public void setMetadata(final MetadataImplementor metadata) {
		this.metadata = metadata;
	}

	public StandardServiceRegistryImpl getServiceRegistry() {
		return serviceRegistry;
	}

	public void setServiceRegistry(final StandardServiceRegistryImpl serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public Dialect getDialect() {
		return dialect;
	}

	public void setDialect(final Dialect dialect) {
		this.dialect = dialect;
	}

	public boolean isMetadataUsed() {
		return isMetadataUsed;
	}

	public void setMetadataUsed(final boolean metadataUsed) {
		isMetadataUsed = metadataUsed;
	}
	//-------------------------------------- mappings
	private List<String> mappings = new ArrayList<String>();
	public List<String> getMappings() {
		return mappings;
	}

	private List<Class> annotatedClasses = new ArrayList<Class>(  );
	public List<Class> getAnnotatedClasses() {
		return annotatedClasses;
	}
	private List<String> annotatedPackages = new ArrayList<String>(  );
	public List<String> getAnnotatedPackages() {
		return annotatedPackages;
	}
	private List<String> xmlFiles = new ArrayList<String>(  );
	public List<String> getOrmXmlFiles() {
		return xmlFiles;
	}

	public TestConfigurationHelper addAnnotatedClass(Class clazz){
		getAnnotatedClasses().add( clazz );
		return this;
	}

	public TestConfigurationHelper addAnnotatedPackage(String name){
		getAnnotatedPackages().add( name );
		return this;
	}

	public TestConfigurationHelper addMapping(String name){
		getMappings().add( name );
		return this;
	}

	public TestConfigurationHelper addOrmXmlFile(String name){
		getOrmXmlFiles().add( name );
		return this;
	}


	//------------------------------------- properties

	public Properties getProperties() {
		if(properties == null){
			properties = constructProperties();
		}
		return properties;
	}

	private Properties constructProperties() {
		Properties properties = new Properties();
		properties.put( Environment.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName() );
		properties.put( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );
		properties.put( Environment.DIALECT, getDialect().getClass().getName() );
		if ( isCreateSchema() ) {
			properties.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		}
		return properties;
	}

}
