/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.jboss.as.jpa.hibernate5;

import java.util.Map;
import java.util.Properties;
import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.SharedCacheMode;
import javax.persistence.spi.PersistenceUnitInfo;

import org.hibernate.cfg.AvailableSettings;

import org.jboss.as.jpa.hibernate5.management.HibernateManagementAdaptor;

import org.jipijapa.cache.spi.Classification;
import org.jipijapa.event.impl.internal.Notification;
import org.jipijapa.plugin.spi.EntityManagerFactoryBuilder;
import org.jipijapa.plugin.spi.JtaManager;
import org.jipijapa.plugin.spi.ManagementAdaptor;
import org.jipijapa.plugin.spi.PersistenceProviderAdaptor;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;
import org.jipijapa.plugin.spi.Platform;
import org.jipijapa.plugin.spi.TwoPhaseBootstrapCapable;

import static org.jboss.as.jpa.hibernate5.JpaLogger.JPA_LOGGER;

/**
 * Implements the PersistenceProviderAdaptor for Hibernate
 *
 * @author Scott Marlow
 * @author Steve Ebersole
 */
public class HibernatePersistenceProviderAdaptor implements PersistenceProviderAdaptor, TwoPhaseBootstrapCapable {

	@SuppressWarnings("WeakerAccess")
	public static final String NAMING_STRATEGY_JPA_COMPLIANT_IMPL = "org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl";

	@SuppressWarnings("WeakerAccess")
	public static final String HIBERNATE_EXTENDED_BEANMANAGER = "org.hibernate.resource.beans.container.spi.ExtendedBeanManager";

	private volatile Platform platform;
	private static final String NONE = SharedCacheMode.NONE.name();

	@Override
	public void injectJtaManager(JtaManager jtaManager) { }

	@Override
	public void injectPlatform(Platform platform) {
		if ( this.platform != platform ) {
			this.platform = platform;
		}
	}

	@Override
	@SuppressWarnings({"deprecation", "unchecked"})
	public void addProviderProperties(Map properties, PersistenceUnitMetadata pu) {
		putPropertyIfAbsent( pu, properties, AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );
		putPropertyIfAbsent( pu, properties, AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED, "false" );
		putPropertyIfAbsent(
				pu,
				properties,
				AvailableSettings.IMPLICIT_NAMING_STRATEGY,
				NAMING_STRATEGY_JPA_COMPLIANT_IMPL
		);
		putPropertyIfAbsent( pu, properties, AvailableSettings.SCANNER, HibernateArchiveScanner.class );
		properties.put( AvailableSettings.APP_CLASSLOADER, pu.getClassLoader() );
		putPropertyIfAbsent(
				pu,
				properties,
				org.hibernate.ejb.AvailableSettings.ENTITY_MANAGER_FACTORY_NAME,
				pu.getScopedPersistenceUnitName()
		);
		putPropertyIfAbsent(
				pu,
				properties,
				AvailableSettings.SESSION_FACTORY_NAME,
				pu.getScopedPersistenceUnitName()
		);
		if ( !pu.getProperties().containsKey( AvailableSettings.SESSION_FACTORY_NAME ) ) {
			putPropertyIfAbsent( pu, properties, AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, Boolean.FALSE );
		}
		// the following properties were added to Hibernate ORM 5.3, for JPA 2.2 spec compliance.
		putPropertyIfAbsent( pu, properties, AvailableSettings.PREFER_GENERATOR_NAME_AS_DEFAULT_SEQUENCE_NAME, true );
		putPropertyIfAbsent( pu, properties, AvailableSettings.JPA_TRANSACTION_COMPLIANCE, true );
		putPropertyIfAbsent( pu, properties, AvailableSettings.JPA_CLOSED_COMPLIANCE, true );
		putPropertyIfAbsent( pu, properties, AvailableSettings.JPA_QUERY_COMPLIANCE, true );
		putPropertyIfAbsent( pu, properties, AvailableSettings.JPA_LIST_COMPLIANCE, true );
		putPropertyIfAbsent( pu, properties, AvailableSettings.JPA_CACHING_COMPLIANCE, true );
		// end of properties added for JPA 2.2 spec compliance.

	}

	@Override
	public void addProviderDependencies(PersistenceUnitMetadata pu) {
		final Properties properties = pu.getProperties();
		final String sharedCacheMode = properties.getProperty( AvailableSettings.JPA_SHARED_CACHE_MODE );

		if ( Classification.NONE.equals( platform.defaultCacheClassification() ) ) {
			if ( !SharedCacheMode.NONE.equals( pu.getSharedCacheMode() ) ) {
				JPA_LOGGER.trace( "second level cache is not supported in platform, ignoring shared cache mode" );
			}
			pu.setSharedCacheMode( SharedCacheMode.NONE );
		}
		// check if 2lc is explicitly disabled which takes precedence over other settings
		boolean sharedCacheDisabled = SharedCacheMode.NONE.equals( pu.getSharedCacheMode() )
				||
				NONE.equals( sharedCacheMode );

		if ( !sharedCacheDisabled &&
				Boolean.parseBoolean( properties.getProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE ) )
				||
				( sharedCacheMode != null && ( !NONE.equals( sharedCacheMode ) ) )
				|| ( !SharedCacheMode.NONE.equals( pu.getSharedCacheMode() ) && ( !SharedCacheMode.UNSPECIFIED.equals(
				pu.getSharedCacheMode() ) ) ) ) {
//            HibernateSecondLevelCache.addSecondLevelCacheDependencies(pu.getProperties(), pu.getScopedPersistenceUnitName());
			JPA_LOGGER.tracef( "second level cache enabled for %s", pu.getScopedPersistenceUnitName() );
		}
		else {
			if ( JPA_LOGGER.isTraceEnabled() ) {
				JPA_LOGGER.tracef(
						"second level cache disabled for %s, pu %s property = %s, pu.getSharedCacheMode = %s",
						pu.getScopedPersistenceUnitName(),
						AvailableSettings.JPA_SHARED_CACHE_MODE,
						sharedCacheMode,
						pu.getSharedCacheMode().toString()
				);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void putPropertyIfAbsent(PersistenceUnitMetadata pu, Map properties, String property, Object value) {
		if ( !pu.getProperties().containsKey( property ) ) {
			properties.put( property, value );
		}
	}

	@Override
	public void beforeCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu) {
		Notification.beforeEntityManagerFactoryCreate( Classification.INFINISPAN, pu );
	}

	@Override
	public void afterCreateContainerEntityManagerFactory(PersistenceUnitMetadata pu) {
		Notification.afterEntityManagerFactoryCreate( Classification.INFINISPAN, pu );
	}

	@Override
	public ManagementAdaptor getManagementAdaptor() {
		return HibernateManagementAdaptor.getInstance();
	}

	/**
	 * determine if management console can display the second level cache entries
	 *
	 * @return false if a custom AvailableSettings.CACHE_REGION_PREFIX property is specified.
	 * true if the scoped persistence unit name is used to prefix cache entries.
	 */
	@Override
	public boolean doesScopedPersistenceUnitNameIdentifyCacheRegionName(PersistenceUnitMetadata pu) {
		String cacheRegionPrefix = pu.getProperties().getProperty( AvailableSettings.CACHE_REGION_PREFIX );

		return cacheRegionPrefix == null || cacheRegionPrefix.equals( pu.getScopedPersistenceUnitName() );
	}

	@Override
	public void cleanup(PersistenceUnitMetadata pu) {

	}

	@Override
	public Object beanManagerLifeCycle(BeanManager beanManager) {

		if ( isHibernateExtendedBeanManagerSupported() ) {
			return new HibernateExtendedBeanManager( beanManager );
		}
		// for ORM 5.0, return null to indicate that the org.hibernate.jpa.event.spi.jpa.ExtendedBeanManager extension should not be used.
		return null;
	}

	@Override
	public void markPersistenceUnitAvailable(Object wrapperBeanManagerLifeCycle) {
		if ( isHibernateExtendedBeanManagerSupported() ) {
			HibernateExtendedBeanManager hibernateExtendedBeanManager = (HibernateExtendedBeanManager) wrapperBeanManagerLifeCycle;
			// notify Hibernate ORM ExtendedBeanManager extension that the entity listener(s) can now be registered.
			hibernateExtendedBeanManager.beanManagerIsAvailableForUse();
		}
	}

	/**
	 * org.hibernate.jpa.event.spi.jpa.ExtendedBeanManager is added to Hibernate 5.1 as an extension for delaying registration
	 * of entity listeners until the CDI AfterDeploymentValidation event is triggered.
	 * This allows entity listener classes to reference the (origin) persistence unit (WFLY-2387).
	 * <p>
	 * return true for Hibernate ORM 5.1+, which should contain the ExtendedBeanManager contract
	 */
	private boolean isHibernateExtendedBeanManagerSupported() {
		try {
			Class.forName( HIBERNATE_EXTENDED_BEANMANAGER );
			return true;
		}
		catch (ClassNotFoundException | NoClassDefFoundError ignore) {
			return false;
		}

	}

	/* start of TwoPhaseBootstrapCapable methods */

	public EntityManagerFactoryBuilder getBootstrap(final PersistenceUnitInfo info, final Map map) {
		return new TwoPhaseBootstrapImpl( info, map );
	}

	/* end of TwoPhaseBootstrapCapable methods */
}

