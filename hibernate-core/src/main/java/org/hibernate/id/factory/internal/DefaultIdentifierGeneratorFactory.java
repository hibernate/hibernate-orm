/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.factory.internal;

import java.io.Serializable;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.id.Assigned;
import org.hibernate.id.Configurable;
import org.hibernate.id.ForeignGenerator;
import org.hibernate.id.GUIDGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.id.SelectGenerator;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.UUIDHexGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.Type;

/**
 * Basic <tt>templated</tt> support for {@link org.hibernate.id.factory.IdentifierGeneratorFactory} implementations.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings( { "deprecation" ,"rawtypes" ,"serial" } )
public class DefaultIdentifierGeneratorFactory
		implements MutableIdentifierGeneratorFactory, ServiceRegistryAwareService, BeanContainer.LifecycleOptions, Serializable {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DefaultIdentifierGeneratorFactory.class );

	private ServiceRegistry serviceRegistry;
	private Dialect dialect;

	private ConcurrentHashMap<String, Class> generatorStrategyToClassNameMap = new ConcurrentHashMap<>();

	/**
	 * Constructs a new DefaultIdentifierGeneratorFactory.
	 */
	public DefaultIdentifierGeneratorFactory() {
		register( "uuid", UUIDGenerator.class );
		register( "uuid2", UUIDGenerator.class );
		register( "uuid.hex", UUIDHexGenerator.class );
		// could be done with UUIDGenerator + strategy
		register( "guid", GUIDGenerator.class );
		register( "assigned", Assigned.class );
		register( "identity", IdentityGenerator.class );
		register( "select", SelectGenerator.class );
		register( "table", TableGenerator.class );
		register( "sequence", SequenceStyleGenerator.class );
		register( "increment", IncrementGenerator.class );
		register( "foreign", ForeignGenerator.class );
		register( "enhanced-sequence", SequenceStyleGenerator.class );
		register( "enhanced-table", TableGenerator.class );
	}

	public void register(String strategy, Class generatorClass) {
		LOG.debugf( "Registering IdentifierGenerator strategy [%s] -> [%s]", strategy, generatorClass.getName() );
		final Class previous = generatorStrategyToClassNameMap.put( strategy, generatorClass );
		if ( previous != null && LOG.isDebugEnabled() ) {
			LOG.debugf( "    - overriding [%s]", previous.getName() );
		}
	}

	@Override
	public Dialect getDialect() {
		return dialect;
	}

	@Override
	public void setDialect(Dialect dialect) {
//		LOG.debugf( "Setting dialect [%s]", dialect );
//		this.dialect = dialect;
//
//		if ( dialect == jdbcEnvironment.getDialect() ) {
//			LOG.debugf(
//					"Call to unsupported method IdentifierGeneratorFactory#setDialect; " +
//							"ignoring as passed Dialect matches internal Dialect"
//			);
//		}
//		else {
//			throw new UnsupportedOperationException(
//					"Call to unsupported method IdentifierGeneratorFactory#setDialect attempting to" +
//							"set a non-matching Dialect : " + dialect.getClass().getName()
//			);
//		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public IdentifierGenerator createIdentifierGenerator(String strategy, Type type, Properties config) {
		try {
			final BeanContainer beanContainer = serviceRegistry.getService(ManagedBeanRegistry.class).getBeanContainer();
			final Class clazz = getIdentifierGeneratorClass( strategy );

			final IdentifierGenerator identifierGenerator;
			if ( generatorStrategyToClassNameMap.containsKey(strategy) || beanContainer == null ) {
				identifierGenerator = ( IdentifierGenerator ) clazz.newInstance();
			}
			else {
				final ContainedBean<IdentifierGenerator> generatorBean = beanContainer.getBean(
						clazz,
						this,
						FallbackBeanInstanceProducer.INSTANCE
				);

				identifierGenerator = generatorBean.getBeanInstance();
			}

			if ( identifierGenerator instanceof Configurable ) {
				( ( Configurable ) identifierGenerator ).configure( type, config, serviceRegistry );
			}

			return identifierGenerator;
		}
		catch ( Exception e ) {
			final String entityName = config.getProperty( IdentifierGenerator.ENTITY_NAME );
			throw new MappingException( String.format( "Could not instantiate id generator [entity-name=%s]", entityName ), e );
		}
	}

	@Override
	public boolean canUseCachedReferences() {
		return false;
	}

	@Override
	public boolean useJpaCompliantCreation() {
		return true;
	}

	@Override
	public Class getIdentifierGeneratorClass(String strategy) {
		checkForDeprecatedStrategies( strategy );

		final String resolvedStrategy = "native".equals( strategy )
				? getDialect().getNativeIdentifierGeneratorStrategy()
				: strategy;

		final Class namedGeneratorClass = generatorStrategyToClassNameMap.get( resolvedStrategy );
		if ( namedGeneratorClass != null ) {
			return namedGeneratorClass;
		}

		try {
			final ClassLoaderService cls = serviceRegistry.getService( ClassLoaderService.class );
			return cls.classForName( resolvedStrategy );
		}
		catch ( ClassLoadingException e ) {
			throw new MappingException( String.format( "Could not interpret id generator strategy [%s]", strategy ) );
		}
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.dialect = serviceRegistry.getService( JdbcEnvironment.class ).getDialect();
	}

	public static final String ID_PACKAGE_NAME = "org.hibernate.id";

	private void checkForDeprecatedStrategies(String strategy) {
		if ( "hilo".equals( strategy ) ) {
			throw new UnsupportedOperationException( "Support for 'hilo' generator has been removed" );
		}

		if ( "seqhilo".equals( strategy ) || ( ID_PACKAGE_NAME + ".SequenceHiLoGenerator" ).equals( strategy ) ) {
			throw new UnsupportedOperationException( "Support for 'seqhilo' generator has been removed" );
		}

		if ( "sequence-identity".equals( strategy ) || ( ID_PACKAGE_NAME + ".SequenceIdentityGenerator" ).equals( strategy ) ) {
			throw new UnsupportedOperationException( "Support for 'sequence-identity' generator has been removed" );
		}

		if ( ( ID_PACKAGE_NAME + ".UUIDHexGenerator" ).equals( strategy ) ) {
			throw new UnsupportedOperationException( "Support for '" + ID_PACKAGE_NAME + ".UUIDHexGenerator' generator has been removed" );
		}
	}
}
