package org.hibernate.temporal.audit;

import java.time.Instant;

import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Changelog;
import org.hibernate.audit.AuditLogFactory;

import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditIdentityIdTest.IdentityEntity.class,
		AuditIdentityIdTest.IdentityRevisionInfo.class
})
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@JiraKey("HHH-21060")
class AuditIdentityIdTest {

	@Changelog
	@Entity(name = "IdentityRevisionInfo")
	@Table(name = "IDENTITY_REVINFO")
	static class IdentityRevisionInfo {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Changelog.ChangesetId
		@Column(name = "REV")
		int id;

		@Changelog.Timestamp
		@Column(name = "REVTSTMP")
		Instant timestamp = Instant.now();
	}

	@Audited
	@Entity(name = "IdentityEntity")
	static class IdentityEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Long id;
		String name;
	}

	@Test
	void testPersistWithIdentityId(SessionFactoryScope scope) {
		final var entity = new IdentityEntity();
		entity.name = "original";
		scope.getSessionFactory().inTransaction( session -> session.persist( entity ) );
		assertNotNull( entity.id );

		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			final var changesets = auditLog.getChangesets( IdentityEntity.class, entity.id );
			assertEquals( 1, changesets.size() );
			try (var s = scope.getSessionFactory().withOptions().atChangeset( changesets.get( 0 ) ).open()) {
				assertEquals( "original", s.find( IdentityEntity.class, entity.id ).name );
			}
		}
	}
}
