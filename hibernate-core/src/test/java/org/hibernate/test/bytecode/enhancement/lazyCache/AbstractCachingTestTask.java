/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazyCache;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.bytecode.enhancement.EnhancerTestTask;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCachingTestTask implements EnhancerTestTask {
	private SessionFactoryImplementor sessionFactory;

	protected SessionFactoryImplementor sessionFactory() {
		return sessionFactory;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Document.class };
	}

	@Override
	public void prepare() {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
		registryBuilder.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
		registryBuilder.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		registryBuilder.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		registryBuilder.addService( ClassLoaderService.class, new ClassLoaderServiceImpl( Thread.currentThread().getContextClassLoader() ) );
		StandardServiceRegistry registry = registryBuilder.build();

		MetadataSources metadataSources = new MetadataSources( registry );
		metadataSources.addAnnotatedClass( Document.class );

		sessionFactory = (SessionFactoryImplementor) metadataSources.buildMetadata().buildSessionFactory();
	}

	@Override
	public void complete() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}
}
