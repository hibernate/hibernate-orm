/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests for the JPA 4 [jakarta.persistence.EntityManagerFactory#addListener] feature
///
/// @author Steve Ebersole
@Jpa( annotatedClasses = AddListenerTests.Stuff.class)
public class AddListenerTests {
	@BeforeEach
	void testPersist(EntityManagerFactoryScope factoryScope) {
		var listener = new StuffListener();

		factoryScope.inTransaction( (em) -> {
			final var prePersistReg = em.getEntityManagerFactory().addListener(
					Stuff.class,
					PrePersist.class,
					listener::capture
			);

			em.persist( new Stuff( 1, "th!ngs" ) );
			assertThat( listener.callCount ).isEqualTo( 1 );

			prePersistReg.cancel();

			em.persist( new Stuff( 2, "irrelevant" ) );
			assertThat( listener.callCount ).isEqualTo( 1 );
		} );
	}

	@Test
	void testLoadAndUpdate(EntityManagerFactoryScope factoryScope) {
		var listener = new StuffListener();

		factoryScope.inTransaction( (em) -> {
			var postLoadReg = em.getEntityManagerFactory().addListener(
					Stuff.class,
					PostLoad.class,
					listener::capture
			);

			var first = em.get( Stuff.class, 1 );
			assertThat( listener.callCount ).isEqualTo( 1 );

			postLoadReg.cancel();

			var second = em.get( Stuff.class, 2 );
			assertThat( listener.callCount ).isEqualTo( 1 );

			listener.reset();

			var postUpdateReg = em.getEntityManagerFactory().addListener(
					Stuff.class,
					PostUpdate.class,
					listener::capture
			);
			first.name = "fixed";
			em.flush();
			assertThat( listener.callCount ).isEqualTo( 1 );

			postUpdateReg.cancel();

			second.name = "altered";
			em.flush();
			assertThat( listener.callCount ).isEqualTo( 1 );
		} );
	}

	@Test
	void testRemoval(EntityManagerFactoryScope factoryScope) {
		var listener = new StuffListener();

		factoryScope.inTransaction( (em) -> {
			var first = em.get( Stuff.class, 1 );
			var second = em.get( Stuff.class, 2 );

			var postRemoveReg = em.getEntityManagerFactory().addListener(
					Stuff.class,
					PostRemove.class,
					listener::capture
			);

			em.remove( first );
			em.flush();
			assertThat( listener.callCount ).isEqualTo( 1 );

			postRemoveReg.cancel();

			em.remove( second );
			em.flush();
			assertThat( listener.callCount ).isEqualTo( 1 );
		} );
	}

	@AfterEach
	void dropTestData(EntityManagerFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Entity(name="Stuff")
	@Table(name="stuffs")
	public static class Stuff {
		@Id
		private Integer id;
		private String name;

		public Stuff() {
		}

		public Stuff(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	public static class StuffListener {
		public int callCount;

		public void capture(Stuff stuff) {
			callCount++;
		}

		public void reset() {
			callCount = 0;
		}
	}
}
