/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.callbacks;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PreUpdate;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@TestForIssue(jiraKey = "HHH-13466")
public class PreUpdateNewUnidirectionalBagTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Person.class, Tag.class };
	}

	@Test
	public void testPreUpdateModifications() {
		Person person = new Person();
		person.id = 1;

		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.persist( person );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Person p = entityManager.find( Person.class, person.id );
			assertNotNull( p );
			final Tag tag = new Tag();
			tag.id = 2;
			tag.description = "description";
			final Set<Tag> tags = new HashSet<Tag>();
			tags.add( tag );
			p.tags = tags;
			entityManager.merge( p );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Person p = entityManager.find( Person.class, person.id );
			assertEquals( 1, p.tags.size() );
			assertEquals( "description", p.tags.iterator().next().description );
			assertNotNull( p.getLastUpdatedAt() );
		} );
	}

	@Entity(name = "Person")
	@EntityListeners( PersonListener.class )
	private static class Person {
		@Id
		private int id;

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		private Instant lastUpdatedAt;

		public Instant getLastUpdatedAt() {
			return lastUpdatedAt;
		}

		public void setLastUpdatedAt(Instant lastUpdatedAt) {
			this.lastUpdatedAt = lastUpdatedAt;
		}

		@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		private Collection<Tag> tags = new ArrayList<Tag>();
	}

	@Entity(name = "Tag")
	public static class Tag {

		@Id
		private int id;

		private String description;
	}

	public static class PersonListener {
		@PreUpdate
		void onPreUpdate(Object o) {
			if ( Person.class.isInstance( o ) ) {
				( (Person) o ).setLastUpdatedAt( Instant.now() );
			}
		}
	}
}
