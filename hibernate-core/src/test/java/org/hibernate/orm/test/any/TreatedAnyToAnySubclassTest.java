/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.cfg.JdbcSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Same regression as {@link TreatedAnyToAnyTest}, but with the second {@link Any} declared by a
 * <em>subclass</em> of the entity the first {@code @Any} resolves to, and mapped to a secondary table.
 * <p>
 * {@link SubclassContainer#reference} is an eager {@code @Any} listing {@link Journal} - the root of a
 * single-table hierarchy - as its only discriminator value. The inner {@code @Any}
 * {@link FilteredJournal#destinataire} is however declared one level below, by {@code Journal}'s subclass
 * {@link FilteredJournal}, with its discriminator and key columns on the secondary table
 * {@code TJOURNAL_FILTERED}. The loader therefore has to read that inner {@code @Any} through
 * {@code treat(SubclassContainer.reference as FilteredJournal)}.
 * </p>
 *
 * <p>
 * The fix for HHH-20632 resolves such a treated path through
 * {@code FromClauseAccess#findTableGroup}, which only succeeds for the paths
 * {@code DiscriminatedAssociationMapping#addConcreteEntityTableGroupJoin} registers, that is
 * {@code treat(owner.any as TheListedEntity)} - one per entity the {@code @Any} lists as a discriminator
 * value. Nothing is ever registered under a <em>subclass</em> of a listed entity, so the lookup missed and
 * fell back to the {@code @Any}'s own {@code StandardVirtualTableGroup}, which only delegates to the
 * owner's table group and its table reference joins, never to the concrete entity table group joins.
 * Building the loader select then failed with:
 * </p>
 *
 * <pre>
 * org.hibernate.sql.ast.tree.from.UnknownTableReferenceException:
 *     Unable to determine TableReference (`TJOURNAL_FILTERED`) for
 *     `treat(SubclassContainer.reference as FilteredJournal).destinataire.{discriminator}`
 *         at AnyDiscriminatorPart.generateFetch(AnyDiscriminatorPart.java:334)
 *         at FetchParent.generateFetchableFetch(FetchParent.java:120)
 *         at LoaderSelectBuilder.lambda$createFetchableConsumer$0(...)
 * </pre>
 *
 * <p>
 * Because {@code SessionFactory} construction eagerly prepares every persister's loaders, this failed at
 * bootstrap, independently of which entity was queried.
 * </p>
 *
 * @author Stéphane Ferreira
 *
 * @see TreatedAnyToAnyTest
 */
@Jpa(
		annotatedClasses = {
				TreatedAnyToAnySubclassTest.SubclassContainer.class,
				TreatedAnyToAnySubclassTest.Journal.class,
				TreatedAnyToAnySubclassTest.FilteredJournal.class,
				TreatedAnyToAnySubclassTest.Person.class
		},
		integrationSettings = @Setting(name = JdbcSettings.SHOW_SQL, value = "true")
)
@JiraKey("HHH-20871")
class TreatedAnyToAnySubclassTest {

	@Test
	void testLoadEntityWithEagerAnyToAnyDeclaredOnSubclassSecondaryTable(EntityManagerFactoryScope scope) {

		final Long[] ids = new Long[2];

		scope.inTransaction(
				entityManager -> {
					Person person = new Person();
					entityManager.persist( person );

					FilteredJournal filteredJournal = new FilteredJournal();
					filteredJournal.setDestinataire( person );
					entityManager.persist( filteredJournal );

					Journal journal = new Journal();
					entityManager.persist( journal );

					SubclassContainer container = new SubclassContainer();
					container.setReference( journal );
					entityManager.persist( container );

					ids[0] = container.getId();
					ids[1] = filteredJournal.getId();
				}
		);

		scope.inTransaction(
				entityManager -> {
					// No HQL: the eager @Any 'reference' makes the loader walk the whole Journal hierarchy and
					// read the inner @Any 'destinataire' declared by the FilteredJournal subclass, on its
					// secondary table - through an implicit treat onto a subclass of the listed entity.
					SubclassContainer container = entityManager.find( SubclassContainer.class, ids[0] );

					assertNotNull( container );
					assertInstanceOf( Journal.class, container.getReference() );

					// the secondary table is joined and read by the very same select, so executing it also
					// checks the qualifier it was resolved against, not merely that the SQL AST could be built
					FilteredJournal filteredJournal = entityManager.find( FilteredJournal.class, ids[1] );

					assertNotNull( filteredJournal );
					assertNotNull( filteredJournal.getDestinataire() );
				}
		);
	}

	@Entity(name = "SubclassContainer")
	@Table(name = "TSUBCONTAINER")
	public static class SubclassContainer {

		@Id
		@GeneratedValue
		private Long id;

		@Any(fetch = FetchType.EAGER)
		@AnyKeyJavaClass(Long.class)
		@AnyDiscriminatorValue(entity = Journal.class, discriminator = "J")
		@Column(name = "REFERENCE_ROLE")
		@JoinColumn(name = "REFERENCE_ID")
		private Reference reference;

		public Long getId() {
			return id;
		}

		public Reference getReference() {
			return reference;
		}

		public void setReference(final Reference reference) {
			this.reference = reference;
		}
	}

	/**
	 * Root of the hierarchy, and the entity {@link SubclassContainer#reference} lists as a discriminator
	 * value. It declares no {@code @Any} itself.
	 */
	@Entity(name = "Journal")
	@Table(name = "TJOURNAL")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "JOURNAL_TYPE")
	@DiscriminatorValue("J")
	public static class Journal implements Reference {

		@Id
		@GeneratedValue
		private Long id;

		public Long getId() {
			return id;
		}
	}

	/**
	 * Subclass of the listed entity, declaring the inner {@code @Any} on a secondary table. No table group
	 * is ever registered under {@code treat(SubclassContainer.reference as FilteredJournal)}.
	 */
	@Entity(name = "FilteredJournal")
	@DiscriminatorValue("F")
	@SecondaryTable(name = "TJOURNAL_FILTERED", pkJoinColumns = @PrimaryKeyJoinColumn(name = "JOURNAL_ID"))
	public static class FilteredJournal extends Journal {

		@Any(fetch = FetchType.LAZY)
		@AnyKeyJavaClass(Long.class)
		@AnyDiscriminatorValue(entity = Person.class, discriminator = "U")
		@Column(name = "DESTINATAIRE_ROLE", table = "TJOURNAL_FILTERED")
		@JoinColumn(name = "DESTINATAIRE_ID", table = "TJOURNAL_FILTERED")
		private Recipient destinataire;

		public Recipient getDestinataire() {
			return destinataire;
		}

		public void setDestinataire(final Recipient destinataire) {
			this.destinataire = destinataire;
		}
	}

	@Entity(name = "Person")
	@Table(name = "TPERSON")
	public static class Person implements Recipient {

		@Id
		@GeneratedValue
		private Long id;

		public Long getId() {
			return id;
		}
	}

	/**
	 * Declared type of the outer {@code @Any} ({@link SubclassContainer#reference}).
	 */
	public interface Reference {
	}

	/**
	 * Declared type of the inner {@code @Any} ({@link FilteredJournal#destinataire}).
	 */
	public interface Recipient {
	}
}
