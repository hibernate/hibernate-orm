/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement;

import org.hibernate.SessionFactory;
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

	public final void prepare(Configuration user) {
		Configuration cfg = new Configuration();
		cfg.setProperties( user.getProperties() );
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );

		Class<?>[] resources = getAnnotatedClasses();
		for ( Class<?> resource : resources ) {
			cfg.addAnnotatedClass( resource );
		}

		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( cfg.getProperties() );
		factory = cfg.buildSessionFactory( serviceRegistry );
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
