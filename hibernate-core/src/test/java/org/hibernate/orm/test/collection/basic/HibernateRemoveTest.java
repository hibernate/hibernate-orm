package org.hibernate.orm.test.collection.basic;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SessionFactory
@DomainModel(annotatedClasses = HibernateRemoveTest.Author.class)
public class HibernateRemoveTest {

	@Test void test(SessionFactoryScope scope) {
		Author gavin = new Author("Gavin", "1ovthafew", "gavinking");
		scope.inTransaction( session -> {
			session.persist(gavin);
		});
		scope.inTransaction( session -> {
			Author author = session.get(Author.class, gavin.id);
			assertEquals( 2, Hibernate.size( author.usernames ) );
			assertFalse( Hibernate.isInitialized( author.usernames) );
		});
		scope.inTransaction( session -> {
			Author author = session.get(Author.class, gavin.id);
			Hibernate.remove( author.usernames, "1ovthafew" );
			assertFalse( Hibernate.isInitialized( author.usernames) );
		});
		scope.inTransaction( session -> {
			Author author = session.get(Author.class, gavin.id);
			assertEquals( 1, Hibernate.size( author.usernames ) );
			assertFalse( Hibernate.isInitialized( author.usernames) );
		});
	}

	@Entity
	public static class Author {

		@Id
		@GeneratedValue
		Long id;

		String name;

		@ElementCollection
		Set<String> usernames = new HashSet<>();

		Author() {}
		Author(String name, String... names) {
			this.name = name;
			Collections.addAll(usernames, names);
		}
	}

}
