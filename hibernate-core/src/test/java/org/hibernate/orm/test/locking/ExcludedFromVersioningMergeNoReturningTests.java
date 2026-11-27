package org.hibernate.orm.test.locking;

import jakarta.persistence.Entity;
import jakarta.persistence.ExcludedFromVersioning;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.sql.ast.internal.H2SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.OptionalTableUpdateOperationRequest;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.JdbcSettings.DIALECT;

/// Exercise version selection after an upsert when the database supports update
/// returning, but, like DB2, cannot return values from a `merge` statement.
@JiraKey("HHH-21288")
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = @Setting(name = DIALECT,
		value = "org.hibernate.orm.test.locking.ExcludedFromVersioningMergeNoReturningTests$MergeNoReturningDialect"))
public class ExcludedFromVersioningMergeNoReturningTests extends AnnotatedExcludedFromVersioningTests {
	public static class MergeNoReturningDialect extends H2Dialect {
		@Override
		public MutationOperation createOptionalTableUpdateOperation(OptionalTableUpdateOperationRequest request) {
			final var update = request.update();
			final var translationRequest =
					new SqlAstTranslationRequest.ModelMutation<>( request.sessionFactory(), update );
			return new H2SqlAstTranslator<>( translationRequest ) {
				@Override
				protected boolean supportsMergeReturning() {
					return false;
				}
			}.createMergeOperation( update );
		}
	}

	@Test
	@DomainModel(annotatedClasses = Handset.class)
	@SessionFactory(useCollectingStatementObserver = true)
	void upsertSelectsVersionAfterMerge(SessionFactoryScope scope) {
		final var handset = new Handset();
		handset.id = 1L;
		handset.model = "A";
		scope.inStatelessTransaction( session -> session.insert( handset ) );

		handset.model = "B";
		final var observer = scope.getCollectingStatementObserver();
		observer.clear();
		scope.inStatelessTransaction( session -> session.upsert( handset ) );

		observer.assertQueries().hasSize( 2 );
		observer.assertQuery( 0 ).startsWith( "merge into " );
		observer.assertQuery( 1 ).startsWith( "select " );
		assertThat( handset.version ).isOne();
	}

	/// Mapped to a table of its own, which the tests of the superclass do not use.
	@Entity(name = "Handset")
	static class Handset {
		@Id
		Long id;
		String model;
		@ExcludedFromVersioning
		long callCount;
		@Version
		Long version;
	}
}
