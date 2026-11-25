package org.hibernate.temporal.audit;

import java.util.List;

import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditEntry;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.audit.ModificationType;

import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Audited changes made by a session which shares the transaction of the session
 * that opened it are written when that transaction completes.
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = AuditSharedTransactionTest.SharedEntity.class)
@JiraKey("HHH-21261")
class AuditSharedTransactionTest {

	@Audited
	@Entity(name = "SharedEntity")
	static class SharedEntity {
		@Id
		Long id;
		String name;
	}

	@Test
	void testChangesInLaterTransactions(SessionFactoryScope scope) {
		scope.inTransaction( parent -> {
			try (var child = parent.sessionWithOptions().connection().openSession()) {
				final var entity = new SharedEntity();
				entity.id = 1L;
				entity.name = "original";
				child.persist( entity );
				child.flush();
			}
		} );
		scope.inTransaction( parent -> {
			try (var child = parent.sessionWithOptions().connection().openSession()) {
				child.find( SharedEntity.class, 1L ).name = "updated";
				child.flush();
			}
		} );
		scope.inTransaction( parent -> {
			try (var child = parent.sessionWithOptions().connection().openSession()) {
				child.remove( child.find( SharedEntity.class, 1L ) );
				child.flush();
			}
		} );

		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertEquals(
					List.of( ModificationType.ADD, ModificationType.MOD, ModificationType.DEL ),
					auditLog.getHistory( SharedEntity.class, 1L ).stream()
							.map( AuditEntry::modificationType )
							.toList()
			);
		}
	}

	/**
	 * The insert and the update are merged into a single {@code ADD} row.
	 */
	@Test
	void testChangesInSameTransaction(SessionFactoryScope scope) {
		scope.inTransaction( parent -> {
			try (var child = parent.sessionWithOptions().connection().openSession()) {
				final var entity = new SharedEntity();
				entity.id = 2L;
				entity.name = "before";
				child.persist( entity );
				child.flush();
				entity.name = "after";
				child.flush();
			}
		} );

		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var history = auditLog.getHistory( SharedEntity.class, 2L );
			assertEquals( 1, history.size() );
			assertEquals( ModificationType.ADD, history.get( 0 ).modificationType() );
			assertEquals( "after", history.get( 0 ).entity().name );
		}
	}
}
