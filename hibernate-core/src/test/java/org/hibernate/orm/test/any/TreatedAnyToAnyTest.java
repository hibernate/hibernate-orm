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
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Expected behavior:
 * <p>
 * Loading an entity whose eager fetch graph walks through an {@link org.hibernate.annotations.Any}
 * association into a <em>second</em> {@code @Any} association must succeed. No HQL is involved: the
 * {@code treat()} is generated implicitly by the entity loader while building the select.
 * </p>
 *
 * <p>
 * Here {@link Container#reference} is an eager {@code @Any} (declared type {@link Reference}). A plain
 * {@code entityManager.find(Container.class, id)} eagerly join-fetches {@code reference}, resolves it to
 * {@link Publication}, and must read {@link Publication#destinataire} — itself an {@code @Any} (declared
 * type {@link Recipient}) whose discriminator/key columns live on {@code Publication}'s own table
 * ({@code TPUBLICATION}). The inner {@code @Any} discriminator is always fetched {@code IMMEDIATE}.
 * </p>
 *
 * <p>
 * This worked up to 7.3.5.Final and regressed in 7.4.x (still failing on 7.4.3.Final and on
 * {@code main} / 8.1-SNAPSHOT), apparently because of HHH-16730 ("joining @Any and @ManyToAny
 * mappings", 7.4.0.CR1): {@code DiscriminatedAssociationAttributeMapping} switched from a
 * {@code StandardVirtualTableGroup} (which resolved the discriminator/key columns against the owner
 * table) + {@code FetchStyle.SELECT} to a real {@code createRootTableGroupJoin(...)} +
 * {@code FetchStyle.JOIN} for {@code IMMEDIATE} fetching. Under the implicit {@code treat()} over an
 * {@code @Any}, the resulting table group no longer exposes the owner table, so fetching the inner
 * {@code @Any} discriminator fails with:
 * </p>
 *
 * <pre>
 * org.hibernate.sql.ast.tree.from.UnknownTableReferenceException:
 *     Unable to determine TableReference (`TPUBLICATION`) for
 *     `treat(container.reference as Publication).destinataire.{discriminator}`
 *         at AnyDiscriminatorPart.generateFetch(AnyDiscriminatorPart.java:375)
 *         at FetchParent.generateFetchableFetch(FetchParent.java:120)
 *         at LoaderSelectBuilder.lambda$createFetchableConsumer$0(...)
 * </pre>
 *
 * @author Vincent Bouthinon
 */
@Jpa(
		annotatedClasses = {
				TreatedAnyToAnyTest.Container.class,
				TreatedAnyToAnyTest.Publication.class,
				TreatedAnyToAnyTest.Person.class
		},
		integrationSettings = @Setting(name = JdbcSettings.SHOW_SQL, value = "true")
)
@JiraKey("HHH-20632")
class TreatedAnyToAnyTest {

	@Test
	void testLoadEntityWithEagerAnyToAnotherAny(EntityManagerFactoryScope scope) {

		final Long[] containerId = new Long[1];

		scope.inTransaction(
				entityManager -> {
					Person person = new Person();
					entityManager.persist( person );

					Publication publication = new Publication();
					publication.setDestinataire( person );
					entityManager.persist( publication );

					Container container = new Container();
					container.setReference( publication );
					entityManager.persist( container );

					containerId[0] = container.getId();
				}
		);

		scope.inTransaction(
				entityManager -> {
					// No HQL: the eager @Any 'reference' makes the loader walk into Publication and read
					// its inner @Any 'destinataire' discriminator (implicit treat over the @Any).
					Container container = entityManager.find( Container.class, containerId[0] );

					assertNotNull( container );
					assertNotNull( container.getReference() );
				}
		);
	}

	@Entity(name = "Container")
	@Table(name = "TCONTAINER")
	public static class Container {

		@Id
		@GeneratedValue
		private Long id;

		@Any(fetch = FetchType.EAGER)
		@AnyKeyJavaClass(Long.class)
		@AnyDiscriminatorValue(entity = Publication.class, discriminator = "P")
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

	@Entity(name = "Publication")
	@Table(name = "TPUBLICATION")
	public static class Publication implements Reference {

		@Id
		@GeneratedValue
		private Long id;

		@Any(fetch = FetchType.LAZY)
		@AnyKeyJavaClass(Long.class)
		@AnyDiscriminatorValue(entity = Person.class, discriminator = "U")
		@Column(name = "DESTINATAIRE_ROLE")
		@JoinColumn(name = "DESTINATAIRE_ID")
		private Recipient destinataire;

		public Long getId() {
			return id;
		}

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
	 * Declared type of the outer {@code @Any} ({@link Container#reference}).
	 */
	public interface Reference {
	}

	/**
	 * Declared type of the inner {@code @Any} ({@link Publication#destinataire}).
	 */
	public interface Recipient {
	}
}
