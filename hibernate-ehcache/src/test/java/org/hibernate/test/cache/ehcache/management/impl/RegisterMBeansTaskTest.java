/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.cache.ehcache.management.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.TimerTask;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.ConfigurationFactory;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cache.ehcache.management.impl.EhcacheHibernate;
import org.hibernate.cache.ehcache.management.impl.EhcacheHibernateMBeanRegistrationImpl;
import org.hibernate.cache.ehcache.management.impl.ProviderMBeanRegistrationHelper;
import org.hibernate.cfg.Configuration;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Mateus Pimenta
 */
public class RegisterMBeansTaskTest {
	
	private Field field;

	@Test
	public void testJMXRegistration() throws Exception {
		SessionFactory sessionFactory = null;
		System.setProperty( "derby.system.home", "target/derby" );
		Configuration config = new Configuration().configure( "/hibernate-config/hibernate.cfg.xml" );
		config.setProperty( "hibernate.hbm2ddl.auto", "create" );
		try {
			sessionFactory = config.buildSessionFactory();
		}
		catch ( HibernateException ex ) {
			System.err.println( "Initial SessionFactory creation failed." + ex );
			throw new ExceptionInInitializerError( ex );
		}		
		
		try {
			sessionFactory.getStatistics().setStatisticsEnabled( true );

			EhcacheHibernateMBeanRegistrationImpl ehcacheHibernateMBeanRegistration = new EhcacheHibernateMBeanRegistrationImpl();

	        TimerTask instance = configureTimeTask(ehcacheHibernateMBeanRegistration, config.getProperties());

	        // Run the task
	        instance.run();
	        
	        // Checks if the ehcache hibernate was successfully started
	        field = ehcacheHibernateMBeanRegistration.getClass().getDeclaredField("ehcacheHibernate");
	        field.setAccessible(true);
	        EhcacheHibernate ehcacheHibernate = (EhcacheHibernate) field.get(ehcacheHibernateMBeanRegistration);
	        Assert.assertTrue(ehcacheHibernate.isHibernateStatisticsSupported());
		} finally {
			sessionFactory.close();
		}
	}

	private TimerTask configureTimeTask(EhcacheHibernateMBeanRegistrationImpl ehcacheHibernateMBeanRegistration, Properties properties) throws Exception {
		net.sf.ehcache.config.Configuration configuration = ConfigurationFactory.parseConfiguration();
		CacheManager manager = new CacheManager( configuration );
		
		
		TimerTask instance = null;
		Class<?>[] classes = ProviderMBeanRegistrationHelper.class.getDeclaredClasses();
		for (Class<?> clazz : classes) {
			if ("org.hibernate.cache.ehcache.management.impl.ProviderMBeanRegistrationHelper.RegisterMBeansTask".equals(clazz.getCanonicalName())) {
		        Constructor<?> constructor = clazz.getConstructor(EhcacheHibernateMBeanRegistrationImpl.class, CacheManager.class, Properties.class);
				constructor.setAccessible(true);
				instance = (TimerTask) constructor.newInstance(ehcacheHibernateMBeanRegistration, manager, properties);
			}
			
		}
		return instance;
	}
	
}
