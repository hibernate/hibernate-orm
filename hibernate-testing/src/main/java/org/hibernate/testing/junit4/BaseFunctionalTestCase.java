package org.hibernate.testing.junit4;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class BaseFunctionalTestCase extends BaseUnitTestCase {
	public static final String VALIDATE_DATA_CLEANUP = "hibernate.test.validateDataCleanup";
	public static final String USE_NEW_METADATA_MAPPINGS = MetadataSources.USE_NEW_METADATA_MAPPINGS;
	public static final Dialect DIALECT = Dialect.getDialect();
	protected static final String[] NO_MAPPINGS = new String[0];
	protected static final Class<?>[] NO_CLASSES = new Class[0];
	protected Configuration configuration;
	protected MetadataImplementor metadata;
	protected StandardServiceRegistryImpl serviceRegistry;
	protected boolean isMetadataUsed;

	protected static Dialect getDialect() {
		return DIALECT;
	}
	//----------------------- configuration properties

	protected boolean createSchema() {
		return true;
	}

	protected Configuration configuration() {
		return configuration;
	}

	protected MetadataImplementor metadata() {
		return metadata;
	}

	protected boolean isMetadataUsed() {
		return isMetadataUsed;
	}

	//----------------------- services and service registry
	protected BootstrapServiceRegistry buildBootstrapServiceRegistry() {
		final BootstrapServiceRegistryBuilder builder = new BootstrapServiceRegistryBuilder();
		prepareBootstrapServiceRegistryBuilder( builder );
		return builder.build();
	}

	protected StandardServiceRegistryImpl serviceRegistry() {
		return serviceRegistry;
	}
	protected StandardServiceRegistryImpl buildServiceRegistry(BootstrapServiceRegistry bootRegistry, Configuration configuration) {
		Properties properties = new Properties();
		properties.putAll( configuration.getProperties() );
		Environment.verifyProperties( properties );
		ConfigurationHelper.resolvePlaceHolders( properties );

		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder( bootRegistry ).applySettings( properties );
		prepareStandardServiceRegistryBuilder( registryBuilder );
		return (StandardServiceRegistryImpl) registryBuilder.build();
	}

	protected void prepareStandardServiceRegistryBuilder(StandardServiceRegistryBuilder serviceRegistryBuilder) {
	}

	protected void prepareBootstrapServiceRegistryBuilder(BootstrapServiceRegistryBuilder builder) {
	}


	//----------------------- source mappings
	protected void addMappings(Configuration configuration) {
		String[] mappings = getMappings();
		if ( mappings != null ) {
			for ( String mapping : mappings ) {
				configuration.addResource(
						getBaseForMappings() + mapping,
						getClass().getClassLoader()
				);
			}
		}
		Class<?>[] annotatedClasses = getAnnotatedClasses();
		if ( annotatedClasses != null ) {
			for ( Class<?> annotatedClass : annotatedClasses ) {
				configuration.addAnnotatedClass( annotatedClass );
			}
		}
		String[] annotatedPackages = getAnnotatedPackages();
		if ( annotatedPackages != null ) {
			for ( String annotatedPackage : annotatedPackages ) {
				configuration.addPackage( annotatedPackage );
			}
		}
		String[] xmlFiles = getXmlFiles();
		if ( xmlFiles != null ) {
			for ( String xmlFile : xmlFiles ) {
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( xmlFile );
				configuration.addInputStream( is );
			}
		}
	}

	protected void addMappings(MetadataSources sources) {
		String[] mappings = getMappings();
		if ( mappings != null ) {
			for ( String mapping : mappings ) {
				sources.addResource(
						getBaseForMappings() + mapping
				);
			}
		}
		Class<?>[] annotatedClasses = getAnnotatedClasses();
		if ( annotatedClasses != null ) {
			for ( Class<?> annotatedClass : annotatedClasses ) {
				sources.addAnnotatedClass( annotatedClass );
			}
		}
		String[] annotatedPackages = getAnnotatedPackages();
		if ( annotatedPackages != null ) {
			for ( String annotatedPackage : annotatedPackages ) {
				sources.addPackage( annotatedPackage );
			}
		}
		String[] xmlFiles = getXmlFiles();
		if ( xmlFiles != null ) {
			for ( String xmlFile : xmlFiles ) {
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( xmlFile );
				sources.addInputStream( is );
			}
		}
	}

	protected String[] getMappings() {
		return NO_MAPPINGS;
	}

	protected String getBaseForMappings() {
		return "org/hibernate/test/";
	}

	protected Class<?>[] getAnnotatedClasses() {
		return NO_CLASSES;
	}

	protected String[] getAnnotatedPackages() {
		return NO_MAPPINGS;
	}

	protected String[] getXmlFiles() {
		// todo : rename to getOrmXmlFiles()
		return NO_MAPPINGS;
	}
	//-------------------------------------------

	protected EntityBinding getEntityBinding(Class<?> clazz) {
		return metadata.getEntityBinding( clazz.getName() );
	}

	protected EntityBinding getRootEntityBinding(Class<?> clazz) {
		return metadata.getRootEntityBinding( clazz.getName() );
	}

	protected Iterator<PluralAttributeBinding> getCollectionBindings() {
		return metadata.getCollectionBindings().iterator();
	}

}
