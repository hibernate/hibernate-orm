/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.callbacks;

import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-17019")
@DomainModel(
		annotatedClasses = {
				PostLoadLazyListenerTest.Person.class, PostLoadLazyListenerTest.Tag.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class PostLoadLazyListenerTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
						   session.createQuery( "delete from Tag" ).executeUpdate();
						   session.createQuery( "delete from Person" ).executeUpdate();
					   }
		);
	}

	@Test
	public void smoke(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person person = new Person( 1, "name" );
					Tag tag = new Tag( 100, person );
					person.tags.add( tag );

					session.persist( person );
					session.persist( tag );
				}
		);

		scope.inTransaction(
				session -> {
					Tag tag = session.find( Tag.class, 100 );
					assertThat( tag )
							.isNotNull();
					assertThat( TagListener.WAS_CALLED ).isTrue();
					assertThat( PersonListener.WAS_CALLED ).isFalse();

					assertThat( tag.person.name ).isEqualTo( "name" );
					assertThat( PersonListener.WAS_CALLED ).isTrue();
				}
		);
	}

	@Entity(name = "Person")
	@EntityListeners(PersonListener.class)
	public static class Person {
		@Id
		private int id;

		private String name;

		@OneToMany(mappedBy = "person")
		private Collection<Tag> tags = new ArrayList<>();

		public Person() {
		}

		public Person(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Tag")
	@EntityListeners(TagListener.class)
	public static class Tag {

		@Id
		private int id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Person person;

		public Tag() {
		}

		public Tag(int id, Person person) {
			this.id = id;
			this.person = person;
		}
	}

	public static class PersonListener {
		static boolean WAS_CALLED = false;

		@PostLoad
		void onPreUpdate(Object o) {
			WAS_CALLED = true;
		}
	}

	public static class TagListener {
		static boolean WAS_CALLED = false;

		@PostLoad
		void onPreUpdate(Object o) {
			WAS_CALLED = true;
		}
	}
}
