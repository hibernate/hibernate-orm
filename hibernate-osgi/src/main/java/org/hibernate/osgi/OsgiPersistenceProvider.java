/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.osgi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;

import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.jpa.boot.spi.StrategyRegistrationProviderList;
import org.hibernate.jpa.boot.spi.TypeContributorList;
import org.hibernate.metamodel.spi.TypeContributor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;

/**
 * Acts as the PersistenceProvider service in OSGi environments
 *
 * @author Brett Meyer
 * @author Tim Ward
 */
public class OsgiPersistenceProvider extends HibernatePersistenceProvider {
	private OsgiClassLoader osgiClassLoader;
	private OsgiJtaPlatform osgiJtaPlatform;
	private Bundle requestingBundle;
	private BundleContext context;

	/**
	 * Constructs a OsgiPersistenceProvider
	 *
	 * @param osgiClassLoader The ClassLoader we built from OSGi Bundles
	 * @param osgiJtaPlatform The OSGi-specific JtaPlatform impl we built
	 * @param requestingBundle The OSGi Bundle requesting the PersistenceProvider
	 * @param context The OSGi context
	 */
	public OsgiPersistenceProvider(
			OsgiClassLoader osgiClassLoader,
			OsgiJtaPlatform osgiJtaPlatform,
			Bundle requestingBundle,
			BundleContext context) {
		this.osgiClassLoader = osgiClassLoader;
		this.osgiJtaPlatform = osgiJtaPlatform;
		this.requestingBundle = requestingBundle;
		this.context = context;
	}

	// TODO: Does "hibernate.classloaders" and osgiClassLoader need added to the
	// EMFBuilder somehow?

	@Override
	@SuppressWarnings("unchecked")
	public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map properties) {
		final Map settings = generateSettings( properties );

		// TODO: This needs tested.
		settings.put( org.hibernate.jpa.AvailableSettings.SCANNER, new OsgiScanner( requestingBundle ) );
		// TODO: This is temporary -- for PersistenceXmlParser's use of
		// ClassLoaderServiceImpl#fromConfigSettings
		settings.put( AvailableSettings.ENVIRONMENT_CLASSLOADER, osgiClassLoader );

		osgiClassLoader.addBundle( requestingBundle );

		final EntityManagerFactoryBuilder builder = getEntityManagerFactoryBuilderOrNull( persistenceUnitName, settings, osgiClassLoader );
		return builder == null ? null : builder.build();
	}

	@Override
	@SuppressWarnings("unchecked")
	public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
		final Map settings = generateSettings( properties );

		// OSGi ClassLoaders must implement BundleReference
		settings.put(
				org.hibernate.jpa.AvailableSettings.SCANNER,
				new OsgiScanner( ( (BundleReference) info.getClassLoader() ).getBundle() )
		);

		osgiClassLoader.addClassLoader( info.getClassLoader() );

		return Bootstrap.getEntityManagerFactoryBuilder( info, settings, osgiClassLoader ).build();
	}

	@SuppressWarnings("unchecked")
	private Map generateSettings(Map properties) {
		final Map settings = new HashMap();
		if ( properties != null ) {
			settings.putAll( properties );
		}

		settings.put( AvailableSettings.JTA_PLATFORM, osgiJtaPlatform );

		final List<Integrator> integrators = OsgiServiceUtil.getServiceImpls( Integrator.class, context );
		final IntegratorProvider integratorProvider = new IntegratorProvider() {
			@Override
			public List<Integrator> getIntegrators() {
				return integrators;
			}
		};
		settings.put( EntityManagerFactoryBuilderImpl.INTEGRATOR_PROVIDER, integratorProvider );

		final List<StrategyRegistrationProvider> strategyRegistrationProviders = OsgiServiceUtil.getServiceImpls(
				StrategyRegistrationProvider.class, context );
		final StrategyRegistrationProviderList strategyRegistrationProviderList = new StrategyRegistrationProviderList() {
			@Override
			public List<StrategyRegistrationProvider> getStrategyRegistrationProviders() {
				return strategyRegistrationProviders;
			}
		};
		settings.put( EntityManagerFactoryBuilderImpl.STRATEGY_REGISTRATION_PROVIDERS, strategyRegistrationProviderList );

		final List<TypeContributor> typeContributors = OsgiServiceUtil.getServiceImpls( TypeContributor.class, context );
		final TypeContributorList typeContributorList = new TypeContributorList() {
			@Override
			public List<TypeContributor> getTypeContributors() {
				return typeContributors;
			}
		};
		settings.put( EntityManagerFactoryBuilderImpl.TYPE_CONTRIBUTORS, typeContributorList );
		
		return settings;
	}
}
