/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.Internal;
import org.hibernate.boot.cfgxml.internal.ConfigLoader;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.StandardServiceInitiators;
import org.hibernate.service.internal.ProvidedService;
import org.hibernate.service.spi.ServiceContributor;

import static org.hibernate.boot.cfgxml.spi.CfgXmlAccessService.LOADED_CONFIG_KEY;

/**
 * Builder for standard {@link ServiceRegistry} instances.
 * <p>
 * Configuration properties are enumerated by {@link AvailableSettings}.
 *
 * @author Steve Ebersole
 * 
 * @see StandardServiceRegistryImpl
 * @see BootstrapServiceRegistryBuilder
 */
public class StandardServiceRegistryBuilder {
	/**
	 * Creates a {@code StandardServiceRegistryBuilder} specific to the needs
	 * of bootstrapping JPA.
	 * <p>
	 * Intended only for use from
	 * {@link org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl}.
	 * <p>
	 * In particular, we ignore properties found in {@code cfg.xml} files.
	 * {@code EntityManagerFactoryBuilderImpl} collects these properties later.
	 *
	 * @see org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl
	 */
	public static StandardServiceRegistryBuilder forJpa(BootstrapServiceRegistry bootstrapServiceRegistry) {
		return new StandardServiceRegistryBuilder(
				bootstrapServiceRegistry,
				new HashMap<>(),
				new LoadedConfig( null ) {
					@Override
					protected void addConfigurationValues(Map<String,Object> configurationValues) {
						// here, do nothing
					}
				}
		) {
			@Override
			public StandardServiceRegistryBuilder configure(LoadedConfig loadedConfig) {
				getAggregatedCfgXml().merge( loadedConfig );
				// super also collects the properties - here we skip that part
				return this;
			}
		};
	}

	/**
	 * The default resource name for a Hibernate configuration XML file.
	 */
	public static final String DEFAULT_CFG_RESOURCE_NAME = "hibernate.cfg.xml";

	private final Map<String,Object> settings;
	private final List<StandardServiceInitiator<?>> initiators;
	private final List<ProvidedService<?>> providedServices = new ArrayList<>();

	private boolean autoCloseRegistry = true;

	private final BootstrapServiceRegistry bootstrapServiceRegistry;
	private final ConfigLoader configLoader;
	private final LoadedConfig aggregatedCfgXml;

	/**
	 * Create a default builder.
	 */
	public StandardServiceRegistryBuilder() {
		this( new BootstrapServiceRegistryBuilder().enableAutoClose().build() );
	}

	/**
	 * Create a builder with the specified bootstrap services.
	 *
	 * @param bootstrapServiceRegistry Provided bootstrap registry to use.
	 */
	public StandardServiceRegistryBuilder(BootstrapServiceRegistry bootstrapServiceRegistry) {
		this( bootstrapServiceRegistry, LoadedConfig.baseline() );
	}

	/**
	 * Intended for use exclusively from JPA bootstrapping, or extensions of this
	 * class.
	 *
	 * Consider this an SPI.
	 *
	 * @see #forJpa
	 */
	protected StandardServiceRegistryBuilder(
			BootstrapServiceRegistry bootstrapServiceRegistry,
			Map<String,Object> settings,
			LoadedConfig loadedConfig) {
		this.bootstrapServiceRegistry = bootstrapServiceRegistry;
		this.configLoader = new ConfigLoader( bootstrapServiceRegistry );
		this.settings = settings;
		this.aggregatedCfgXml = loadedConfig;
		this.initiators = standardInitiatorList();
	}

	/**
	 * Intended for use exclusively from Quarkus bootstrapping, or extensions of
	 * this class which need to override the standard ServiceInitiator list.
	 *
	 * Consider this an SPI.
	 */
	protected StandardServiceRegistryBuilder(
			BootstrapServiceRegistry bootstrapServiceRegistry,
			Map<String,Object> settings,
			ConfigLoader loader,
			LoadedConfig loadedConfig,
			List<StandardServiceInitiator<?>> initiators) {
		this.bootstrapServiceRegistry = bootstrapServiceRegistry;
		this.configLoader = loader;
		this.settings = settings;
		this.aggregatedCfgXml = loadedConfig;
		this.initiators = initiators;
	}

	/**
	 * Create a builder with the specified bootstrap services.
	 *
	 * @param bootstrapServiceRegistry Provided bootstrap registry to use.
	 */
	public StandardServiceRegistryBuilder(
			BootstrapServiceRegistry bootstrapServiceRegistry,
			LoadedConfig loadedConfigBaseline) {
		this.settings = PropertiesHelper.map( Environment.getProperties() );
		this.bootstrapServiceRegistry = bootstrapServiceRegistry;
		this.configLoader = new ConfigLoader( bootstrapServiceRegistry );
		this.aggregatedCfgXml = loadedConfigBaseline;
		this.initiators = standardInitiatorList();
	}

	public ConfigLoader getConfigLoader() {
		return configLoader;
	}

	/**
	 * Intended for internal testing use only!!
	 */
	public LoadedConfig getAggregatedCfgXml() {
		return aggregatedCfgXml;
	}

	/**
	 * Used from the {@link #initiators} variable initializer
	 *
	 * @return List of standard initiators
	 */
	private static List<StandardServiceInitiator<?>> standardInitiatorList() {
		return new ArrayList<>( StandardServiceInitiators.LIST );
	}

	public BootstrapServiceRegistry getBootstrapServiceRegistry() {
		return bootstrapServiceRegistry;
	}

	/**
	 * Read settings from a {@link java.util.Properties} file by resource name.
	 * <p>
	 * Differs from {@link #configure()} and {@link #configure(String)} in that
	 * here we expect to read a {@linkplain java.util.Properties properties} file,
	 * while for {@link #configure} we read the configuration from XML.
	 *
	 * @param resourceName The name by which to perform a resource look up for the properties file
	 *
	 * @return this, for method chaining
	 *
	 * @see #configure()
	 * @see #configure(String)
	 */
	public StandardServiceRegistryBuilder loadProperties(String resourceName) {
		settings.putAll( PropertiesHelper.map( configLoader.loadProperties( resourceName ) ) );
		return this;
	}

	/**
	 * Read settings from a {@link java.util.Properties} file by File reference
	 * <p>
	 * Differs from {@link #configure()} and {@link #configure(String)} in that
	 * here we expect to read a {@linkplain java.util.Properties properties} file,
	 * while for {@link #configure} we read the configuration from XML.
	 *
	 * @param file The properties File reference
	 *
	 * @return this, for method chaining
	 *
	 * @see #configure()
	 * @see #configure(String)
	 */
	public StandardServiceRegistryBuilder loadProperties(File file) {
		settings.putAll( PropertiesHelper.map( configLoader.loadProperties( file ) ) );
		return this;
	}

	/**
	 * Read setting information from an XML file using the standard resource location.
	 *
	 * @return this, for method chaining
	 *
	 * @see #DEFAULT_CFG_RESOURCE_NAME
	 * @see #configure(String)
	 * @see #loadProperties(String)
	 */
	public StandardServiceRegistryBuilder configure() {
		return configure( DEFAULT_CFG_RESOURCE_NAME );
	}

	/**
	 * Read setting information from an XML file using the named resource location.
	 *
	 * @param resourceName The named resource
	 *
	 * @return this, for method chaining
	 */
	public StandardServiceRegistryBuilder configure(String resourceName) {
		return configure( configLoader.loadConfigXmlResource( resourceName ) );
	}

	public StandardServiceRegistryBuilder configure(File configurationFile) {
		return configure( configLoader.loadConfigXmlFile( configurationFile ) );
	}

	public StandardServiceRegistryBuilder configure(URL url) {
		return configure( configLoader.loadConfigXmlUrl( url ) );
	}

	public StandardServiceRegistryBuilder configure(LoadedConfig loadedConfig) {
		aggregatedCfgXml.merge( loadedConfig );
		settings.putAll( loadedConfig.getConfigurationValues() );

		return this;
	}

	/**
	 * Apply a setting value.
	 *
	 * @param settingName The name of the setting
	 * @param value The value to use.
	 *
	 * @return this, for method chaining
	 */
	public StandardServiceRegistryBuilder applySetting(String settingName, Object value) {
		settings.put( settingName, value );
		return this;
	}

	/**
	 * Apply a group of settings.
	 *
	 * @param settings The incoming settings to apply
	 *
	 * @return this, for method chaining
	 */
	public StandardServiceRegistryBuilder applySettings(Map<String,Object> settings) {
		this.settings.putAll( settings );
		return this;
	}

	/**
	 * Apply a group of settings.
	 *
	 * @param settings The incoming settings to apply
	 *
	 * @return this, for method chaining
	 */
	public StandardServiceRegistryBuilder applySettings(Properties settings) {
		this.settings.putAll( PropertiesHelper.map(settings) );
		return this;
	}

	/**
	 * Discard all the settings applied so far.
	 */
	public void clearSettings() {
		settings.clear();
	}

	/**
	 * Adds a service initiator.
	 *
	 * @param initiator The initiator to be added
	 *
	 * @return this, for method chaining
	 */
	public StandardServiceRegistryBuilder addInitiator(StandardServiceInitiator<?> initiator) {
		initiators.add( initiator );
		return this;
	}

	/**
	 * Adds a user-provided service.
	 *
	 * @param serviceRole The role of the service being added
	 * @param service The service implementation
	 *
	 * @return this, for method chaining
	 */
	public <T extends Service> StandardServiceRegistryBuilder addService(Class<T> serviceRole, T service) {
		providedServices.add( new ProvidedService<>( serviceRole, service ) );
		return this;
	}

	/**
	 * By default, when a {@link ServiceRegistry} is no longer referenced by any
	 * other registries as a parent it will be closed. Some applications that
	 * explicitly build "shared registries" may need to circumvent that behavior.
	 * <p>
	 * This method indicates that the registry being built should not be
	 * automatically closed. The caller takes responsibility for closing it.
	 *
	 * @return this, for method chaining
	 */
	public StandardServiceRegistryBuilder disableAutoClose() {
		autoCloseRegistry = false;
		return this;
	}

	/**
	 * Enables {@link #disableAutoClose auto-closing}.
	 *
	 * @return this, for method chaining
	 */
	public StandardServiceRegistryBuilder enableAutoClose() {
		autoCloseRegistry = true;
		return this;
	}

	/**
	 * Build and return the {@link StandardServiceRegistry}.
	 *
	 * @return A newly-instantiated {@link StandardServiceRegistry}
	 */
	public StandardServiceRegistry build() {
		applyServiceContributors();

		final Map<String,Object> settingsCopy = new HashMap<>( settings );
		settingsCopy.put( LOADED_CONFIG_KEY, aggregatedCfgXml );
		ConfigurationHelper.resolvePlaceHolders( settingsCopy );

		return StandardServiceRegistryImpl.create(
				autoCloseRegistry,
				bootstrapServiceRegistry,
				initiators,
				providedServices,
				settingsCopy
		);
	}

	private void applyServiceContributors() {
		final Iterable<ServiceContributor> serviceContributors =
				bootstrapServiceRegistry.requireService( ClassLoaderService.class )
						.loadJavaServices( ServiceContributor.class );

		for ( ServiceContributor serviceContributor : serviceContributors ) {
			serviceContributor.contribute( this );
		}
	}

	/**
	 * Obtain the current aggregated settings.
	 */
	@Internal
	public Map<String,Object> getSettings() {
		return settings;
	}

	/**
	 * Destroy a service registry.
	 * <p>
	 * Applications should only destroy registries they have explicitly created.
	 *
	 * @param serviceRegistry The registry to be closed.
	 */
	public static void destroy(ServiceRegistry serviceRegistry) {
		if ( serviceRegistry != null ) {
			( (StandardServiceRegistryImpl) serviceRegistry ).destroy();
		}
	}
}
