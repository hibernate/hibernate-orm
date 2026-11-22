package org.hibernate.temporal.audit;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Audited;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = AuditWithNativeTemporalStrategyTest.Book.class)
@ServiceRegistry(settings = @Setting(name = TEMPORAL_TABLE_STRATEGY, value = "NATIVE"))
class AuditWithNativeTemporalStrategyTest {
	@Test
	void readCurrentAndHistoricalState(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Book() ) );
		scope.inSession( session -> assertNotNull( session.find( Book.class, 1 ) ) );
		try ( var session = scope.getSessionFactory().withOptions().asOf( Instant.EPOCH ).openSession() ) {
			assertNull( session.find( Book.class, 1 ) );
		}
	}

	@Audited
	@Entity(name = "Book")
	static class Book {
		@Id
		int id = 1;
	}
}
