/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import org.hibernate.annotations.Audited;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.SharedSessionContract;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests @Audited with composite identifiers: @EmbeddedId, @IdClass,
 * and non-aggregated multiple @Id.
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditCompositeIdTest.EmbeddedIdEntity.class,
		AuditCompositeIdTest.IdClassEntity.class,
		AuditCompositeIdTest.MultiIdEntity.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditCompositeIdTest$TxIdSupplier"))
class AuditCompositeIdTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}

	}

	@Test
	void testEmbeddedId(SessionFactoryScope scope) {
		currentTxId = 0;
		final var key = new CompositeKey( 1L, 2L );

		scope.inTransaction( session -> {
			var e = new EmbeddedIdEntity();
			e.id = key;
			e.data = "initial";
			session.persist( e );
		} );

		scope.inTransaction( session ->
				session.find( EmbeddedIdEntity.class, key ).data = "updated" );

		try (var s = scope.getSessionFactory().withOptions().atChangeset( 1 ).openSession()) {
			var e = s.find( EmbeddedIdEntity.class, key );
			assertThat( e ).isNotNull();
			assertThat( e.data ).isEqualTo( "initial" );
		}

		try (var s = scope.getSessionFactory().withOptions().atChangeset( 2 ).openSession()) {
			var e = s.find( EmbeddedIdEntity.class, key );
			assertThat( e ).isNotNull();
			assertThat( e.data ).isEqualTo( "updated" );
		}
	}

	@Test
	void testIdClass(SessionFactoryScope scope) {
		currentTxId = 100;
		final var key = new CompositeKey( 10L, 20L );

		scope.inTransaction( session -> {
			var e = new IdClassEntity();
			e.part1 = 10L;
			e.part2 = 20L;
			e.data = "initial";
			session.persist( e );
		} );

		scope.inTransaction( session ->
				session.find( IdClassEntity.class, key ).data = "updated" );

		try (var s = scope.getSessionFactory().withOptions().atChangeset( 101 ).openSession()) {
			var e = s.find( IdClassEntity.class, key );
			assertThat( e ).isNotNull();
			assertThat( e.data ).isEqualTo( "initial" );
		}

		try (var s = scope.getSessionFactory().withOptions().atChangeset( 102 ).openSession()) {
			var e = s.find( IdClassEntity.class, key );
			assertThat( e ).isNotNull();
			assertThat( e.data ).isEqualTo( "updated" );
		}
	}

	@Test
	void testMultipleId(SessionFactoryScope scope) {
		currentTxId = 200;

		scope.inTransaction( session -> {
			var e = new MultiIdEntity();
			e.pk1 = 10L;
			e.pk2 = 20L;
			e.data = "initial";
			session.persist( e );
		} );

		scope.inTransaction( session ->
				session.createSelectionQuery(
								"from MultiIdEntity where pk1 = 10 and pk2 = 20", MultiIdEntity.class )
						.getSingleResult().data = "updated" );

		try (var s = scope.getSessionFactory().withOptions().atChangeset( 201 ).openSession()) {
			var e = s.createSelectionQuery(
							"from MultiIdEntity where pk1 = 10 and pk2 = 20", MultiIdEntity.class )
					.getSingleResultOrNull();
			assertThat( e ).isNotNull();
			assertThat( e.data ).isEqualTo( "initial" );
		}

		try (var s = scope.getSessionFactory().withOptions().atChangeset( 202 ).openSession()) {
			var e = s.createSelectionQuery(
							"from MultiIdEntity where pk1 = 10 and pk2 = 20", MultiIdEntity.class )
					.getSingleResultOrNull();
			assertThat( e ).isNotNull();
			assertThat( e.data ).isEqualTo( "updated" );
		}
	}

	// ---- Entity classes ----

	@Embeddable
	static class CompositeKey implements Serializable {
		long part1;
		long part2;

		CompositeKey() {
		}

		CompositeKey(long part1, long part2) {
			this.part1 = part1;
			this.part2 = part2;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof CompositeKey ck && part1 == ck.part1 && part2 == ck.part2;
		}

		@Override
		public int hashCode() {
			return Objects.hash( part1, part2 );
		}
	}

	@Audited
	@Entity(name = "EmbeddedIdEntity")
	static class EmbeddedIdEntity {
		@EmbeddedId
		CompositeKey id;
		String data;
	}

	@Audited
	@Entity(name = "IdClassEntity")
	@IdClass(CompositeKey.class)
	static class IdClassEntity {
		@Id
		long part1;
		@Id
		long part2;
		String data;
	}

	@Audited
	@Entity(name = "MultiIdEntity")
	static class MultiIdEntity {
		@Id
		long pk1;
		@Id
		long pk2;
		String data;
	}
}
