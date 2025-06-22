/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks;

import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionIdJdbcTypeCode;
import org.hibernate.annotations.GenericGenerator;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@JiraKey(value = "HHH-13466")
@Jpa(annotatedClasses = {
		PreUpdateNewUnidirectionalIdBagTest.Person.class,
		PreUpdateNewUnidirectionalIdBagTest.Tag.class
})
public class PreUpdateNewUnidirectionalIdBagTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testPreUpdateModifications(EntityManagerFactoryScope scope) {
		Person person = new Person();
		person.id = 1;

		scope.inTransaction(
				entityManager -> entityManager.persist( person )
		);

		scope.inTransaction(
				entityManager -> {
					Person p = entityManager.find( Person.class, person.id );
					assertNotNull( p );
					final Tag tag = new Tag();
					tag.id = 2;
					tag.description = "description";
					final Set<Tag> tags = new HashSet<Tag>();
					tags.add( tag );
					p.tags = tags;
					entityManager.merge( p );
				}
		);

		scope.inTransaction(
				entityManager -> {
					Person p = entityManager.find( Person.class, person.id );
					assertEquals( 1, p.tags.size() );
					assertEquals( "description", p.tags.iterator().next().description );
					assertNotNull( p.getLastUpdatedAt() );
				}
		);
	}

	@Entity(name = "Person")
	@EntityListeners( PersonListener.class )
	@GenericGenerator(name="increment", strategy = "increment")
	public static class Person {
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

		@CollectionId( column = @Column(name = "n_key_tag"), generator = "increment" )
		@CollectionIdJdbcTypeCode( Types.BIGINT )
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
