/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.compound;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.HibernateException;
import org.hibernate.annotations.NaturalId;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@JiraKey("HHH-10326")
@DomainModel(annotatedClasses = {
		CompoundMixedNaturalIdTest.Owner.class,
		CompoundMixedNaturalIdTest.Thing.class
})
@SessionFactory
public class CompoundMixedNaturalIdTest {

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testImmutableAttributeWithinCompoundShouldNotBeChangeable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Owner first = new Owner();
			session.persist( first );

			final Thing thing = new Thing();
			thing.owner = first;
			thing.name = "name";
			session.persist( thing );
		} );

		scope.inTransaction( session -> {
			final Thing thing = session.createQuery( "from Thing", Thing.class )
					.uniqueResult();

			final Owner second = new Owner();
			session.persist( second );

			thing.owner = second;

			try {
				session.flush();
				fail( "Changing an immutable natural id attribute should have failed" );
			}
			catch (HibernateException expected) {
				assertThat( expected.getMessage() ).contains( "immutable" );
			}
		} );
	}

	@Test
	void testMutableAttributeWithinCompoundShouldBeChangeable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Owner owner = new Owner();
			session.persist( owner );

			final Thing thing = new Thing();
			thing.owner = owner;
			thing.name = "original";
			session.persist( thing );
		} );

		scope.inTransaction( session -> {
			final Thing thing = session.createQuery( "from Thing", Thing.class )
					.uniqueResult();

			thing.name = "updated";
			session.flush();
		} );
	}

	@Entity(name = "Owner")
	@Table(name = "owners")
	public static class Owner {
		@Id
		@GeneratedValue
		Integer id;
	}

	@Entity(name = "Thing")
	@Table(name = "things")
	public static class Thing {
		@Id
		@GeneratedValue
		Integer id;

		@NaturalId
		@ManyToOne
		Owner owner;

		@NaturalId(mutable = true)
		String name;
	}
}
