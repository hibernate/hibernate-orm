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
