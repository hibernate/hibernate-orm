package org.hibernate.orm.test.stateless.insert;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.SessionEventListener;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.annotations.SourceType.DB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@JiraKey("HHH-21093")
@DomainModel(annotatedClasses = TimestampVersionBatchTest.TimestampedEntry.class)
@SessionFactory
class TimestampVersionBatchTest {
	@BeforeEach
	void checkDialect(SessionFactoryScope scope) {
		final var factory = scope.getSessionFactory();
		assumeTrue( factory.getJdbcServices().getExtractedMetaDataSupport().supportsBatchUpdates() );
		assumeTrue( factory.getJdbcServices().getDialect().getCurrentTemporalSupport()
				.getCurrentTimestampSelection() != null );
	}

	@AfterEach
	void dropData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void fetchingVersionTimestampsPreservesTheBatch(SessionFactoryScope scope) {
		final var inserts = new AtomicInteger();
		scope.inStatelessTransaction( session -> {
			countBatches( session, inserts );
			session.insertMultiple( List.of( new TimestampedEntry( 1L ), new TimestampedEntry( 2L ),
					new TimestampedEntry( 3L ) ) );
		} );
		assertEquals( 1, inserts.get() );

		final var updates = new AtomicInteger();
		scope.inStatelessTransaction( session -> {
			final var entries = session.createSelectionQuery( "from TimestampedEntry", TimestampedEntry.class )
					.getResultList();
			entries.forEach( entry -> {
				assertNotNull( entry.timestamp );
				entry.content = "updated";
			} );
			countBatches( session, updates );
			session.updateMultiple( entries );
		} );
		assertEquals( 1, updates.get() );

		scope.inTransaction( session -> assertEquals( 3L,
				session.createQuery( "select count(e) from TimestampedEntry e where e.content = 'updated'", Long.class )
						.getSingleResult() ) );
	}

	private static void countBatches(StatelessSessionImplementor session, AtomicInteger batchExecutions) {
		session.getEventListenerManager().addListener( new SessionEventListener() {
			@Override
			public void jdbcExecuteBatchStart() {
				batchExecutions.incrementAndGet();
			}
		} );
	}

	@Entity(name = "TimestampedEntry")
	static class TimestampedEntry {
		@Id
		Long id;
		@Version
		@CurrentTimestamp(source = DB)
		LocalDateTime timestamp;
		String content;

		TimestampedEntry() {
		}

		TimestampedEntry(Long id) {
			this.id = id;
		}
	}
}
