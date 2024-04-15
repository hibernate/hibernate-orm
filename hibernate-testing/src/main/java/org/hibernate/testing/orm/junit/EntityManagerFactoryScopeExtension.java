/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.Optional;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import org.jboss.logging.Logger;

import javax.persistence.EntityManagerFactory;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.create;

/**
 * The thing that actually manages lifecycle of the EntityManagerFactory related to a test class.
 * Work in conjunction with EntityManagerFactoryScope and EntityManagerFactoryScopeContainer.
 *
 * @see EntityManagerFactoryScope
 * @see EntityManagerFactoryScopeContainer
 * @see EntityManagerFactoryProducer
 *
 * @author Chris Cranford
 */
public class EntityManagerFactoryScopeExtension
		implements TestInstancePostProcessor, AfterAllCallback, TestExecutionExceptionHandler {

	private static final Logger log = Logger.getLogger( EntityManagerFactoryScopeExtension.class );

	public static ExtensionContext.Namespace namespace(Object testInstance) {
		return create( EntityManagerFactoryScopeExtension.class.getName(), testInstance );
	}

	public static Optional<EntityManagerFactoryScope> findEntityManagerFactoryScope(ExtensionContext context) {
		final Optional entityManagerFactoryScope = Optional.ofNullable(
				context.getStore( namespace( context.getRequiredTestInstance() ) )
				.get( ENTITYMANAGER_FACTORY_KEY )
		);
		return entityManagerFactoryScope;
	}

	public static final Object ENTITYMANAGER_FACTORY_KEY = "ENTITYMANAGER_FACTORY";

	public EntityManagerFactoryScopeExtension() {
		log.trace( "EntityManagerFactoryScopeExtension#<init>" );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// TestInstancePostProcessor

	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
		log.trace( "EntityManagerFactoryScopeExtension#postProcessTestInstance" );
		if ( EntityManagerFactoryScopeContainer.class.isInstance( testInstance ) ) {
			final EntityManagerFactoryScopeContainer scopeContainer = EntityManagerFactoryScopeContainer.class.cast(
					testInstance );
			final EntityManagerFactoryScope scope = new EntityManagerFactoryScopeImpl(
					scopeContainer.getEntityManagerFactoryProducer()
			);
			context.getStore( namespace( testInstance ) ).put( ENTITYMANAGER_FACTORY_KEY, scope );

			scopeContainer.injectEntityManagerFactoryScope( scope );
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// AfterAllCallback

	@Override
	public void afterAll(ExtensionContext context) {
		final EntityManagerFactoryScope scope = (EntityManagerFactoryScope)
				context.getStore( namespace( context.getRequiredTestInstance() ) ).remove( ENTITYMANAGER_FACTORY_KEY );
		if ( scope != null ) {
			scope.releaseEntityManagerFactory();
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// TestExecutionExceptionHandler

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		final Optional<EntityManagerFactoryScope> scopeOptional = findEntityManagerFactoryScope( context );
		if ( ! scopeOptional.isPresent() ) {
			log.debug( "Could not locate EntityManagerFactoryScope on exception" );
		}
		else {
			scopeOptional.get().releaseEntityManagerFactory();
		}

		throw throwable;
	}

	private static class EntityManagerFactoryScopeImpl extends AbstractEntityManagerFactoryScope {

		private final EntityManagerFactoryProducer producer;

		public EntityManagerFactoryScopeImpl(EntityManagerFactoryProducer producer) {
			log.trace( "EntityManagerFactoryScope#<init>" );
			this.producer = producer;
		}

		@Override
		protected EntityManagerFactory createEntityManagerFactory() {
			return producer.produceEntityManagerFactory();
		}
	}

}
