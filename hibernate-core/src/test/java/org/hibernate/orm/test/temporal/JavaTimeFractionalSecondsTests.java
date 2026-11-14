package org.hibernate.orm.test.temporal;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import org.hibernate.annotations.FractionalSeconds;
import org.hibernate.orm.test.mapping.javatime.redesign.TemporalEligibilityChecks;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.community.dialect.FirebirdDialect;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.DateTimeUtils;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
// The legacy offset/zoned matrix is retained in LegacyJavaTimeFractionalSecondsTests.
public class JavaTimeFractionalSecondsTests {
	@Test
	@DomainModel(annotatedClasses = {TestEntity.class, TestEntity0.class, TestEntity3.class, TestEntity9.class} )
	@RequiresDialectFeature(feature = TemporalEligibilityChecks.ExplicitLocalFields.class)
	void testMapping(DomainModelScope scope) {
		final MetadataImplementor domainModel = scope.getDomainModel();

		final Dialect dialect = domainModel.getDatabase().getDialect();
		final int defaultPrecision = dialect.getTypeSizingProfile().defaultTimestampPrecision();

		final PersistentClass entityBinding = scope.getEntityBinding( TestEntity.class );
		checkPrecision( "theInstant", defaultPrecision, entityBinding, domainModel );
		checkPrecision( "theLocalDateTime", defaultPrecision, entityBinding, domainModel );
		// SQL Server retains an unspecified TIME precision (server default), distinct from explicit zero.
		checkPrecision( "theLocalTime", dialect instanceof SQLServerDialect ? null : 0, entityBinding, domainModel );

		final PersistentClass entityBinding0 = scope.getEntityBinding( TestEntity0.class );
		checkPrecision( "theInstant", 0, entityBinding0, domainModel );

		final PersistentClass entityBinding3 = scope.getEntityBinding( TestEntity3.class );
		checkPrecision( "theInstant", 3, entityBinding3, domainModel );
		checkPrecision( "theLocalDateTime", 3, entityBinding3, domainModel );
		checkPrecision( "theLocalTime", 3, entityBinding3, domainModel );

		final PersistentClass entityBinding9 = scope.getEntityBinding( TestEntity9.class );
		checkPrecision( "theInstant", 9, entityBinding9, domainModel );
	}

	@Test
	@DomainModel(annotatedClasses = { InstantEntity.class, InstantEntity3.class })
	void instantMappings(DomainModelScope scope) {
		final var metadata = scope.getDomainModel();
		checkPrecision( "theInstant", metadata.getDatabase().getDialect().getTypeSizingProfile().defaultTimestampPrecision(),
				scope.getEntityBinding( InstantEntity.class ), metadata );
		checkPrecision( "theInstant", 3, scope.getEntityBinding( InstantEntity3.class ), metadata );
	}

	@Test
	@DomainModel(annotatedClasses = InstantEntity3.class)
	@SessionFactory
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTimestampPrecision3.class)
	void instantPrecisionThree(SessionFactoryScope scope) {
		final var start = Instant.parse( "2024-02-29T12:34:56.123456789Z" );
		final var dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		scope.inTransaction( session -> {
			final var row = new InstantEntity3(); row.id = 1; row.theInstant = start;
			session.persist( row );
		} );
		scope.inTransaction( session -> assertThat( session.find( InstantEntity3.class, 1 ).theInstant )
				.isEqualTo( DateTimeUtils.adjustToPrecision( start, 3, dialect ) ) );
	}

	private void checkPrecision(
			String propertyName,
			Integer expectedMinimumSize,
			PersistentClass entityBinding,
			MetadataImplementor domainModel) {
		final Property theInstant = entityBinding.getProperty( propertyName );
		final BasicValue value = (BasicValue) theInstant.getValue();
		final Column column = (Column) value.getColumn();
		final Size columnSize = column.getColumnSize( value.getDialect(), domainModel );
		assertThat( columnSize.getPrecision() ).isEqualTo( expectedMinimumSize );
	}

	@Test
	@DomainModel(annotatedClasses = InstantEntity.class)
	@SessionFactory
	@SkipForDialect(dialectClass = SybaseDialect.class, reason = "Because... Sybase...", matchSubTypes = true)
	void testUsage(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final Instant start;
		if ( dialect.getTypeSizingProfile().defaultTimestampPrecision() == 6 ) {
			start = Instant.now().truncatedTo( ChronoUnit.MICROS );
		}
		else {
			start = Instant.now();
		}

		scope.inTransaction( (session) -> {
			final InstantEntity testEntity = new InstantEntity();
			testEntity.id = 1;
			testEntity.theInstant = start;
			session.persist( testEntity );
		} );

		scope.inTransaction( (session) -> {
			final InstantEntity testEntity = session.find( InstantEntity.class, 1 );
			assertThat( testEntity.theInstant ).isEqualTo( DateTimeUtils.adjustToDefaultPrecision( start, dialect ) );
		} );
	}

	@Test
	@DomainModel(annotatedClasses = TestEntity0.class)
	@SessionFactory
	@SkipForDialect(dialectClass = DerbyDialect.class, reason = "Derby does not support sized timestamp")
	@SkipForDialect(dialectClass = HANADialect.class, reason = "HANA does not support specifying a precision on timestamps")
	@SkipForDialect(dialectClass = SybaseDialect.class, reason = "Because... Sybase...", matchSubTypes = true)
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Altibase does not support specifying a precision on timestamps")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "Informix only supports precision from 1 to 5")
	@SkipForDialect(dialectClass = FirebirdDialect.class, reason = "Firebird does not support specifying a precision on timestamps")
	@SkipForDialect(dialectClass = SpannerPostgreSQLDialect.class, reason = "Spanner doesn't support specifying precision on timestamps")
	@SkipForDialect(dialectClass = SpannerDialect.class, reason = "Spanner doesn't support specifying precision on timestamps")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTimestampPrecision0.class)
	void testUsage0(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final Instant start;
		if ( dialect.getTypeSizingProfile().defaultTimestampPrecision() == 6 ) {
			start = Instant.now().truncatedTo( ChronoUnit.MICROS );
		}
		else {
			start = Instant.now();
		}

		scope.inTransaction( (session) -> {
			final TestEntity0 testEntity = new TestEntity0();
			testEntity.id = 1;
			testEntity.theInstant = start;
			session.persist( testEntity );
		} );

		scope.inTransaction( (session) -> {
			final TestEntity0 testEntity = session.find( TestEntity0.class, 1 );
			assertThat( testEntity.theInstant ).isEqualTo( DateTimeUtils.adjustToPrecision( start, 0, dialect ) );
		} );
	}

	@Test
	@DomainModel(annotatedClasses = TestEntity3.class)
	@SessionFactory
	@SkipForDialect(dialectClass = DerbyDialect.class, reason = "Derby does not support sized timestamp")
	@SkipForDialect(dialectClass = HANADialect.class, reason = "HANA does not support specifying a precision on timestamps")
	@SkipForDialect(dialectClass = SybaseDialect.class, reason = "Because... Sybase...", matchSubTypes = true)
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Altibase does not support specifying a precision on timestamps")
	@SkipForDialect(dialectClass = FirebirdDialect.class, reason = "Firebird does not support specifying a precision on timestamps")
	@SkipForDialect(dialectClass = SpannerPostgreSQLDialect.class, reason = "Spanner doesn't support specifying precision on timestamps")
	@SkipForDialect(dialectClass = SpannerDialect.class, reason = "Spanner doesn't support specifying precision on timestamps")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTimestampPrecision3.class)
	@RequiresDialectFeature(feature = TemporalEligibilityChecks.ExplicitLocalFields.class)
	void testUsage3(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final Instant start;
		if ( dialect.getTypeSizingProfile().defaultTimestampPrecision() == 6 ) {
			start = Instant.now().truncatedTo( ChronoUnit.MICROS );
		}
		else {
			start = Instant.now();
		}

		final var local = LocalDateTime.of( 2024, 2, 29, 12, 34, 56, 123456789 );
		scope.inTransaction( (session) -> {
			final TestEntity3 testEntity = new TestEntity3();
			testEntity.id = 1;
			testEntity.theInstant = start;
			testEntity.theLocalDateTime = local;
			testEntity.theLocalTime = local.toLocalTime();
			session.persist( testEntity );
		} );

		scope.inTransaction( (session) -> {
			final TestEntity3 testEntity = session.find( TestEntity3.class, 1 );
			assertThat( testEntity.theInstant ).isEqualTo( DateTimeUtils.adjustToPrecision( start, 3, dialect ) );
			assertThat( testEntity.theLocalDateTime ).isEqualTo( DateTimeUtils.adjustToPrecision( local, 3, dialect ) );
			assertThat( testEntity.theLocalTime ).isEqualTo( DateTimeUtils.adjustToPrecision( local.toLocalTime(), 3, dialect ) );
		} );
	}

	@Test
	@DomainModel(annotatedClasses = TestEntity9.class)
	@SessionFactory
	@SkipForDialect(dialectClass = MariaDBDialect.class, reason = "MariaDB only supports precision <= 6")
	@SkipForDialect(dialectClass = MySQLDialect.class, reason = "MySQL only supports precision <= 6", matchSubTypes = true)
	@SkipForDialect(dialectClass = SQLServerDialect.class, reason = "SQL Server only supports precision <= 6")
	@SkipForDialect(dialectClass = SybaseDialect.class, reason = "Because... Sybase...", matchSubTypes = true)
	@SkipForDialect(dialectClass = PostgreSQLDialect.class, reason = "PostgreSQL only supports precision <= 6")
	@SkipForDialect(dialectClass = PostgresPlusDialect.class, reason = "PostgresPlus only supports precision <= 6")
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "CockroachDB only supports precision <= 6")
	@SkipForDialect(dialectClass = HANADialect.class, reason = "HANA does not support specifying a precision on timestamps")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "Informix only supports precision from 1 to 5")
	@SkipForDialect(dialectClass = FirebirdDialect.class, reason = "Firebird does not support specifying a precision on timestamps")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsMicroSecondPrecision.class)
	void testUsage9(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final Instant start;
		if ( dialect.getTypeSizingProfile().defaultTimestampPrecision() == 6 ) {
			start = Instant.now().truncatedTo( ChronoUnit.MICROS );
		}
		else {
			start = Instant.now();
		}

		scope.inTransaction( (session) -> {
			final TestEntity9 testEntity = new TestEntity9();
			testEntity.id = 1;
			testEntity.theInstant = start;
			session.persist( testEntity );
		} );

		scope.inTransaction( (session) -> {
			final TestEntity9 testEntity = session.find( TestEntity9.class, 1 );

			assertThat( testEntity.theInstant ).isEqualTo( start );
		} );
	}

	@Entity(name = "InstantEntity")
	public static class InstantEntity {
		@Id Integer id;
		Instant theInstant;
	}

	@Entity(name = "InstantEntity3")
	public static class InstantEntity3 {
		@Id Integer id;
		@FractionalSeconds(3) Instant theInstant;
	}

	@Entity(name="TestEntity")
	@Table(name="TestEntity")
	public static class TestEntity {
		@Id
		private Integer id;

		private Instant theInstant;

		@JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
		private LocalDateTime theLocalDateTime;

		@JdbcTypeCode(SqlTypes.LOCAL_TIME)
		private LocalTime theLocalTime;

	}

	@Entity(name="TestEntity0")
	@Table(name="TestEntity0")
	public static class TestEntity0 {
		@Id
		private Integer id;

		@FractionalSeconds(0)
		private Instant theInstant;

	}

	@Entity(name="TestEntity3")
	@Table(name="TestEntity3")
	public static class TestEntity3 {
		@Id
		private Integer id;

		@FractionalSeconds(3)
		private Instant theInstant;

		@JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
		@FractionalSeconds(3)
		private LocalDateTime theLocalDateTime;

		@JdbcTypeCode(SqlTypes.LOCAL_TIME)
		@FractionalSeconds(3)
		private LocalTime theLocalTime;

	}

	@Entity(name="TestEntity9")
	@Table(name="TestEntity9")
	public static class TestEntity9 {
		@Id
		private Integer id;

		@FractionalSeconds(9)
		private Instant theInstant;

	}
}
