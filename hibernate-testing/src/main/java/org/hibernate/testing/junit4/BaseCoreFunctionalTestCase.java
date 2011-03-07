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
package org.hibernate.testing.junit4;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.internal.ServiceRegistryImpl;
import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.OnExpectedFailure;
import org.hibernate.testing.OnFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Applies functional testing logic for core Hibernate testing on top of {@link BaseUnitTestCase}
 *
 * @author Steve Ebersole
 */
public class BaseCoreFunctionalTestCase extends BaseUnitTestCase {
	private static final Logger log = LoggerFactory.getLogger( BaseCoreFunctionalTestCase.class );

	private static Configuration configuration;
	private static ServiceRegistryImpl serviceRegistry;
	private static SessionFactoryImplementor sessionFactory;

	public static Configuration getConfiguration() {
		return configuration;
	}

	public static SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@BeforeClassOnce
	private void buildSessionFactory() {
		log.trace( "Building session factory" );
		configuration = buildConfiguration();
		serviceRegistry = buildServiceRegistry( configuration );
		sessionFactory = (SessionFactoryImplementor) configuration.buildSessionFactory( serviceRegistry );
		afterSessionFactoryBuilt();
	}

	protected Configuration buildConfiguration() {
		Configuration cfg = constructConfiguration();
		configure( cfg );
		addMappings( cfg );
		cfg.buildMappings();
		afterConfigurationBuilt( cfg );
		return cfg;
	}

	protected Configuration constructConfiguration() {
		return new Configuration();
	}

	protected void configure(Configuration cfg) {
	}

	protected void addMappings(Configuration cfg) {
	}

	protected void afterConfigurationBuilt(Configuration configuration) {
	}

	protected ServiceRegistryImpl buildServiceRegistry(Configuration configuration) {
		Properties properties = new Properties();
		properties.putAll( configuration.getProperties() );
		Environment.verifyProperties( properties );
		ConfigurationHelper.resolvePlaceHolders( properties );
		ServiceRegistryImpl serviceRegistry = new ServiceRegistryImpl( properties );
		applyServices( serviceRegistry );
		return serviceRegistry;
	}

	protected void applyServices(ServiceRegistryImpl serviceRegistry) {
	}

	protected void afterSessionFactoryBuilt() {
	}

	protected boolean rebuildSessionFactoryOnError() {
		return true;
	}

	@AfterClassOnce
	private void releaseSessionFactory() {
		if ( sessionFactory == null ) {
			return;
		}
		log.trace( "Releasing session factory" );
		sessionFactory.close();
		sessionFactory = null;
		configuration = null;
	}

	@OnFailure
	@OnExpectedFailure
	public void onFailure() {
		log.trace( "Processing failure-expected ignore" );
		if ( ! rebuildSessionFactoryOnError() ) {
			return;
		}

		rebuildSessionFactory();
	}

	protected void rebuildSessionFactory() {
		if ( sessionFactory == null ) {
			return;
		}
		sessionFactory.close();
		serviceRegistry.destroy();

		serviceRegistry = buildServiceRegistry( configuration );
		sessionFactory = (SessionFactoryImplementor) configuration.buildSessionFactory( serviceRegistry );
		afterSessionFactoryBuilt();
	}
}
