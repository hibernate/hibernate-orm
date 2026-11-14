package org.hibernate.temporal;

import org.hibernate.annotations.Temporal;
import org.hibernate.cfg.StateManagementSettings;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

import static org.hibernate.testing.orm.junit.DialectContext.awaitTimestampTick;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SessionFactory
@DomainModel(annotatedClasses = TemporalEntityHistoryIdentityIdTest.IdentityTemporalEntity.class)
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"))
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@JiraKey("HHH-21060")
class TemporalEntityHistoryIdentityIdTest {

	@Temporal(rowStart = "effective_from", rowEnd = "effective_to")
	@Entity(name = "IdentityTemporalEntity")
	static class IdentityTemporalEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Long id;
		String text;
	}

	@Test
	void testPersistWithIdentityId(SessionFactoryScope scope) {
		final var entity = new IdentityTemporalEntity();
		entity.text = "hello";
		scope.getSessionFactory().inTransaction( session -> session.persist( entity ) );
		assertNotNull( entity.id );

		awaitTimestampTick();
		final var instant = Instant.now();
		awaitTimestampTick();
		scope.getSessionFactory().inTransaction(
				session -> session.find( IdentityTemporalEntity.class, entity.id ).text = "goodbye"
		);

		scope.getSessionFactory().inTransaction(
				session -> assertEquals( "goodbye", session.find( IdentityTemporalEntity.class, entity.id ).text )
		);
		try (var session = scope.getSessionFactory().withOptions().asOf( instant ).open()) {
			session.inTransaction(
					tx -> assertEquals( "hello", session.find( IdentityTemporalEntity.class, entity.id ).text )
			);
		}
	}
}
