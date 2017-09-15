/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache;

import java.net.MalformedURLException;
import java.net.URL;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.ehcache.internal.CacheTransactionContextImpl;
import org.hibernate.cache.ehcache.internal.nonstop.NonstopAccessStrategyFactory;
import org.hibernate.cache.ehcache.internal.regions.DomainDataRegionImpl;
import org.hibernate.cache.ehcache.internal.strategy.EhcacheAccessStrategyFactory;
import org.hibernate.cache.ehcache.internal.strategy.EhcacheAccessStrategyFactoryImpl;
import org.hibernate.cache.ehcache.management.impl.ProviderMBeanRegistrationHelper;
import org.hibernate.cache.spi.CacheTransactionContext;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.service.spi.InjectService;
import org.hibernate.sql.NotYetImplementedException;

import org.jboss.logging.Logger;

/**
 * Abstract implementation of an Ehcache specific RegionFactory.
 *
 * @author Chris Dennis
 * @author Greg Luck
 * @author Emmanuel Bernard
 * @author Abhishek Sanoujam
 * @author Alex Snaps
 */
abstract class AbstractEhcacheRegionFactory implements RegionFactory {

	/**
	 * The Hibernate system property specifying the location of the ehcache configuration file name.
	 * <p/>
	 * If not set, ehcache.xml will be looked for in the root of the classpath.
	 * <p/>
	 * If set to say ehcache-1.xml, ehcache-1.xml will be looked for in the root of the classpath.
	 */
	public static final String NET_SF_EHCACHE_CONFIGURATION_RESOURCE_NAME = "net.sf.ehcache.configurationResourceName";

	private static final EhCacheMessageLogger LOG = Logger.getMessageLogger(
			EhCacheMessageLogger.class,
			AbstractEhcacheRegionFactory.class.getName()
	);

	/**
	 * MBean registration helper class instance for Ehcache Hibernate MBeans.
	 */
	protected final ProviderMBeanRegistrationHelper mbeanRegistrationHelper = new ProviderMBeanRegistrationHelper();

	/**
	 * Ehcache CacheManager that supplied Ehcache instances for this Hibernate RegionFactory.
	 */
	protected volatile CacheManager manager;

	/**
	 * Settings object for the Hibernate persistence unit.
	 */
	protected SessionFactoryOptions settings;

	/**
	 * {@link EhcacheAccessStrategyFactory} for creating various access strategies
	 */
	protected final EhcacheAccessStrategyFactory accessStrategyFactory =
			new NonstopAccessStrategyFactory( new EhcacheAccessStrategyFactoryImpl() );

	/**
	 * {@inheritDoc}
	 * <p/>
	 * In Ehcache we default to minimal puts since this should have minimal to no
	 * affect on unclustered users, and has great benefit for clustered users.
	 *
	 * @return true, optimize for minimal puts
	 */
	@Override
	public boolean isMinimalPutsEnabledByDefault() {

		return true;
	}

	@Override
	public long nextTimestamp() {
		return net.sf.ehcache.util.Timestamper.next();
	}

	@Override
	public CacheTransactionContext createTransactionContext(SharedSessionContractImplementor session) {
		return new CacheTransactionContextImpl( this );
	}

	@Override
	public DomainDataRegion buildDomainDataRegion(
			DomainDataRegionConfig regionConfig,
			DomainDataRegionBuildingContext buildingContext) {
		return new DomainDataRegionImpl( regionConfig, this, buildingContext );
	}

	@Override
	public QueryResultsRegion buildQueryResultsRegion(
			String regionName,
			SessionFactoryImplementor sessionFactory) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public TimestampsRegion buildTimestampsRegion(
			String regionName,
			SessionFactoryImplementor sessionFactory) {
		throw new NotYetImplementedException(  );
	}

	@InjectService
	@SuppressWarnings("UnusedDeclaration")
	public void setClassLoaderService(ClassLoaderService classLoaderService) {
		this.classLoaderService = classLoaderService;
	}

	private ClassLoaderService classLoaderService;

	private Ehcache getCache(String name) throws CacheException {
		try {
			Ehcache cache = manager.getEhcache( name );
			if ( cache == null ) {
				LOG.unableToFindEhCacheConfiguration( name );
				manager.addCache( name );
				cache = manager.getEhcache( name );
				LOG.debug( "started EHCache region: " + name );
			}
			return cache;
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}

	}

	/**
	 * Load a resource from the classpath.
	 */
	protected URL loadResource(String configurationResourceName) {
		URL url = null;
		if ( classLoaderService != null ) {
			url = classLoaderService.locateResource( configurationResourceName );
		}
		if ( url == null ) {
			final ClassLoader standardClassloader = Thread.currentThread().getContextClassLoader();
			if ( standardClassloader != null ) {
				url = standardClassloader.getResource( configurationResourceName );
			}
			if ( url == null ) {
				url = AbstractEhcacheRegionFactory.class.getResource( configurationResourceName );
			}
			if ( url == null ) {
				try {
					url = new URL( configurationResourceName );
				}
				catch ( MalformedURLException e ) {
					// ignore
				}
			}
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Creating EhCacheRegionFactory from a specified resource: %s.  Resolved to URL: %s",
					configurationResourceName,
					url
			);
		}
		if ( url == null ) {

			LOG.unableToLoadConfiguration( configurationResourceName );
		}
		return url;
	}

	/**
	 * Default access-type used when the configured using JPA 2.0 config.  JPA 2.0 allows <code>@Cacheable(true)</code> to be attached to an
	 * entity without any access type or usage qualification.
	 * <p/>
	 * We are conservative here in specifying {@link AccessType#READ_WRITE} so as to follow the mantra of "do no harm".
	 * <p/>
	 * This is a Hibernate 3.5 method.
	 */
	public AccessType getDefaultAccessType() {
		return AccessType.READ_WRITE;
	}
}
