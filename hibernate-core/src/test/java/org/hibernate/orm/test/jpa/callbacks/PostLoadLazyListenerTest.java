/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.callbacks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;

@JiraKey("HHH-17019")
@RunWith(BytecodeEnhancerRunner.class)
public class PostLoadLazyListenerTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class, Tag.class };
	}

	@After
	public void tearDown() {
		inTransaction( session -> {
					session.createQuery( "delete from Tag" ).executeUpdate();
					session.createQuery( "delete from Person" ).executeUpdate();
				}
		);
	}

	@Test
	public void smoke() {
		inTransaction( session -> {
					Person person = new Person( 1, "name" );
					Tag tag = new Tag( 100, person );
					person.tags.add( tag );

					session.persist( person );
					session.persist( tag );
				}
		);

		inTransaction( session -> {
					Tag tag = session.find( Tag.class, 100 );
					assertThat( tag )
							.isNotNull();
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
}
