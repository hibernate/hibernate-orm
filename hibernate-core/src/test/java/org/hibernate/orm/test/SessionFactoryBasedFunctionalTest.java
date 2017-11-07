/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.junit5.FunctionalSessionFactoryTesting;
import org.hibernate.testing.junit5.SessionFactoryProducer;
import org.hibernate.testing.junit5.SessionFactoryScope;
import org.hibernate.testing.junit5.SessionFactoryScopeContainer;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@FunctionalSessionFactoryTesting
public abstract class SessionFactoryBasedFunctionalTest
		extends BaseUnitTest
		implements SessionFactoryProducer, SessionFactoryScopeContainer {
	private static final Logger log = Logger.getLogger( SessionFactoryBasedFunctionalTest.class );

	private SessionFactoryScope sessionFactoryScope;

	protected SessionFactoryScope sessionFactoryScope() {
		return sessionFactoryScope;
	}

	protected SessionFactoryImplementor sessionFactory() {
		return sessionFactoryScope.getSessionFactory();
	}

	@Override
	public SessionFactoryImplementor produceSessionFactory() {
		log.trace( "Producing SessionFactory" );

		final StandardServiceRegistryBuilder ssrBuilder = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, exportSchema() ? "create-drop" : "none" );
		applySettings( ssrBuilder );
		final StandardServiceRegistry ssr = ssrBuilder.build();

		try {
			MetadataSources metadataSources = new MetadataSources( ssr );
			applyMetadataSources( metadataSources );

			final SessionFactoryImplementor factory = (SessionFactoryImplementor) metadataSources.buildMetadata().buildSessionFactory();
			sessionFactoryBuilt( factory );
			return factory;
		}
		catch (Exception e) {
			StandardServiceRegistryBuilder.destroy( ssr );
			throw e;
		}
	}

	protected void sessionFactoryBuilt(SessionFactoryImplementor factory) {
	}

	protected void applySettings(StandardServiceRegistryBuilder builer) {
	}

	protected boolean strictJpaCompliance() {
		return false;
	}

	protected boolean exportSchema() {
		return false;
	}

	protected void applyMetadataSources(MetadataSources metadataSources) {
	}

	@Override
	public void injectSessionFactoryScope(SessionFactoryScope scope) {
		sessionFactoryScope = scope;
	}

	@Override
	public SessionFactoryProducer getSessionFactoryProducer() {
		return this;
	}
}
