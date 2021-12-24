/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.factory.internal;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.MappingException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.id.Assigned;
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
import org.hibernate.id.factory.IdGenFactoryLogging;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.id.factory.spi.GenerationTypeStrategy;
import org.hibernate.id.factory.spi.GenerationTypeStrategyRegistration;
import org.hibernate.id.factory.spi.GeneratorDefinitionResolver;
import org.hibernate.id.factory.spi.StandardGenerator;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.jpa.spi.IdentifierGeneratorStrategyProvider;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.GenerationType;

import static org.hibernate.id.factory.IdGenFactoryLogging.ID_GEN_FAC_LOGGER;

/**
 * Basic {@code templated} support for {@link org.hibernate.id.factory.IdentifierGeneratorFactory} implementations.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings( { "deprecation" ,"rawtypes" } )
public class StandardIdentifierGeneratorFactory
		implements IdentifierGeneratorFactory, BeanContainer.LifecycleOptions, Serializable {

	private final ConcurrentHashMap<GenerationType, GenerationTypeStrategy> generatorTypeStrategyMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Class<? extends IdentifierGenerator>> legacyGeneratorClassNameMap = new ConcurrentHashMap<>();

	private final ServiceRegistry serviceRegistry;
	private final BeanContainer beanContainer;

	private Dialect dialect;


	/**
	 * Constructs a new factory
	 */
	public StandardIdentifierGeneratorFactory(ServiceRegistry serviceRegistry) {
		this( serviceRegistry, shouldIgnoreBeanContainer( serviceRegistry ) );
	}

	private static boolean shouldIgnoreBeanContainer(ServiceRegistry serviceRegistry) {
		final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );
		final Object beanManagerRef = configService.getSettings().get( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER );

		if ( beanManagerRef instanceof ExtendedBeanManager ) {
			return true;
		}

		if ( configService.getSetting( AvailableSettings.DELAY_CDI_ACCESS, StandardConverters.BOOLEAN, false ) ) {
			return true;
		}

		return false;
	}

	/**
	 * Constructs a new factory, explicitly controlling whether to use
	 * CDI or not
	 */
	public StandardIdentifierGeneratorFactory(ServiceRegistry serviceRegistry, boolean ignoreBeanContainer) {
		this.serviceRegistry = serviceRegistry;

		if ( ignoreBeanContainer ) {
			ID_GEN_FAC_LOGGER.debug( "Ignoring CDI for resolving IdentifierGenerator instances as extended or delayed CDI support was enabled" );
			this.beanContainer = null;
		}
		else {
			this.beanContainer = serviceRegistry.getService( ManagedBeanRegistry.class ).getBeanContainer();
			if ( beanContainer == null ) {
				ID_GEN_FAC_LOGGER.debug( "Resolving IdentifierGenerator instances will not use CDI as it was not configured" );
			}
		}

		generatorTypeStrategyMap.put( GenerationType.AUTO, AutoGenerationTypeStrategy.INSTANCE );
		generatorTypeStrategyMap.put( GenerationType.SEQUENCE, SequenceGenerationTypeStrategy.INSTANCE );
		generatorTypeStrategyMap.put( GenerationType.TABLE, TableGenerationTypeStrategy.INSTANCE );
		generatorTypeStrategyMap.put( GenerationType.IDENTITY, IdentityGenerationTypeStrategy.INSTANCE );

		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		final Collection<GenerationTypeStrategyRegistration> generationTypeStrategyRegistrations = classLoaderService.loadJavaServices( GenerationTypeStrategyRegistration.class );
		generationTypeStrategyRegistrations.forEach( (registration) -> registration.registerStrategies(
				(generationType, generationTypeStrategy) -> {
					final GenerationTypeStrategy previous = generatorTypeStrategyMap.put(
							generationType,
							generationTypeStrategy
					);
					if ( previous != null ) {
						IdGenFactoryLogging.ID_GEN_FAC_LOGGER.debugf(
								"GenerationTypeStrategyRegistration [%s] overrode previous registration for GenerationType#%s : %s",
								registration,
								generationType.name(),
								previous
						);
					}
				},
				serviceRegistry
		) );

		register( "uuid2", UUIDGenerator.class );
		// can be done with UuidGenerator + strategy
		register( "guid", GUIDGenerator.class );
		register( "uuid", UUIDHexGenerator.class );			// "deprecated" for new use
		register( "uuid.hex", UUIDHexGenerator.class ); 	// uuid.hex is deprecated
		register( "assigned", Assigned.class );
		register( "identity", IdentityGenerator.class );
		register( "select", SelectGenerator.class );
		register( "sequence", SequenceStyleGenerator.class );
		register( "increment", IncrementGenerator.class );
		register( "foreign", ForeignGenerator.class );
		register( "enhanced-sequence", SequenceStyleGenerator.class );
		register( "enhanced-table", TableGenerator.class );

		final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );
		final Object providerSetting = configService.getSettings().get( AvailableSettings.IDENTIFIER_GENERATOR_STRATEGY_PROVIDER );
		if ( providerSetting != null ) {
			DeprecationLogger.DEPRECATION_LOGGER.deprecatedSetting2(
					AvailableSettings.IDENTIFIER_GENERATOR_STRATEGY_PROVIDER,
					"supply a org.hibernate.id.factory.spi.GenerationTypeStrategyRegistration Java service"
			);
			final IdentifierGeneratorStrategyProvider idGeneratorStrategyProvider = serviceRegistry.getService( StrategySelector.class ).resolveStrategy(
					IdentifierGeneratorStrategyProvider.class,
					providerSetting
			);
			for ( Map.Entry<String,Class<?>> entry : idGeneratorStrategyProvider.getStrategies().entrySet() ) {
				register( entry.getKey(), (Class) entry.getValue() );
			}
		}
	}

	private void register(String strategy, Class<? extends IdentifierGenerator> generatorClass) {
		ID_GEN_FAC_LOGGER.debugf( "Registering IdentifierGenerator strategy [%s] -> [%s]", strategy, generatorClass.getName() );
		final Class previous = legacyGeneratorClassNameMap.put( strategy, generatorClass );
		if ( previous != null && ID_GEN_FAC_LOGGER.isDebugEnabled() ) {
			ID_GEN_FAC_LOGGER.debugf( "    - overriding [%s]", previous.getName() );
		}
	}

	@Override
	public IdentifierGenerator createIdentifierGenerator(
			GenerationType generationType,
			String generatedValueGeneratorName,
			String generatorName,
			JavaType<?> javaTypeDescriptor,
			Properties config,
			GeneratorDefinitionResolver definitionResolver) {
		final GenerationTypeStrategy strategy = generatorTypeStrategyMap.get( generationType );
		if ( strategy != null ) {
			return strategy.createIdentifierGenerator(
					generationType,
					generatorName,
					javaTypeDescriptor,
					config,
					definitionResolver,
					serviceRegistry
			);
		}
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Dialect getDialect() {
		if ( dialect == null ) {
			dialect = serviceRegistry.getService( JdbcEnvironment.class ).getDialect();
		}
		return dialect;
	}

	@Override
	public IdentifierGenerator createIdentifierGenerator(String strategy, Type type, Properties config) {
		try {
			final Class<? extends IdentifierGenerator> clazz = getIdentifierGeneratorClass( strategy );
			final IdentifierGenerator identifierGenerator;

			if ( beanContainer == null
					|| StandardGenerator.class.isAssignableFrom( clazz )
					|| legacyGeneratorClassNameMap.containsKey( strategy ) ) {
				identifierGenerator = clazz.newInstance();
			}
			else {
				final ContainedBean<? extends IdentifierGenerator> generatorBean = beanContainer.getBean(
						clazz,
						this,
						FallbackBeanInstanceProducer.INSTANCE
				);
				identifierGenerator = generatorBean.getBeanInstance();
			}

			identifierGenerator.configure( type, config, serviceRegistry );
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
	public Class<? extends IdentifierGenerator> getIdentifierGeneratorClass(String strategy) {
		if ( "hilo".equals( strategy ) ) {
			throw new UnsupportedOperationException( "Support for 'hilo' generator has been removed" );
		}
		String resolvedStrategy = "native".equals( strategy )
				? getDialect().getNativeIdentifierGeneratorStrategy()
				: strategy;

		Class generatorClass = legacyGeneratorClassNameMap.get( resolvedStrategy );
		try {
			if ( generatorClass == null ) {
				final ClassLoaderService cls = serviceRegistry.getService( ClassLoaderService.class );
				generatorClass = cls.classForName( resolvedStrategy );
			}
		}
		catch ( ClassLoadingException e ) {
			throw new MappingException( String.format( "Could not interpret id generator strategy [%s]", strategy ) );
		}
		return generatorClass;
	}
}
