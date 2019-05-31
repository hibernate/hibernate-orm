/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Tag;

import org.jboss.logging.Logger;

import org.hibernate.testing.junit5.StandardTags;
import org.hibernate.testing.junit5.dynamictests.DynamicAfterAll;
import org.hibernate.testing.junit5.envers.EnversSessionFactoryProducer;
import org.hibernate.testing.junit5.envers.EnversSessionFactoryScope;

/**
 * Envers base test class that uses a native Hibernate {@link SessionFactory} configuration and only
 * configures a single session and audit reader instance used throughout the entire test.
 *
 * @author Chris Cranford
 */
@Tag(StandardTags.ENVERS)
public class EnversSingleSessionBasedFunctionalTest
		extends AbstractEnversDynamicTest
		implements EnversSessionFactoryProducer {

	private static final Logger log = Logger.getLogger( EnversSingleSessionBasedFunctionalTest.class );

	private EnversSessionFactoryScope sessionFactoryScope;
	private SessionImplementor session;
	private AuditReader auditReader;

	@Override
	public SessionFactory produceSessionFactory(String auditStrategyName) {
		log.debugf( "Producing SessionFactory (%s)", auditStrategyName );
		this.auditStrategyName = auditStrategyName;

		final StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();
		ssrb.applySetting( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, Boolean.TRUE.toString() );
		ssrb.applySetting( AvailableSettings.HBM2DDL_AUTO, exportSchema() ? "create-drop" : "none" );

		final Map<String, Object> settings = new HashMap<>();
		addSettings( settings );
		ssrb.applySettings( settings );

		final StandardServiceRegistry ssr = ssrb.build();
		try {
			MetadataSources metadataSources = new MetadataSources( ssr );
			applyMetadataSources( metadataSources );

			return metadataSources.buildMetadata().buildSessionFactory();
		}
		catch ( Throwable t ) {
			t.printStackTrace();
			StandardServiceRegistryBuilder.destroy( ssr );
			throw t;
		}
	}

	@DynamicAfterAll
	public void releaseSessionFactory() {
		if ( auditReader != null ) {
			auditReader.close();
			auditReader = null;
		}

		log.debugf( "Releasing SessionFactory (%s)", auditStrategyName );
		sessionFactoryScope.releaseSessionFactory();
	}

	@Override
	protected void injectExecutionContext(EnversDynamicExecutionContext context) {
		sessionFactoryScope = new EnversSessionFactoryScope( this, context.getStrategy() );
	}

	@Override
	protected Dialect getDialect() {
		return sessionFactoryScope.getDialect();
	}

	protected void inSession(Consumer<SessionImplementor> action) {
		if ( session == null ) {
			session = initializeSession();
		}

		action.accept( session );
	}

	protected <T> T inSession(Function<SessionImplementor, T> action) {
		if ( session == null ) {
			session = initializeSession();
		}

		return action.apply( session );
	}

	@SafeVarargs
	protected final void inTransactions(Consumer<SessionImplementor>... actions) {
		if ( session == null ) {
			session = initializeSession();
		}

		for ( Consumer<SessionImplementor> action : actions ) {
			sessionFactoryScope.inTransaction( session, action );
		}
	}

	protected AuditReader getAuditReader() {
		if ( auditReader == null ) {
			auditReader = AuditReaderFactory.get( session );
		}

		return auditReader;
	}

	protected void forceNewSession() {
		// Cleanup existing resources if allocated
		releaseSessionFactory();
		this.session = null;

		// Create new session
		// An audit reader will be allocated when first requested
		session = initializeSession();
	}

	private void applyMetadataSources(MetadataSources metadataSources) {
		for ( String mappingFile : getMappings() ) {
			metadataSources.addResource( getBaseForMappings() + mappingFile );
		}
		for ( Class<?> annotatedClass : getAnnotatedClasses() ) {
			metadataSources.addAnnotatedClass( annotatedClass );
		}
	}

	private SessionImplementor initializeSession() {
		return (SessionImplementor) sessionFactoryScope.getSessionFactory().openSession();
	}
}
