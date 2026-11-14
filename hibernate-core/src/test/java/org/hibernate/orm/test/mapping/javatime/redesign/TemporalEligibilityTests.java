package org.hibernate.orm.test.mapping.javatime.redesign;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Provider identity, preserving producers and JDBC mapping support are independent requirements.
///
/// @author Steve Ebersole
class TemporalEligibilityTests {
	@Test
	void liveProfileDoesNotFollowInheritance() {
		final var check = new TemporalEligibilityChecks.LiveProfile();
		assertThat( check.apply( new H2Dialect() ) ).isTrue();
		assertThat( check.apply( new PostgreSQLDialect() ) ).isTrue();
		assertThat( check.apply( new PostgresPlusDialect() ) ).isTrue();
		assertThat( check.apply( new SpannerPostgreSQLDialect() ) ).isFalse();
		assertThat( check.apply( new UnknownPostgreSQLDialect() ) ).isFalse();
	}

	@Test
	void generatedVersionsNeedTheirOwnProducerAssessment() {
		final var local = new TemporalEligibilityChecks.LocalDateTimeProducer();
		assertThat( local.apply( new H2Dialect() ) ).isTrue();
		assertThat( local.apply( new HSQLDialect() ) ).isFalse();
		final var normalized = new TemporalEligibilityChecks.NormalizedOffsetDateTimeProducer();
		assertThat( normalized.apply( new PostgreSQLDialect() ) ).isTrue();
		assertThat( normalized.apply( new PostgresPlusDialect() ) ).isFalse();
	}

	@Test
	void utcCarrierAndInlineAreAssessedSeparately() {
		assertThat( new TemporalEligibilityChecks.InlineOffsetTime().apply( new H2Dialect() ) ).isTrue();
		assertThat( new TemporalEligibilityChecks.UtcOffsetTime().apply( new H2Dialect() ) ).isTrue();
		assertThat( new TemporalEligibilityChecks.InlineOffsetTime().apply( new HSQLDialect() ) ).isFalse();
		assertThat( new TemporalEligibilityChecks.UtcOffsetTime().apply( new HSQLDialect() ) ).isFalse();
	}

	private static class UnknownPostgreSQLDialect extends PostgreSQLDialect {
	}
}
