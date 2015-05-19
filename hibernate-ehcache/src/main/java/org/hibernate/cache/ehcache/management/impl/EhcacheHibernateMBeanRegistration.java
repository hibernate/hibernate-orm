/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.management.impl;

import java.util.Properties;

import net.sf.ehcache.CacheManager;

import org.hibernate.SessionFactory;

/**
 * Interface for helping registering mbeans for ehcache backed hibernate second-level cache
 * <p/>
 * <p/>
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 */
public interface EhcacheHibernateMBeanRegistration {

	/**
	 * Registers MBean for the input manager and session factory properties.
	 * <p/>
	 * MBeans will be registered based on the input session factory name. If the input name is null or blank, the name of the cache-manager
	 * is used
	 *
	 * @param manager the {@link CacheManager} to register the MBean for
	 * @param properties properties to used to create the associated {@link SessionFactory}
	 *
	 * @throws Exception reflecting the source of the problem registering the MBean
	 */
	public void registerMBeanForCacheManager(CacheManager manager, Properties properties) throws Exception;

	/**
	 * Enable hibernate statistics in the mbean.
	 *
	 * @param sessionFactory the {@link SessionFactory} to enable stats for
	 */
	public void enableHibernateStatisticsSupport(SessionFactory sessionFactory);

}
