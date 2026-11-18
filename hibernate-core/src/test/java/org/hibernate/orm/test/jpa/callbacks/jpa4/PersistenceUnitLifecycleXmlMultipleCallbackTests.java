package org.hibernate.orm.test.jpa.callbacks.jpa4;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;

import org.hibernate.boot.MetadataSources;
import org.hibernate.models.ModelsException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DomainModel(
		annotatedClasses = PersistenceUnitLifecycleXmlMultipleCallbackTests.BasicEntity.class,
		xmlMappings = "org/hibernate/orm/test/jpa/callbacks/jpa4/persistence-unit-multiple-lifecycle-callbacks.orm.xml"
)
@SessionFactory
public class PersistenceUnitLifecycleXmlMultipleCallbackTests {
	@Test
	void persistenceUnitListenerCanDeclareMultiplePostCreateAndPreCloseElements(SessionFactoryScope scope) {
		Events.reset();

		final var factory = scope.getSessionFactory();

		assertThat( Events.names ).containsExactly(
				"post-create:factory:true",
				"overloaded-post-create:factory:true"
		);

		Events.reset();
		final EntityManager entityManager = factory.createEntityManager();

		assertThat( Events.names ).containsExactly(
				"post-create:manager:true",
				"overloaded-post-create:manager:true"
		);

		Events.reset();
		entityManager.close();

		assertThat( Events.names ).containsExactly(
				"pre-close:manager:true",
				"overloaded-pre-close:manager:true"
		);

		Events.reset();
		scope.releaseSessionFactory();

		assertThat( Events.names ).containsExactly(
				"pre-close:factory:true",
				"overloaded-pre-close:factory:true"
		);
	}

	@Test
	void persistenceUnitListenerRejectsMultipleCallbacksForSameEventAndParameterType(ServiceRegistryScope registryScope) {
		assertThatThrownBy( () -> new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClass( BasicEntity.class )
				.addResource( "org/hibernate/orm/test/jpa/callbacks/jpa4/persistence-unit-duplicate-lifecycle-callbacks.orm.xml" )
				.buildMetadata() )
				.isInstanceOf( ModelsException.class )
				.hasMessageContaining( DuplicateListener.class.getSimpleName() );
	}

	public static class Events {
		static final List<String> names = new ArrayList<>();

		static void reset() {
			names.clear();
		}
	}

	@Entity(name = "PersistenceUnitLifecycleXmlMultipleBasicEntity")
	public static class BasicEntity {
		@Id
		private Integer id;
	}

	public static class MultiElementListener {
		public void factoryCreated(EntityManagerFactory entityManagerFactory) {
			Events.names.add( "post-create:factory:" + entityManagerFactory.isOpen() );
		}

		public void managerCreated(EntityManager entityManager) {
			Events.names.add( "post-create:manager:" + entityManager.isOpen() );
		}

		public void factoryClosing(EntityManagerFactory entityManagerFactory) {
			Events.names.add( "pre-close:factory:" + entityManagerFactory.isOpen() );
		}

		public void managerClosing(EntityManager entityManager) {
			Events.names.add( "pre-close:manager:" + entityManager.isOpen() );
		}
	}

	public static class OverloadedListener {
		public void created(EntityManagerFactory entityManagerFactory) {
			Events.names.add( "overloaded-post-create:factory:" + entityManagerFactory.isOpen() );
		}

		public void created(EntityManager entityManager) {
			Events.names.add( "overloaded-post-create:manager:" + entityManager.isOpen() );
		}

		public void closing(EntityManagerFactory entityManagerFactory) {
			Events.names.add( "overloaded-pre-close:factory:" + entityManagerFactory.isOpen() );
		}

		public void closing(EntityManager entityManager) {
			Events.names.add( "overloaded-pre-close:manager:" + entityManager.isOpen() );
		}
	}

	public static class DuplicateListener {
		public void first(EntityManagerFactory entityManagerFactory) {
			Events.names.add( "first" );
		}

		public void second(EntityManagerFactory entityManagerFactory) {
			Events.names.add( "second" );
		}
	}
}
