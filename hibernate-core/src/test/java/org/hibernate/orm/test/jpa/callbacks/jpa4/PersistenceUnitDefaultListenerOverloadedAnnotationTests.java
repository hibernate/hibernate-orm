package org.hibernate.orm.test.jpa.callbacks.jpa4;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostPersist;

import org.hibernate.boot.MetadataSources;
import org.hibernate.models.ModelsException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A persistence-unit default listener declared in {@code orm.xml} without callback
 * sub-elements uses its annotations. Jakarta Persistence 4.0 allows several callback
 * methods for the same event as long as their parameter types differ, so both
 * {@code @PostPersist} overloads must be registered.
 */
@DomainModel(
		annotatedClasses = {
				PersistenceUnitDefaultListenerOverloadedAnnotationTests.Book.class,
				PersistenceUnitDefaultListenerOverloadedAnnotationTests.Magazine.class
		},
		xmlMappings = "org/hibernate/orm/test/jpa/callbacks/jpa4/persistence-unit-overloaded-annotated-listener.orm.xml"
)
@SessionFactory
public class PersistenceUnitDefaultListenerOverloadedAnnotationTests {

	@BeforeEach
	void reset() {
		Events.reset();
	}

	@Test
	void defaultListenerRegistersAnnotatedOverloadsForSameEvent(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Book( 1, "book" ) ) );
		assertThat( Events.names ).containsExactly( "post-persist-book:book" );

		Events.reset();
		scope.inTransaction( session -> session.persist( new Magazine( 2, "magazine" ) ) );
		assertThat( Events.names ).containsExactly( "post-persist-magazine:magazine" );
	}

	@Test
	void defaultListenerRejectsAnnotatedCallbacksForSameEventAndParameterType(ServiceRegistryScope registryScope) {
		assertThatThrownBy( () -> new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClass( Book.class )
				.addResource( "org/hibernate/orm/test/jpa/callbacks/jpa4/persistence-unit-duplicate-annotated-listener.orm.xml" )
				.buildMetadata() )
				.isInstanceOf( ModelsException.class )
				.hasMessageContaining( "PostPersist" )
				.hasMessageContaining( DuplicateObserver.class.getSimpleName() );
	}

	public static class Events {
		static final List<String> names = new ArrayList<>();

		static void reset() {
			names.clear();
		}
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		private Integer id;
		private String title;

		public Book() {
		}

		public Book(Integer id, String title) {
			this.id = id;
			this.title = title;
		}
	}

	@Entity(name = "Magazine")
	public static class Magazine {
		@Id
		private Integer id;
		private String title;

		public Magazine() {
		}

		public Magazine(Integer id, String title) {
			this.id = id;
			this.title = title;
		}
	}

	public static class Observer {
		@PostPersist
		void onPersist(Book book) {
			Events.names.add( "post-persist-book:" + book.title );
		}

		@PostPersist
		void onPersist(Magazine magazine) {
			Events.names.add( "post-persist-magazine:" + magazine.title );
		}
	}

	public static class DuplicateObserver {
		@PostPersist
		void first(Book book) {
			Events.names.add( "first" );
		}

		@PostPersist
		void second(Book book) {
			Events.names.add( "second" );
		}
	}
}
