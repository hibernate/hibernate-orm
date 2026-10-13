package org.hibernate.orm.test.batch;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.cfg.BatchSettings;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies identity insert eligibility separately so unsupported dialects skip before bootstrap.
///
/// @author Steve Ebersole
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@DomainModel(annotatedClasses = BatchingGuideIdentityTests.IdentityPerson.class)
@ServiceRegistry(settings = @Setting(name = BatchSettings.STATEMENT_BATCH_SIZE, value = "2"))
@SessionFactory(useCollectingStatementObserver = true)
class BatchingGuideIdentityTests {
	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void identityInsertsExecuteWithoutBatchAdditions(SessionFactoryScope scope) {
		var observer = scope.getCollectingStatementObserver();
		observer.clear();
		scope.inTransaction( session -> {
			for ( int i = 0; i < 5; i++ ) {
				session.persist( new IdentityPerson() );
			}
		} );
		assertThat( observer.getStatements() ).hasSize( 5 )
				.allSatisfy( statement -> assertThat( statement.batchPosition() ).isEqualTo( -1 ) );
		scope.inTransaction( session -> assertThat( session.createQuery(
				"from BatchGuideIdentity", IdentityPerson.class ).getResultList() ).hasSize( 5 ) );
	}

	@Entity(name = "BatchGuideIdentity")
	@Table(name = "batch_guide_identity")
	public static class IdentityPerson {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Long id;
	}
}
