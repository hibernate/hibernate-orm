/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.jakarta.data.tck.runner;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceUnitTransactionType;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.junit.jupiter.WeldInitiator;
import org.jboss.weld.junit.jupiter.WeldJunit5Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * JUnit Jupiter extension that bridges ORM's SessionFactory creation with Weld CDI.
 */
public class DataTckExtension extends WeldJunit5Extension {

	private static final ExtensionContext.Namespace NAMESPACE =
			ExtensionContext.Namespace.create( DataTckExtension.class );

	@Override
	protected void weldInit(ExtensionContext context, Weld weld, WeldInitiator.Builder builder) {
		final Object testInstance = context.getRequiredTestInstance();
		final DataTck annotation = findDataTckAnnotation( testInstance.getClass() );

		final EntityManagerFactory emf = buildEntityManagerFactory( context, annotation );

		registerRequiredBeanClasses( weld, testInstance, annotation );

		final TestMethodCloseableResources closeables = new TestMethodCloseableResources( context );

		final Function<InjectionPoint, Object> persistenceContextFactory;
		if ( annotation.sharedEntityManager() ) {
			final EntityManager em = closeables.add( emf.createEntityManager() );
			persistenceContextFactory = ip -> em;
		}
		else {
			persistenceContextFactory = ip -> closeables.add( emf.createEntityManager() );
		}

		builder.activate( RequestScoped.class )
				.inject( testInstance )
				.setPersistenceUnitFactory( ip -> emf )
				.setPersistenceContextFactory( persistenceContextFactory )
				.setPersistenceAgentFactory( ip -> closeables.add( emf.createEntityAgent() ) );
	}

	private static void registerRequiredBeanClasses(Weld weld, Object testInstance, DataTck annotation) {
		Class<?> tckBaseClass = testInstance.getClass().getSuperclass();
		weld.addBeanClass( tckBaseClass );
		for ( Class<?> repoClass : annotation.repositoryClasses() ) {
			weld.addBeanClass( repoClass );
		}
	}

	private static EntityManagerFactory buildEntityManagerFactory(ExtensionContext context, DataTck annotation) {
		EntityManagerFactory emf = new EntityManagerFactoryProducer(
				annotation.domainClasses(),
				annotation.repositoryClasses()
		).createEntityManagerFactory();

		context.getStore( NAMESPACE ).put( EntityManagerFactory.class, emf );
		return emf;
	}

	@Override
	public void afterEach(ExtensionContext context) {
		super.afterEach( context );
		TestMethodCloseableResources closeables = context.getStore( NAMESPACE )
				.remove( TestMethodCloseableResources.class, TestMethodCloseableResources.class );
		if ( closeables != null ) {
			closeables.closeAll();
		}
		EntityManagerFactory emf = context.getStore( NAMESPACE )
				.get( EntityManagerFactory.class, EntityManagerFactory.class );
		if ( emf != null && emf.isOpen() ) {
			emf.getSchemaManager().truncate();
		}
	}

	@Override
	public void afterAll(ExtensionContext context) {
		super.afterAll( context );
		EntityManagerFactory emf = context.getStore( NAMESPACE )
				.remove( EntityManagerFactory.class, EntityManagerFactory.class );
		if ( emf != null && emf.isOpen() ) {
			emf.close();
		}
	}

	private static DataTck findDataTckAnnotation(Class<?> clazz) {
		DataTck annotation = clazz.getAnnotation( DataTck.class );
		if ( annotation != null ) {
			return annotation;
		}
		throw new IllegalStateException(
				"Test class " + clazz.getName() + " is not annotated with @DataTck" );
	}

	private record EntityManagerFactoryProducer(Class<?>[] domainClasses, Class<?>[] repositoryClasses) {

		public EntityManagerFactory createEntityManagerFactory() {
			// Must use JPA bootstrap (not native Configuration) because
			// StatelessSessionImpl.convertException() checks isJpaBootstrap()
			// to convert ConstraintViolationException to EntityExistsException
			PersistenceConfiguration configuration = new PersistenceConfiguration( "jakarta-data-tck" )
					.transactionType( PersistenceUnitTransactionType.JTA );
			for ( Class<?> cls : domainClasses ) {
				configuration.managedClass( cls );
			}
			for ( Class<?> repoImpl : repositoryClasses ) {
				for ( Class<?> iface : repoImpl.getInterfaces() ) {
					configuration.managedClass( iface );
				}
			}
			return configuration.createEntityManagerFactory();
		}
	}

	private static class TestMethodCloseableResources {

		private final List<AutoCloseable> resources = new ArrayList<>();

		public TestMethodCloseableResources(ExtensionContext context) {
			context.getStore( NAMESPACE ).put( TestMethodCloseableResources.class, this );
		}

		<T extends AutoCloseable> T add(T resource) {
			resources.add( resource );
			return resource;
		}

		public void closeAll() {
			for ( AutoCloseable resource : resources ) {
				try {
					resource.close();
				}
				catch (Exception ignored) {
				}
			}
			resources.clear();
		}
	}
}
