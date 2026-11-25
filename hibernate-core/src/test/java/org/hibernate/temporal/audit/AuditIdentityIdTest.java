package org.hibernate.temporal.audit;

import java.util.List;

import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditEntry;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.audit.ModificationType;

import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = AuditIdentityIdTest.IdentityEntity.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@JiraKey("HHH-21060")
class AuditIdentityIdTest {

	@Audited
	@Entity(name = "IdentityEntity")
	static class IdentityEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Long id;
		String name;
	}

	@Test
	void testPersistUpdateDelete(SessionFactoryScope scope) {
		final var entity = new IdentityEntity();
		entity.name = "original";
		scope.getSessionFactory().inTransaction( session -> session.persist( entity ) );
		assertNotNull( entity.id );
		scope.getSessionFactory().inTransaction(
				session -> session.find( IdentityEntity.class, entity.id ).name = "updated"
		);
		scope.getSessionFactory().inTransaction(
				session -> session.remove( session.find( IdentityEntity.class, entity.id ) )
		);

		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var changesets = auditLog.getChangesets( IdentityEntity.class, entity.id );
			assertEquals( 3, changesets.size() );
			assertEquals( "original", findAt( scope, changesets.get( 0 ), entity.id ).name );
			assertEquals( "updated", findAt( scope, changesets.get( 1 ), entity.id ).name );
			assertNull( findAt( scope, changesets.get( 2 ), entity.id ) );
			assertEquals(
					List.of( ModificationType.ADD, ModificationType.MOD, ModificationType.DEL ),
					auditLog.getHistory( IdentityEntity.class, entity.id ).stream()
							.map( AuditEntry::modificationType )
							.toList()
			);
		}
	}

	/**
	 * An insert and an update of the same entity in one transaction
	 * are merged into a single {@code ADD} row with the final state.
	 */
	@Test
	void testAddModMerge(SessionFactoryScope scope) {
		final var entity = new IdentityEntity();
		entity.name = "before";
		scope.getSessionFactory().inTransaction( session -> {
			session.persist( entity );
			session.flush();
			entity.name = "after";
		} );

		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var history = auditLog.getHistory( IdentityEntity.class, entity.id );
			assertEquals( 1, history.size() );
			assertEquals( ModificationType.ADD, history.get( 0 ).modificationType() );
			assertEquals( "after", history.get( 0 ).entity().name );
		}
	}

	/**
	 * An insert and a delete of the same entity in one transaction
	 * cancel out, leaving no audit row.
	 */
	@Test
	void testAddDeleteCancellation(SessionFactoryScope scope) {
		final var entity = new IdentityEntity();
		entity.name = "ephemeral";
		scope.getSessionFactory().inTransaction( session -> {
			session.persist( entity );
			session.flush();
			session.remove( entity );
		} );

		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertEquals( 0, auditLog.getChangesets( IdentityEntity.class, entity.id ).size() );
		}
	}

	private static IdentityEntity findAt(SessionFactoryScope scope, Object changeset, Long id) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).open()) {
			return session.find( IdentityEntity.class, id );
		}
	}
}
