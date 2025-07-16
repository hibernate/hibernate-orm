/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import jakarta.persistence.Basic;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Jpa(annotatedClasses =
		{SingleTableConstraintsTest.Author.class,
				SingleTableConstraintsTest.Publisher.class,
				SingleTableConstraintsTest.Publication.class,
				SingleTableConstraintsTest.Journal.class,
				SingleTableConstraintsTest.Paper.class,
				SingleTableConstraintsTest.Monograph.class})
class SingleTableConstraintsTest {
	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			Monograph monograph = new Monograph();
			monograph.id = 1;
			monograph.title = "title";
			monograph.text = "text";
			monograph.pages = 10;
			monograph.edition = 1;
			Publisher publisher = new Publisher();
			publisher.name = "publisher";
			monograph.publisher = publisher;
			em.persist( publisher );
			em.persist( monograph );

			Journal journal = new Journal();
			journal.id = 2;
			journal.title = "Journal Title";
			journal.text = "Journal Content";
			journal.pages = 100;
			Author editor = new Author();
			editor.ssn = "123-45-6789";
			editor.name = "John Editor";
			journal.editor = editor;
			em.persist( editor );
			em.persist( journal );

			Paper paper = new Paper();
			paper.id = 3;
			paper.title = "Paper Title";
			paper.text = "Paper Content";
			paper.pages = 10;
			Author reviewer = new Author();
			reviewer.ssn = "987-65-4321";
			reviewer.name = "Jane Reviewer";
			paper.reviewer = reviewer;
			paper.journal = journal;
			em.persist( reviewer );
			em.persist( paper );
		} );

		scope.inTransaction( em -> {
			try {
				em.createNativeQuery(
							"""
							insert into Publication (text, title, edition,\spublisher_id, type, id)
							values ('Lorem ipsum', 'Lorem Ipsum', 1, 1, 'Monograph', 5)
							""" )
						.executeUpdate();
				fail();
			}
			catch (ConstraintViolationException expected) {
				assertEquals( ConstraintViolationException.ConstraintKind.NOT_NULL, expected.getKind() );
			}
		} );
		scope.inTransaction( em -> {
			try {
				em.createNativeQuery(
							"""
							insert into Publication (pages, text, edition, publisher_id, type, id)
							values (20, 'Lorem ipsum', 1, 1, 'Monograph', 5)
							""" )
						.executeUpdate();
				fail();
			}
			catch (ConstraintViolationException expected) {
				assertEquals( ConstraintViolationException.ConstraintKind.NOT_NULL, expected.getKind() );
			}
		} );
		scope.inTransaction( em -> {
			try {
				em.createNativeQuery(
							"""
							insert into Publication (pages, text, title, publisher_id, type, id)
							values (100, 'Lorem ipsum', 'Lorem Ipsum', 1, 'Monograph', 5)
							""" )
					.executeUpdate();
				fail();
			}
			catch (ConstraintViolationException expected) {
				assertEquals( ConstraintViolationException.ConstraintKind.CHECK, expected.getKind() );
			}
		} );
		scope.inTransaction( em -> {
			try {
				em.createNativeQuery(
							"""
							insert into Publication (pages, text, title, edition, type, id)
							values (100, 'Lorem ipsum', 'Lorem Ipsum', 1, 'Monograph', 5)
							""" )
						.executeUpdate();
				fail();
			}
			catch (ConstraintViolationException expected) {
				assertEquals( ConstraintViolationException.ConstraintKind.CHECK, expected.getKind() );
			}
		} );
		scope.inTransaction( em -> {
			try {
				em.createNativeQuery(
							"""
							insert into Publication (pages, text, title, edition, publisher_id, type, id)
							values (100, 'Lorem ipsum', 'Lorem Ipsum', 1, 1, 'Shrubbery', 5)
							""" )
						.executeUpdate();
				fail();
			}
			catch (ConstraintViolationException expected) {
				assertEquals( ConstraintViolationException.ConstraintKind.CHECK, expected.getKind() );
			}
		} );
		scope.inTransaction( em -> {
			em.createNativeQuery(
							"""
							insert into Publication (pages, text, title, edition, publisher_id, type, id)
							values (100, 'Lorem ipsum', 'Lorem Ipsum', 1, 1, 'Monograph', 5)
							""" )
					.executeUpdate();
		} );
	}

	@Entity(name = "Publication")
	@DiscriminatorColumn(name = "type")
	static abstract class Publication {
		@Id
		long id;
		@Basic(optional = false)
		String title;
		@Basic(optional = false)
		String text;
		int pages;
	}
	@Entity(name = "Monograph")
	static class Monograph extends Publication {
		@ManyToOne(optional = false)
		Publisher publisher;
		int edition;
	}
	@Entity(name = "Paper")
	static class Paper extends Publication {
		@ManyToOne(optional = false)
		Journal journal;
		@ManyToOne(optional = false)
		Author reviewer;
	}
	@Entity(name = "Journal")
	static class Journal extends Publication {
		@ManyToOne(optional = false)
		Author editor;
	}
	@Entity(name = "Author")
	static class Author {
		@Id
		String ssn;

		@NotNull
		String name;
	}
	@Entity(name = "Publisher")
	static class Publisher {
		@Id
		@GeneratedValue
		long id;

		@NotNull
		String name;
	}
}
