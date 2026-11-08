package org.hibernate.orm.test.stateless.insert;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import org.hibernate.SessionEventListener;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import static jakarta.persistence.GenerationType.SEQUENCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@JiraKey("HHH-21093")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSequences.class)
@DomainModel(annotatedClasses = SequenceBatchInsertTest.SequenceEntry.class)
@SessionFactory
class SequenceBatchInsertTest {
	@Test
	void fetchingIdsPreservesTheInsertBatch(SessionFactoryScope scope) {
		assumeTrue( scope.getSessionFactory().getJdbcServices().getExtractedMetaDataSupport().supportsBatchUpdates() );
		var batchExecutions = new AtomicInteger();
		scope.inStatelessTransaction( session -> {
			session.getEventListenerManager().addListener( new SessionEventListener() {
				@Override
				public void jdbcExecuteBatchStart() {
					batchExecutions.incrementAndGet();
				}
			} );
			session.insertMultiple( List.of( new SequenceEntry(), new SequenceEntry(), new SequenceEntry() ) );
		} );

		assertEquals( 1, batchExecutions.get() );
		scope.inTransaction( session -> assertEquals( 3L,
				session.createQuery( "select count(e) from SequenceEntry e", Long.class ).getSingleResult() ) );
	}

	@Entity(name = "SequenceEntry")
	static class SequenceEntry {
		@Id
		@GeneratedValue(strategy = SEQUENCE, generator = "entry_seq")
		@SequenceGenerator(name = "entry_seq", allocationSize = 1)
		Long id;
	}
}
