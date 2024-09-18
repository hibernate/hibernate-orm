/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.factory.internal;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.generator.Generator;
import org.hibernate.generator.GeneratorCreationContext;
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
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.id.factory.spi.GenerationTypeStrategy;
import org.hibernate.id.factory.spi.GenerationTypeStrategyRegistration;
import org.hibernate.id.factory.spi.GeneratorDefinitionResolver;
import org.hibernate.id.factory.spi.StandardGenerator;
import org.hibernate.jpa.spi.IdentifierGeneratorStrategyProvider;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.internal.Helper;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.GenerationType;

import static org.hibernate.cfg.AvailableSettings.IDENTIFIER_GENERATOR_STRATEGY_PROVIDER;
import static org.hibernate.id.factory.IdGenFactoryLogging.ID_GEN_FAC_LOGGER;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;

/**
 * Basic implementation of {@link org.hibernate.id.factory.IdentifierGeneratorFactory},
 * responsible for instantiating the predefined built-in id generators, and generators
 * declared using {@link org.hibernate.annotations.GenericGenerator}.
 *
 * @author Steve Ebersole
 */
public class StandardIdentifierGeneratorFactory
		implements IdentifierGeneratorFactory, BeanContainer.LifecycleOptions, Serializable {

	private final ConcurrentHashMap<GenerationType, GenerationTypeStrategy> generatorTypeStrategyMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Class<? extends Generator>> legacyGeneratorClassNameMap = new ConcurrentHashMap<>();

	private final ServiceRegistry serviceRegistry;
	private final BeanContainer beanContainer;

	private Dialect dialect;


	/**
	 * Constructs a new factory
	 */
	public StandardIdentifierGeneratorFactory(ServiceRegistry serviceRegistry) {
		this( serviceRegistry, !Helper.allowExtensionsInCdi( serviceRegistry ) );
	}

	/**
	 * Constructs a new factory, explicitly controlling whether to use CDI or not
	 */
	public StandardIdentifierGeneratorFactory(ServiceRegistry serviceRegistry, boolean ignoreBeanContainer) {
		this.serviceRegistry = serviceRegistry;
		beanContainer = getBeanContainer( serviceRegistry, ignoreBeanContainer );
		registerJpaGenerators();
		logOverrides();
		registerPredefinedGenerators();
		registerUsingLegacyContributor();
	}

	private static BeanContainer getBeanContainer(ServiceRegistry serviceRegistry, boolean ignoreBeanContainer) {
		if (ignoreBeanContainer) {
			ID_GEN_FAC_LOGGER.debug( "Ignoring CDI for resolving IdentifierGenerator instances as extended or delayed CDI support was enabled" );
			return null;
		}
		else {
			final BeanContainer beanContainer =
					serviceRegistry.requireService( ManagedBeanRegistry.class )
							.getBeanContainer();
			if ( beanContainer == null ) {
				ID_GEN_FAC_LOGGER.debug( "Resolving IdentifierGenerator instances will not use CDI as it was not configured" );
			}
			return beanContainer;
		}
	}

	private void registerJpaGenerators() {
		generatorTypeStrategyMap.put( GenerationType.AUTO, AutoGenerationTypeStrategy.INSTANCE );
		generatorTypeStrategyMap.put( GenerationType.SEQUENCE, SequenceGenerationTypeStrategy.INSTANCE );
		generatorTypeStrategyMap.put( GenerationType.TABLE, TableGenerationTypeStrategy.INSTANCE );
		generatorTypeStrategyMap.put( GenerationType.IDENTITY, IdentityGenerationTypeStrategy.INSTANCE );
		generatorTypeStrategyMap.put( GenerationType.UUID, UUIDGenerationTypeStrategy.INSTANCE );
	}

	private void logOverrides() {
		serviceRegistry.requireService( ClassLoaderService.class )
				.loadJavaServices( GenerationTypeStrategyRegistration.class )
				.forEach( (registration) -> registration.registerStrategies(
						(generationType, generationTypeStrategy) -> {
							final GenerationTypeStrategy previous =
									generatorTypeStrategyMap.put( generationType, generationTypeStrategy );
							if ( previous != null ) {
								ID_GEN_FAC_LOGGER.debugf(
										"GenerationTypeStrategyRegistration [%s] overrode previous registration for GenerationType#%s : %s",
										registration,
										generationType.name(),
										previous
								);
							}
						},
						serviceRegistry
				) );
	}

	private void registerPredefinedGenerators() {
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
	}

	private void registerUsingLegacyContributor() {
		final ConfigurationService configService = serviceRegistry.requireService( ConfigurationService.class );
		final Object providerSetting = configService.getSettings().get( IDENTIFIER_GENERATOR_STRATEGY_PROVIDER );
		if ( providerSetting != null ) {
			DEPRECATION_LOGGER.deprecatedSetting2(
					IDENTIFIER_GENERATOR_STRATEGY_PROVIDER,
					"supply a org.hibernate.id.factory.spi.GenerationTypeStrategyRegistration Java service"
			);
			final IdentifierGeneratorStrategyProvider idGeneratorStrategyProvider =
					serviceRegistry.requireService( StrategySelector.class )
							.resolveStrategy( IdentifierGeneratorStrategyProvider.class, providerSetting );
			for ( Map.Entry<String,Class<?>> entry : idGeneratorStrategyProvider.getStrategies().entrySet() ) {
				@SuppressWarnings({"rawtypes", "unchecked"})
				Class<? extends IdentifierGenerator> generatorClass = (Class) entry.getValue();
				register( entry.getKey(), generatorClass );
			}
		}
	}

	private void register(String strategy, Class<? extends Generator> generatorClass) {
		ID_GEN_FAC_LOGGER.debugf( "Registering IdentifierGenerator strategy [%s] -> [%s]", strategy, generatorClass.getName() );
		final Class<?> previous = legacyGeneratorClassNameMap.put( strategy, generatorClass );
		if ( previous != null && ID_GEN_FAC_LOGGER.isDebugEnabled() ) {
			ID_GEN_FAC_LOGGER.debugf( "    - overriding [%s]", previous.getName() );
		}
	}

	@Override
	public IdentifierGenerator createIdentifierGenerator(
			GenerationType generationType,
			String generatedValueGeneratorName,
			String generatorName,
			JavaType<?> javaType,
			Properties config,
			GeneratorDefinitionResolver definitionResolver) {
		final GenerationTypeStrategy strategy = generatorTypeStrategyMap.get( generationType );
		if ( strategy != null ) {
			return strategy.createIdentifierGenerator(
					generationType,
					generatorName,
					javaType,
					config,
					definitionResolver,
					serviceRegistry
			);
		}
		throw new UnsupportedOperationException( "No GenerationTypeStrategy specified" );
	}

	private Dialect getDialect() {
		if ( dialect == null ) {
			dialect = serviceRegistry.requireService( JdbcEnvironment.class ).getDialect();
		}
		return dialect;
	}

	@Override @Deprecated
	public Generator createIdentifierGenerator(
			String strategy, Type type, GeneratorCreationContext creationContext, Properties parameters) {
		try {
			final Class<? extends Generator> clazz = getIdentifierGeneratorClass( strategy );
			final Generator identifierGenerator;
			if ( beanContainer == null
					|| StandardGenerator.class.isAssignableFrom( clazz )
					|| legacyGeneratorClassNameMap.containsKey( strategy ) ) {
				identifierGenerator = clazz.newInstance();
			}
			else {
				identifierGenerator =
						beanContainer.getBean( clazz, this, FallbackBeanInstanceProducer.INSTANCE )
								.getBeanInstance();
			}

			if ( identifierGenerator instanceof Configurable ) {
				final Configurable configurable = (Configurable) identifierGenerator;
				if ( creationContext != null ) {
					configurable.create( creationContext );
				}
				configurable.configure( type, parameters, serviceRegistry );
			}
			return identifierGenerator;
		}
		catch ( Exception e ) {
			final String entityName = parameters.getProperty( IdentifierGenerator.ENTITY_NAME );
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

	private Class<? extends Generator> getIdentifierGeneratorClass(String strategy) {
		switch ( strategy ) {
			case "hilo":
				throw new UnsupportedOperationException( "Support for 'hilo' generator has been removed" );
			case "native":
				strategy = getDialect().getNativeIdentifierGeneratorStrategy();
				//then fall through:
			default:
				Class<? extends Generator> generatorClass = legacyGeneratorClassNameMap.get( strategy );
				return generatorClass != null ? generatorClass : generatorClassForName( strategy );
		}
	}

	private Class<? extends Generator> generatorClassForName(String strategy) {
		try {
			Class<? extends Generator> clazz =
					serviceRegistry.requireService( ClassLoaderService.class )
							.classForName( strategy );
			if ( !Generator.class.isAssignableFrom( clazz ) ) {
				// in principle, this shouldn't happen, since @GenericGenerator
				// constrains the type to subtypes of Generator
				throw new MappingException( clazz.getName() + " does not implement 'Generator'" );
			}
			return clazz;
		}
		catch ( ClassLoadingException e ) {
			throw new MappingException( String.format( "Could not interpret id generator strategy [%s]", strategy ) );
		}
	}
}
