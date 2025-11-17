/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				JoinInheritanceInsertTest.Book.class,
				JoinInheritanceInsertTest.SpellBook.class
		}
)
@SessionFactory
@Jira( "HHH-19669" )
public class JoinInheritanceInsertTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testInsertNoRootFieldsExceptForTheId(SessionFactoryScope scope) {
		Integer id = 1;
		scope.inTransaction( session -> {
			session.createMutationQuery( "insert into SpellBook (id, forbidden) values (:id, :forbidden)" )
					.setParameter( "id", id )
					.setParameter( "forbidden", true )
					.executeUpdate();
		} );

		scope.inTransaction( session -> {
			SpellBook spellBook = session.find( SpellBook.class, id );
			assertThat( spellBook ).isNotNull();
			assertThat( spellBook.getForbidden() ).isTrue();
			assertThat( spellBook.getTitle() ).isNull();
		} );
	}

	@Test
	public void testInsert2(SessionFactoryScope scope) {
		Integer id = 1;
		String title = "Spell Book: A Comprehensive Guide to Magic Spells and Incantations";

		scope.inTransaction( session -> {
			session.createMutationQuery( "insert into SpellBook (id, title, forbidden) values (:id, :title, :forbidden)" )
					.setParameter( "id", id )
					.setParameter( "title", title )
					.setParameter( "forbidden", true )
					.executeUpdate();
		} );

		scope.inTransaction( session -> {
			SpellBook spellBook = session.find( SpellBook.class, id );
			assertThat( spellBook ).isNotNull();
			assertThat( spellBook.getTitle() ).isEqualTo( title );
			assertThat( spellBook.getForbidden() ).isTrue();
		} );
	}

	@Entity(name = "Book")
	@Table(name = "BookJS")
	@Inheritance(strategy = InheritanceType.JOINED)
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

		public Integer getId() {
			return id;
		}

		public String getTitle() {
			return title;
		}
	}

	@Entity(name = "SpellBook")
	@Table(name = "SpellBookJS")
	public static class SpellBook extends Book {

		private boolean forbidden;

		SpellBook() {
		}

		public SpellBook(Integer id, String title, boolean forbidden) {
			super( id, title );
			this.forbidden = forbidden;
		}

		public boolean getForbidden() {
			return forbidden;
		}
	}
}
