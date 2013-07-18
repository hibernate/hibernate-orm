package org.hibernate.testing.junit4;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.Clob;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.hibernate.EmptyInterceptor;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Mappings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.metamodel.MetadataBuilder;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.SessionFactoryBuilder;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.AbstractPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.type.Type;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class TestSessionFactoryHelper {

	private SessionFactoryImplementor sessionFactory;
	private TestConfigurationHelper testConfiguration;
	private TestServiceRegistryHelper serviceRegistryHelper;
	private Callback callback = new CallbackImpl();

	public TestSessionFactoryHelper(
			final TestServiceRegistryHelper serviceRegistryHelper, final TestConfigurationHelper testConfiguration) {
		this.serviceRegistryHelper = serviceRegistryHelper;
		this.testConfiguration = testConfiguration;
	}

	public SessionFactoryImplementor getSessionFactory() {
		if ( sessionFactory == null || sessionFactory.isClosed() ) {
			destory();
			buildSessionFactory();
		}
		return sessionFactory;
	}

	public TestServiceRegistryHelper getServiceRegistryHelper() {
		return serviceRegistryHelper;
	}

	public void setServiceRegistryHelper(final TestServiceRegistryHelper serviceRegistryHelper) {
		this.serviceRegistryHelper = serviceRegistryHelper;
	}

	public TestConfigurationHelper getTestConfiguration() {
		return testConfiguration;
	}

	public void setTestConfiguration(final TestConfigurationHelper testConfiguration) {
		this.testConfiguration = testConfiguration;
	}

	protected void configSessionFactoryBuilder(SessionFactoryBuilder sessionFactoryBuilder, Configuration configuration) {
		if ( configuration.getEntityNotFoundDelegate() != null ) {
			sessionFactoryBuilder.with( configuration.getEntityNotFoundDelegate() );
		}
		if ( configuration.getSessionFactoryObserver() != null ){
			sessionFactoryBuilder.add( configuration.getSessionFactoryObserver() );
		}
		if ( configuration.getInterceptor() != EmptyInterceptor.INSTANCE ) {
			sessionFactoryBuilder.with( configuration.getInterceptor() );
		}
	}


	public void buildSessionFactory() {
		Properties properties = getTestConfiguration().getProperties();

		BootstrapServiceRegistry bootRegistry = getServiceRegistryHelper().getBootstrapServiceRegistry();

		if ( getTestConfiguration().isMetadataUsed() ) {
			MetadataBuilder metadataBuilder = getMetadataBuilder(
					bootRegistry, getServiceRegistryHelper().getServiceRegistry()
			);
			getCallback().configure( metadataBuilder );
			MetadataImplementor metadata = (MetadataImplementor) metadataBuilder.build();
			getCallback().afterMetadataBuilt( metadata );
			applyCacheSettings( metadata );
			getTestConfiguration().setMetadata( metadata );
			SessionFactoryBuilder sessionFactoryBuilder = metadata.getSessionFactoryBuilder();
			getCallback().configSessionFactoryBuilder( sessionFactoryBuilder );
			sessionFactory = (SessionFactoryImplementor) sessionFactoryBuilder.build();
		}
		else {
			Configuration configuration = constructAndConfigureConfiguration( properties );
			// this is done here because Configuration does not currently support 4.0 xsd
			afterConstructAndConfigureConfiguration( configuration );
			getTestConfiguration().setConfiguration( configuration );
			sessionFactory = (SessionFactoryImplementor) configuration.buildSessionFactory( getServiceRegistryHelper().getServiceRegistry() );
		}
		getCallback().afterSessionFactoryBuilt();
	}

//	protected Configuration constructConfiguration() {
//		return new Configuration().setProperties( getTestConfiguration().getProperties() );
//	}

	public static void applyCacheSettings(
			MetadataImplementor metadataImplementor, String strategy, boolean overrideCacheStrategy) {
		if ( StringHelper.isEmpty( strategy ) ) {
			return;
		}
		for ( EntityBinding entityBinding : metadataImplementor.getEntityBindings() ) {
			boolean hasLob = false;
			for ( AttributeBinding attributeBinding : entityBinding.getAttributeBindingClosure() ) {
				if ( attributeBinding.getAttribute().isSingular() ) {
					Type type = attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping();
					String typeName = type.getName();
					if ( "blob".equals( typeName ) || "clob".equals( typeName ) ) {
						hasLob = true;
					}
					if ( Blob.class.getName().equals( typeName ) || Clob.class.getName().equals( typeName ) ) {
						hasLob = true;
					}
				}
			}
			if ( !hasLob && entityBinding.getSuperEntityBinding() == null && overrideCacheStrategy ) {
				Caching caching = entityBinding.getHierarchyDetails().getCaching();
				if ( caching == null ) {
					caching = new Caching();
				}
				caching.setRegion( entityBinding.getEntity().getName() );
				caching.setCacheLazyProperties( true );
				caching.setAccessType( AccessType.fromExternalName( strategy ) );
				entityBinding.getHierarchyDetails().setCaching( caching );
			}
			for ( AttributeBinding attributeBinding : entityBinding.getAttributeBindingClosure() ) {
				if ( !attributeBinding.getAttribute().isSingular() ) {
					AbstractPluralAttributeBinding binding = AbstractPluralAttributeBinding.class.cast( attributeBinding );
					Caching caching = binding.getCaching();
					if ( caching == null ) {
						caching = new Caching();
					}
					caching.setRegion(
							StringHelper.qualify(
									entityBinding.getEntity().getName(),
									attributeBinding.getAttribute().getName()
							)
					);
					caching.setCacheLazyProperties( true );
					caching.setAccessType( AccessType.fromExternalName( strategy ) );
					binding.setCaching( caching );
				}
			}
		}
	}

	protected void applyCacheSettings(MetadataImplementor metadataImplementor) {
		applyCacheSettings( metadataImplementor, getTestConfiguration().getCacheConcurrencyStrategy(), getTestConfiguration().isOverrideCacheStrategy() );
	}

	protected void applyCacheSettings(Configuration configuration) {
		if ( getTestConfiguration().getCacheConcurrencyStrategy() != null ) {
			Iterator itr = configuration.getClassMappings();
			while ( itr.hasNext() ) {
				PersistentClass clazz = (PersistentClass) itr.next();
				Iterator props = clazz.getPropertyClosureIterator();
				boolean hasLob = false;
				while ( props.hasNext() ) {
					Property prop = (Property) props.next();
					if ( prop.getValue().isSimpleValue() ) {
						String type = ( (SimpleValue) prop.getValue() ).getTypeName();
						if ( "blob".equals( type ) || "clob".equals( type ) ) {
							hasLob = true;
						}
						if ( Blob.class.getName().equals( type ) || Clob.class.getName().equals( type ) ) {
							hasLob = true;
						}
					}
				}
				if ( !hasLob && !clazz.isInherited() && getTestConfiguration().isOverrideCacheStrategy() ) {
					configuration.setCacheConcurrencyStrategy( clazz.getEntityName(), getTestConfiguration().getCacheConcurrencyStrategy() );
				}
			}
			itr = configuration.getCollectionMappings();
			while ( itr.hasNext() ) {
				Collection coll = (Collection) itr.next();
				configuration.setCollectionCacheConcurrencyStrategy( coll.getRole(), getTestConfiguration().getCacheConcurrencyStrategy() );
			}
		}
	}
	protected void afterConfigurationBuilt(Configuration configuration) {
		getCallback().afterConfigurationBuilt( configuration.createMappings(), getTestConfiguration().getDialect() );
	}

	private Configuration constructAndConfigureConfiguration(Properties properties) {
		Configuration cfg = new Configuration().setProperties( properties );
		getCallback().configure( cfg );
		return cfg;
	}

	private void afterConstructAndConfigureConfiguration(Configuration cfg) {
		addMappings( cfg );
		cfg.buildMappings();
		applyCacheSettings( cfg );
		afterConfigurationBuilt( cfg );
	}


	protected MetadataBuilder getMetadataBuilder(
			BootstrapServiceRegistry bootRegistry, StandardServiceRegistryImpl serviceRegistry) {
		MetadataSources sources = new MetadataSources( bootRegistry );
		addMappings( sources );
		return sources.getMetadataBuilder( serviceRegistry );
	}

	//----------------------- source mappings
	public void addMappings(Configuration configuration) {
		List<String> mappings = getTestConfiguration().getMappings();
		if ( mappings != null ) {
			for ( String mapping : mappings ) {
				configuration.addResource(
						mapping, getClass().getClassLoader()
				);
			}
		}
		List<Class> annotatedClasses = getTestConfiguration().getAnnotatedClasses();
		if ( annotatedClasses != null ) {
			for ( Class<?> annotatedClass : annotatedClasses ) {
				configuration.addAnnotatedClass( annotatedClass );
			}
		}
		List<String> annotatedPackages = getTestConfiguration().getAnnotatedPackages();
		if ( annotatedPackages != null ) {
			for ( String annotatedPackage : annotatedPackages ) {
				configuration.addPackage( annotatedPackage );
			}
		}
		List<String> xmlFiles = getTestConfiguration().getOrmXmlFiles();
		if ( xmlFiles != null ) {
			for ( String xmlFile : xmlFiles ) {
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( xmlFile );
				configuration.addInputStream( is );
			}
		}
	}

	public void addMappings(MetadataSources sources) {
		List<String> mappings = getTestConfiguration().getMappings();
		if ( mappings != null ) {
			for ( String mapping : mappings ) {
				sources.addResource( mapping );
			}
		}
		List<Class> annotatedClasses = getTestConfiguration().getAnnotatedClasses();
		if ( annotatedClasses != null ) {
			for ( Class<?> annotatedClass : annotatedClasses ) {
				sources.addAnnotatedClass( annotatedClass );
			}
		}
		List<String> annotatedPackages = getTestConfiguration().getAnnotatedPackages();
		if ( annotatedPackages != null ) {
			for ( String annotatedPackage : annotatedPackages ) {
				sources.addPackage( annotatedPackage );
			}
		}
		List<String> xmlFiles = getTestConfiguration().getOrmXmlFiles();
		if ( xmlFiles != null ) {
			for ( String xmlFile : xmlFiles ) {
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( xmlFile );
				sources.addInputStream( is );
			}
		}
	}


	public void destory() {
		if ( sessionFactory == null ) {
			return;
		}
		sessionFactory.close();
		sessionFactory = null;
	}

	public Callback getCallback() {
		return callback;
	}

	public TestSessionFactoryHelper setCallback(final Callback callback) {
		this.callback = callback;
		return this;
	}

	public static interface Callback {
		void afterMetadataBuilt(MetadataImplementor metadataImplementor);

		void configure(MetadataBuilder metadataBuilder);

		void configSessionFactoryBuilder(SessionFactoryBuilder sessionFactoryBuilder);

		void afterSessionFactoryBuilt();

		void configure(Configuration configuration);
		void afterConfigurationBuilt(Mappings mappings, Dialect dialect);
	}

	public static class CallbackImpl implements Callback {
		@Override
		public void afterMetadataBuilt(final MetadataImplementor metadataImplementor) {
		}


		@Override
		public void configSessionFactoryBuilder(final SessionFactoryBuilder sessionFactoryBuilder) {
		}

		@Override
		public void afterSessionFactoryBuilt() {
		}

		@Override
		public void configure(final Configuration configuration) {
		}
		@Override
		public void configure(final MetadataBuilder metadataBuilder) {
		}


		@Override
		public void afterConfigurationBuilt(final Mappings mappings, final Dialect dialect) {
		}
	}

}
