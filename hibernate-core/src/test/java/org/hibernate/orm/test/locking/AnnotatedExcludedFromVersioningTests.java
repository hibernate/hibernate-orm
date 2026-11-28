package org.hibernate.orm.test.locking;

import jakarta.persistence.Entity;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Variant of {@link ExcludedFromVersioningTestsBase} where {@link Phone}/{@link Counter} are mapped via
/// the `@jakarta.persistence.ExcludedFromVersioning` annotation already present on their fields.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		ExcludedFromVersioningTestsBase.Phone.class,
		ExcludedFromVersioningTestsBase.Counter.class
})
public class AnnotatedExcludedFromVersioningTests extends ExcludedFromVersioningTestsBase {
	@Test
	@DomainModel(annotatedClasses = { Phone.class, DynamicPhone.class })
	@SessionFactory
	void emulatedUpsertWithExcludedProperty(SessionFactoryScope scope) {
		final var phone = new DynamicPhone();
		phone.setId( 1L );
		phone.setNumber( "123" );
		scope.inStatelessTransaction( session -> session.upsert( phone ) );
		assertThat( phone.getVersion() ).isZero();
		phone.incrementCallCount();
		scope.inStatelessTransaction( session -> session.upsert( phone ) );
		assertThat( phone.getVersion() ).isZero();
		phone.setNumber( "456" );
		scope.inStatelessTransaction( session -> session.upsert( phone ) );
		assertThat( phone.getVersion() ).isOne();
		scope.inSession( session -> {
			final var stored = session.find( DynamicPhone.class, 1L );
			assertThat( stored.getCallCount() ).isOne();
			assertThat( stored.getNumber() ).isEqualTo( "456" );
			assertThat( stored.getVersion() ).isOne();
		} );
	}

	/// Where a dialect cannot return values from a `merge` statement, as on DB2,
	/// the version is selected after the merge.
	@Test
	@JiraKey("HHH-21288")
	void upsertReadsIncrementedVersion(SessionFactoryScope scope) {
		final var phone = new Phone();
		phone.setId( 1L );
		phone.setNumber( "123" );
		scope.inStatelessTransaction( session -> session.insert( phone ) );
		assertThat( phone.getVersion() ).isZero();

		phone.setNumber( "456" );
		final var observer = scope.getCollectingStatementObserver();
		observer.clear();
		scope.inStatelessTransaction( session -> session.upsert( phone ) );
		if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof DB2Dialect ) {
			observer.assertQueries().hasSize( 2 );
			observer.assertQuery( 0 ).startsWith( "merge into " );
			observer.assertQuery( 1 ).startsWith( "select " );
		}
		assertThat( phone.getVersion() ).isOne();
		scope.inSession( session -> {
			final var stored = session.find( Phone.class, 1L );
			assertThat( stored.getNumber() ).isEqualTo( "456" );
			assertThat( stored.getVersion() ).isOne();
		} );
	}

	@Entity(name = "DynamicPhone")
	@DynamicUpdate
	static class DynamicPhone extends Phone {}
}
