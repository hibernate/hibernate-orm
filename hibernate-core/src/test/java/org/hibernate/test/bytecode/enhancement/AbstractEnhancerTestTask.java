/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;

/**
 * @author Luis Barreiro
 */
public abstract class AbstractEnhancerTestTask implements EnhancerTestTask {

	private ServiceRegistry serviceRegistry;
	private SessionFactory factory;

	public final void prepare(Configuration config) {
		config.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );

		Class<?>[] resources = getAnnotatedClasses();
		for ( Class<?> resource : resources ) {
			config.addAnnotatedClass( resource );
		}

		StandardServiceRegistryBuilder serviceBuilder = new StandardServiceRegistryBuilder( );
		serviceBuilder.addService( ClassLoaderService.class, new ClassLoaderServiceImpl( Thread.currentThread().getContextClassLoader() ) );

		serviceBuilder.applySettings( config.getProperties() );
		serviceRegistry = serviceBuilder.build();
		factory = config.buildSessionFactory( serviceRegistry );
	}

	public final void complete() {
		try {
			cleanup();
		}
		finally {
			factory.close();
			factory = null;
			if ( serviceRegistry != null ) {
				ServiceRegistryBuilder.destroy( serviceRegistry );
				serviceRegistry = null;
			}
		}
	}

	protected SessionFactory getFactory() {
		return factory;
	}

	protected abstract void cleanup();

}
