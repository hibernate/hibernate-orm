package org.hibernate.orm.test.mapping.identifier;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.InheritanceType.TABLE_PER_CLASS;
import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		TablePerClassVersionedMutationTest.Animal.class,
		TablePerClassVersionedMutationTest.Cat.class
})
@SessionFactory(useCollectingStatementObserver = true)
@JiraKey("HHH-21271")
public class TablePerClassVersionedMutationTest {
	@Test
	void updateAndDeleteRootInstance(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Animal( 1 ) ) );
		final var statements = scope.getCollectingStatementObserver();
		statements.clear();
		scope.inTransaction( session -> session.find( Animal.class, 1 ).name = "updated" );
		assertThat( statements.getSqlQueries() ).anyMatch( sql ->
				sql.matches( "(?is)update versioned_animal\\b.*\\bwhere\\b.*\\bversion\\s*=\\s*\\?.*" ) );
		scope.inTransaction( session -> {
			final var animal = session.find( Animal.class, 1 );
			assertThat( animal.name ).isEqualTo( "updated" );
			session.remove( animal );
		} );
		scope.inTransaction( session -> assertThat( session.find( Animal.class, 1 ) ).isNull() );
	}

	@Entity(name = "Animal")
	@Table(name = "versioned_animal")
	@Inheritance(strategy = TABLE_PER_CLASS)
	public static class Animal {
		@Id
		Integer id;
		@Version
		Integer version;
		String name;

		Animal() {
		}

		Animal(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Cat")
	@Table(name = "versioned_cat")
	public static class Cat extends Animal {
	}
}
