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

import org.hibernate.boot.cfgxml.internal.ConfigLoader;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.IntegratorService;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.StandardServiceInitiators;
import org.hibernate.service.internal.ProvidedService;
import org.hibernate.service.spi.ServiceContributor;

/**
 * Builder for standard {@link org.hibernate.service.ServiceRegistry} instances.
 *
 * @author Steve Ebersole
 * 
 * @see StandardServiceRegistryImpl
 * @see org.hibernate.boot.registry.BootstrapServiceRegistryBuilder
 */
public class StandardServiceRegistryBuilder {
	/**
	 * Intended only for use from {@link org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl}.
	 *
	 * Creates a StandardServiceRegistryBuilder specific to the needs of JPA bootstrapping.
	 * Specifically we ignore properties found in `cfg.xml` files in terms of adding them to
	 * the builder immediately.  EntityManagerFactoryBuilderImpl handles collecting these
	 * properties itself.
	 */
	public static StandardServiceRegistryBuilder forJpa(BootstrapServiceRegistry bootstrapServiceRegistry) {
		final LoadedConfig loadedConfig = new LoadedConfig( null ) {
			@Override
			protected void addConfigurationValues(Map configurationValues) {
				// here, do nothing
			}
		};
		return new StandardServiceRegistryBuilder(
				bootstrapServiceRegistry,
				new HashMap(),
				loadedConfig
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
	 * The default resource name for a hibernate configuration xml file.
	 */
	public static final String DEFAULT_CFG_RESOURCE_NAME = "hibernate.cfg.xml";

	private final Map settings;
	private final List<StandardServiceInitiator> initiators;
	private final List<ProvidedService> providedServices = new ArrayList<>();

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
	 * Intended for use exclusively from JPA boot-strapping, or extensions of
	 * this class. Consider this an SPI.
	 *
	 * @see #forJpa
	 */
	protected StandardServiceRegistryBuilder(
			BootstrapServiceRegistry bootstrapServiceRegistry,
			Map settings,
			LoadedConfig loadedConfig) {
		this.bootstrapServiceRegistry = bootstrapServiceRegistry;
		this.configLoader = new ConfigLoader( bootstrapServiceRegistry );
		this.settings = settings;
		this.aggregatedCfgXml = loadedConfig;
		this.initiators = standardInitiatorList();
	}

	/**
	 * Intended for use exclusively from Quarkus boot-strapping, or extensions of
	 * this class which need to override the standard ServiceInitiator list.
	 * Consider this an SPI.
	 */
	protected StandardServiceRegistryBuilder(
			BootstrapServiceRegistry bootstrapServiceRegistry,
			Map settings,
			LoadedConfig loadedConfig,
			List<StandardServiceInitiator> initiators) {
		this.bootstrapServiceRegistry = bootstrapServiceRegistry;
		this.configLoader = new ConfigLoader( bootstrapServiceRegistry );
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
		this.settings = Environment.getProperties();
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
	private static List<StandardServiceInitiator> standardInitiatorList() {
		final List<StandardServiceInitiator> initiators = new ArrayList<>( StandardServiceInitiators.LIST.size() );
		initiators.addAll( StandardServiceInitiators.LIST );
		return initiators;
	}

	@SuppressWarnings("unused")
	public BootstrapServiceRegistry getBootstrapServiceRegistry() {
		return bootstrapServiceRegistry;
	}

	/**
	 * Read settings from a {@link java.util.Properties} file by resource name.
	 * <p>
	 * Differs from {@link #configure()} and {@link #configure(String)} in that here we expect to read a
	 * {@link java.util.Properties} file while for {@link #configure} we read the XML variant.
	 *
	 * @param resourceName The name by which to perform a resource look up for the properties file.
	 *
	 * @return this, for method chaining
	 *
	 * @see #configure()
	 * @see #configure(String)
	 */
	@SuppressWarnings({"unchecked"})
	public StandardServiceRegistryBuilder loadProperties(String resourceName) {
		settings.putAll( configLoader.loadProperties( resourceName ) );
		return this;
	}

	/**
	 * Read settings from a {@link java.util.Properties} file by File reference
	 * <p>
	 * Differs from {@link #configure()} and {@link #configure(String)} in that here we expect to read a
	 * {@link java.util.Properties} file while for {@link #configure} we read the XML variant.
	 *
	 * @param file The properties File reference
	 *
	 * @return this, for method chaining
	 *
	 * @see #configure()
	 * @see #configure(String)
	 */
	@SuppressWarnings({"unchecked"})
	public StandardServiceRegistryBuilder loadProperties(File file) {
		settings.putAll( configLoader.loadProperties( file ) );
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

	@SuppressWarnings({"unchecked"})
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
	@SuppressWarnings({"unchecked", "UnusedDeclaration"})
	public StandardServiceRegistryBuilder applySetting(String settingName, Object value) {
		settings.put( settingName, value );
		return this;
	}

	/**
	 * Apply a groups of setting values.
	 *
	 * @param settings The incoming settings to apply
	 *
	 * @return this, for method chaining
	 */
	@SuppressWarnings({"unchecked", "UnusedDeclaration"})
	public StandardServiceRegistryBuilder applySettings(Map settings) {
		this.settings.putAll( settings );
		return this;
	}

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
	@SuppressWarnings({"UnusedDeclaration"})
	public StandardServiceRegistryBuilder addInitiator(StandardServiceInitiator initiator) {
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
	@SuppressWarnings({"unchecked"})
	public StandardServiceRegistryBuilder addService(final Class serviceRole, final Service service) {
		providedServices.add( new ProvidedService( serviceRole, service ) );
		return this;
	}

	/**
	 * By default, when a ServiceRegistry is no longer referenced by any other
	 * registries as a parent it will be closed.
	 * <p/>
	 * Some applications that explicitly build "shared registries" may want to
	 * circumvent that behavior.
	 * <p/>
	 * This method indicates that the registry being built should not be
	 * automatically closed.  The caller agrees to take responsibility to
	 * close it themselves.
	 *
	 * @return this, for method chaining
	 */
	public StandardServiceRegistryBuilder disableAutoClose() {
		this.autoCloseRegistry = false;
		return this;
	}

	/**
	 * See the discussion on {@link #disableAutoClose}.  This method enables
	 * the auto-closing.
	 *
	 * @return this, for method chaining
	 */
	public StandardServiceRegistryBuilder enableAutoClose() {
		this.autoCloseRegistry = true;
		return this;
	}

	/**
	 * Build the StandardServiceRegistry.
	 *
	 * @return The StandardServiceRegistry.
	 */
	@SuppressWarnings("unchecked")
	public StandardServiceRegistry build() {
		applyServiceContributingIntegrators();
		applyServiceContributors();

		final Map settingsCopy = new HashMap( settings );
		settingsCopy.put( org.hibernate.boot.cfgxml.spi.CfgXmlAccessService.LOADED_CONFIG_KEY, aggregatedCfgXml );
		ConfigurationHelper.resolvePlaceHolders( settingsCopy );

		return new StandardServiceRegistryImpl(
				autoCloseRegistry,
				bootstrapServiceRegistry,
				initiators,
				providedServices,
				settingsCopy
		);
	}

	@SuppressWarnings("deprecation")
	private void applyServiceContributingIntegrators() {
		for ( Integrator integrator : bootstrapServiceRegistry.getService( IntegratorService.class )
				.getIntegrators() ) {
			if ( org.hibernate.integrator.spi.ServiceContributingIntegrator.class.isInstance( integrator ) ) {
				org.hibernate.integrator.spi.ServiceContributingIntegrator.class.cast( integrator ).prepareServices(
						this );
			}
		}
	}

	private void applyServiceContributors() {
		final Iterable<ServiceContributor> serviceContributors =
				bootstrapServiceRegistry.getService( ClassLoaderService.class )
						.loadJavaServices( ServiceContributor.class );

		for ( ServiceContributor serviceContributor : serviceContributors ) {
			serviceContributor.contribute( this );
		}
	}

	/**
	 * Temporarily exposed since Configuration is still around and much code still uses Configuration.  This allows
	 * code to configure the builder and access that to configure Configuration object (used from HEM atm).
	 *
	 * @return The settings map.
	 *
	 * @deprecated Temporarily exposed since Configuration is still around and much code still uses Configuration.
	 * This allows code to configure the builder and access that to configure Configuration object.
	 */
	@Deprecated
	public Map getSettings() {
		return settings;
	}

	/**
	 * Destroy a service registry.  Applications should only destroy registries they have explicitly created.
	 *
	 * @param serviceRegistry The registry to be closed.
	 */
	public static void destroy(ServiceRegistry serviceRegistry) {
		if ( serviceRegistry == null ) {
			return;
		}

		( (StandardServiceRegistryImpl) serviceRegistry ).destroy();
	}
}
