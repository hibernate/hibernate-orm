/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cache.ehcache.internal.util;

import java.net.URL;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType;
import org.jboss.logging.Logger;

import org.hibernate.cache.ehcache.EhCacheMessageLogger;


/**
 * Copy of Ehcache utils into Hibernate code base
 *
 * @author Chris Dennis
 * @author Abhishek Sanoujam
 * @author Alex Snaps
 */
public final class HibernateEhcacheUtils {
	private static final EhCacheMessageLogger LOG = Logger.getMessageLogger(
			EhCacheMessageLogger.class,
			HibernateEhcacheUtils.class.getName()
	);

	private HibernateEhcacheUtils() {
	}

	/**
	 * Create a cache manager configuration from the supplied url, correcting it for Hibernate compatibility.
	 * <p/>
	 * Currently "correcting" for Hibernate compatibility means simply switching any identity based value modes
	 * to serialization.
	 *
	 * @param url The url to load the config from
	 *
	 * @return The Ehcache Configuration object
	 */
	public static Configuration loadAndCorrectConfiguration(URL url) {
		final Configuration config = ConfigurationFactory.parseConfiguration( url );
		
		// EHC-875 / HHH-6576
		if ( config == null ) {
			return null;
		}

		if ( config.getDefaultCacheConfiguration() != null
				&& config.getDefaultCacheConfiguration().isTerracottaClustered() ) {
			setupHibernateTimeoutBehavior(
					config.getDefaultCacheConfiguration()
							.getTerracottaConfiguration()
							.getNonstopConfiguration()
			);
		}

		for ( CacheConfiguration cacheConfig : config.getCacheConfigurations().values() ) {
			if ( cacheConfig.isTerracottaClustered() ) {
				setupHibernateTimeoutBehavior( cacheConfig.getTerracottaConfiguration().getNonstopConfiguration() );
			}
		}
		return config;
	}

	private static void setupHibernateTimeoutBehavior(NonstopConfiguration nonstopConfig) {
		nonstopConfig.getTimeoutBehavior().setType( TimeoutBehaviorType.EXCEPTION.getTypeName() );
	}
}
