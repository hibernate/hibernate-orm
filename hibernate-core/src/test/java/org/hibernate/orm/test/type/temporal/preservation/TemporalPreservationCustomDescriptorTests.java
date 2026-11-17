package org.hibernate.orm.test.type.temporal.preservation;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.TemporalPreservationException;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.TemporalPreservationPolicy;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.type.spi.TemporalPreservation;
import org.hibernate.dialect.type.spi.TemporalPreservation.InstantBasis;
import org.hibernate.dialect.type.spi.TemporalPreservation.Level;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.LocalDateJavaType;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.LocalDateJdbcType;
import org.hibernate.type.descriptor.jdbc.OffsetDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.TemporalJdbcType;

import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Tests how a JDBC type which the Dialect knows nothing of comes to be
/// accepted with temporal preservation enabled: by reporting for itself
/// through [TemporalJdbcType], or by a Dialect which reports on it.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalPreservationCustomDescriptorTests {
	private static final Map<String, Object> ENABLED = Map.of( MappingSettings.TEMPORAL_PRESERVATION, true );
	private static final Map<String, Object> DISABLED = Map.of( MappingSettings.TEMPORAL_PRESERVATION, false );

	private static final LocalDate FIRST = LocalDate.of( 2000, 1, 1 );
	private static final LocalDate LAST = LocalDate.of( 2099, 12, 31 );

	/// A descriptor is judged by its exact class, so one which merely
	/// extends an assessed descriptor is not assessed.
	@Test
	void descriptorWhichSaysNothingIsRejected() {
		assertRejected( ENABLED, WithSilentDate.class, "has not been established to preserve the DATE" );
		inFactory( DISABLED, WithSilentDate.class, factory -> {
		} );
	}

	@Test
	void descriptorWhichReportsIsAccepted() {
		inFactory( ENABLED, WithReportingDate.class, factory -> {
			final var entity = new WithReportingDate();
			entity.id = 1;
			entity.theValue = LocalDate.of( 2024, 7, 1 );
			factory.inTransaction( session -> session.persist( entity ) );
			factory.inTransaction( session -> assertThat( session.find( WithReportingDate.class, 1 ).theValue )
					.isEqualTo( entity.theValue ) );
		} );
	}

	/// Values are held to the range which the descriptor reports.
	@Test
	void reportedRangeIsEnforced() {
		inFactory( ENABLED, WithReportingDate.class, factory -> {
			final var entity = new WithReportingDate();
			entity.id = 1;
			entity.theValue = LAST.plusDays( 1 );
			assertThatThrownBy( () -> factory.inTransaction( session -> session.persist( entity ) ) )
					.isInstanceOf( TemporalPreservationException.class )
					.hasMessageContaining( "ReportingDate" )
					.hasMessageContaining( FIRST.toString() );
		} );
	}

	@Test
	void descriptorWhichDeclinesOrReportsLossIsRejected() {
		assertRejected( ENABLED, WithDecliningDate.class, "has not been established to preserve the DATE" );
		assertRejected( ENABLED, WithLosingDate.class, "is known to lose the DATE" );
	}

	@Test
	void faultyReportIsRejected() {
		assertThatThrownBy( () -> inFactory( ENABLED, WithNullDate.class, factory -> {
		} ) )
				.isInstanceOf( MappingException.class )
				.hasMessageContaining( "TemporalJdbcType" )
				.hasMessageContaining( "returned null" );
		assertThatThrownBy( () -> inFactory( ENABLED, WithFailingDate.class, factory -> {
		} ) )
				.isInstanceOf( MappingException.class )
				.hasMessageContaining( "TemporalJdbcType" )
				.hasMessageContaining( "failed to report" );
		// with preservation disabled the descriptor is never asked
		inFactory( DISABLED, WithFailingDate.class, factory -> {
		} );
	}

	/// Values are checked as they pass through `BasicBinder` and
	/// `BasicExtractor`, so a descriptor which goes around those would have
	/// its report accepted and its values left unchecked.
	@Test
	void descriptorWhichEscapesTheChecksIsRejected() {
		assertRejected( ENABLED, WithOwnBinder.class, "binder is not a BasicBinder" );
		assertRejected( ENABLED, WithOwnExtract.class, "extractor overrides BasicExtractor.extract()" );
		// a binder borrowed from another descriptor checks values against that descriptor, not this one
		assertRejected( ENABLED, WithBorrowedBinder.class, "binder is that of LocalDateJdbcType" );
		assertRejected( ENABLED, WithBorrowedExtractor.class, "extractor is that of LocalDateJdbcType" );
		inFactory( DISABLED, WithOwnBinder.class, factory -> {
		} );
	}

	/// A value rendered as a literal is held to the reported range before
	/// the formatter of the descriptor is asked, so a formatter of its own
	/// design does not escape it.
	@Test
	void literalsOfADescriptorWithItsOwnFormatterAreChecked() {
		// values given to the criteria API are rendered as literals of the type of what they are compared with
		final Map<String, Object> inline = Map.of(
				MappingSettings.TEMPORAL_PRESERVATION, true,
				QuerySettings.CRITERIA_VALUE_HANDLING_MODE, "inline"
		);
		inFactory( inline, WithOwnLiteral.class, factory -> {
			final var entity = new WithOwnLiteral();
			entity.id = 1;
			entity.theValue = LocalDate.of( 2024, 7, 1 );
			factory.inTransaction( session -> session.persist( entity ) );
			factory.inTransaction( session -> {
				assertThat( countByLiteral( session, entity.theValue ) ).isEqualTo( 1L );
				assertThat( OwnLiteralDate.rendered ).isTrue();
				assertThatThrownBy( () -> countByLiteral( session, LAST.plusDays( 1 ) ) )
						.isInstanceOf( TemporalPreservationException.class )
						.hasMessageContaining( "render as a SQL literal" )
						.hasMessageContaining( FIRST.toString() );
			} );
		} );
	}

	private static Long countByLiteral(Session session, LocalDate value) {
		final var builder = session.getCriteriaBuilder();
		final var query = builder.createQuery( Long.class );
		final var root = query.from( WithOwnLiteral.class );
		query.select( builder.count( root ) ).where( builder.equal( root.get( "theValue" ), value ) );
		return session.createQuery( query ).getSingleResult();
	}

	/// What a report states is weighed against the policy of the mapping.
	@Test
	void reportIsHeldToThePolicy() {
		assertRejected( ENABLED, WithInstantOnly.class, "is known to lose the OFFSET" );
		inFactory( ENABLED, WithNormalizedInstantOnly.class, factory -> {
			final var entity = new WithNormalizedInstantOnly();
			entity.id = 1;
			entity.theValue = OffsetDateTime.of( 2024, 7, 1, 12, 0, 0, 0, ZoneOffset.ofHours( 2 ) );
			factory.inTransaction( session -> session.persist( entity ) );
			factory.inTransaction( session -> assertThat(
					session.find( WithNormalizedInstantOnly.class, 1 ).theValue.toInstant()
			).isEqualTo( entity.theValue.toInstant() ) );
		} );
	}

	/// A Dialect which reports on a combination is not second-guessed.
	@Test
	void dialectReportTakesPrecedence() {
		assertRejected(
				Map.of(
						MappingSettings.TEMPORAL_PRESERVATION, true,
						JdbcSettings.DIALECT, DistrustingDialect.class.getName()
				),
				WithReportingDate.class,
				"is known to lose the DATE"
		);
	}

	/// A Dialect may report on a descriptor which says nothing for itself.
	@Test
	void dialectMayReportOnACustomDescriptor() {
		inFactory(
				Map.of(
						MappingSettings.TEMPORAL_PRESERVATION, true,
						JdbcSettings.DIALECT, VouchingDialect.class.getName()
				),
				WithSilentDate.class,
				factory -> {
					final var entity = new WithSilentDate();
					entity.id = 1;
					entity.theValue = LocalDate.of( 2024, 7, 1 );
					factory.inTransaction( session -> session.persist( entity ) );
					factory.inTransaction( session -> assertThat( session.find( WithSilentDate.class, 1 ).theValue )
							.isEqualTo( entity.theValue ) );
				}
		);
	}

	private static void assertRejected(Map<String, Object> settings, Class<?> entityClass, String reason) {
		assertThatThrownBy( () -> inFactory( settings, entityClass, factory -> {
		} ) )
				.isInstanceOf( MappingException.class )
				.hasMessageContaining( entityClass.getSimpleName() + ".theValue" )
				.hasMessageContaining( reason )
				.hasMessageContaining( MappingSettings.TEMPORAL_PRESERVATION );
	}

	private static void inFactory(
			Map<String, Object> settings,
			Class<?> entityClass,
			Consumer<SessionFactoryImplementor> action) {
		final var registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		registryBuilder.applySetting( "hibernate.hbm2ddl.auto", "create-drop" );
		settings.forEach( registryBuilder::applySetting );
		try ( var registry = registryBuilder.build() ) {
			final var metadata = new MetadataSources( registry ).addAnnotatedClass( entityClass ).buildMetadata();
			try ( var factory = (SessionFactoryImplementor) metadata.buildSessionFactory() ) {
				action.accept( factory );
			}
		}
	}

	public static class SilentDate extends LocalDateJdbcType {
	}

	public static class ReportingDate extends LocalDateJdbcType implements TemporalJdbcType {
		@Override
		public Optional<TemporalPreservation> getTemporalPreservation(JavaType<?> javaType, Dialect dialect) {
			return javaType.getClass() == LocalDateJavaType.class && dialect instanceof H2Dialect
					? Optional.of( TemporalPreservation.ofDate( FIRST, LAST ) )
					: Optional.empty();
		}
	}

	public static class DecliningDate extends LocalDateJdbcType implements TemporalJdbcType {
		@Override
		public Optional<TemporalPreservation> getTemporalPreservation(JavaType<?> javaType, Dialect dialect) {
			return Optional.empty();
		}
	}

	public static class LosingDate extends LocalDateJdbcType implements TemporalJdbcType {
		@Override
		public Optional<TemporalPreservation> getTemporalPreservation(JavaType<?> javaType, Dialect dialect) {
			return Optional.of( new TemporalPreservation(
					Level.UNSUPPORTED, Level.UNKNOWN, Level.UNKNOWN, Level.UNKNOWN, Level.UNKNOWN, null, null, null
			) );
		}
	}

	public static class NullDate extends LocalDateJdbcType implements TemporalJdbcType {
		@Override
		public Optional<TemporalPreservation> getTemporalPreservation(JavaType<?> javaType, Dialect dialect) {
			//noinspection OptionalAssignedToNull
			return null;
		}
	}

	public static class FailingDate extends LocalDateJdbcType implements TemporalJdbcType {
		@Override
		public Optional<TemporalPreservation> getTemporalPreservation(JavaType<?> javaType, Dialect dialect) {
			throw new IllegalStateException( "No answer" );
		}
	}

	/// Keeps the instant of an `OffsetDateTime`, and says that it loses the offset.
	public static class InstantOnly extends OffsetDateTimeJdbcType implements TemporalJdbcType {
		@Override
		public Optional<TemporalPreservation> getTemporalPreservation(JavaType<?> javaType, Dialect dialect) {
			return Optional.of( new TemporalPreservation(
					Level.UNKNOWN,
					Level.UNKNOWN,
					Level.SUPPORTED,
					Level.UNSUPPORTED,
					Level.UNKNOWN,
					InstantBasis.NATIVE,
					Instant.parse( "2000-01-01T00:00:00Z" ),
					Instant.parse( "2099-12-31T23:59:59Z" )
			) );
		}
	}

	/// Reports as [ReportingDate] does, and binds values by its own means.
	public static class OwnBinderDate extends ReportingDate {
		@Override
		public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
			final ValueBinder<X> checked = super.getBinder( javaType );
			return new ValueBinder<>() {
				@Override
				public void bind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
					st.setObject( index, value );
				}

				@Override
				public void bind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
					checked.bind( st, value, name, options );
				}
			};
		}
	}

	/// Reports as [ReportingDate] does, and binds values with the binder of another descriptor.
	public static class BorrowedBinderDate extends ReportingDate {
		@Override
		public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
			return LocalDateJdbcType.INSTANCE.getBinder( javaType );
		}
	}

	/// Reports as [ReportingDate] does, and reads values with the extractor of another descriptor.
	public static class BorrowedExtractorDate extends ReportingDate {
		@Override
		public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
			return LocalDateJdbcType.INSTANCE.getExtractor( javaType );
		}
	}

	/// Reports as [ReportingDate] does, and replaces the entry point of its extractor.
	public static class OwnExtractDate extends ReportingDate {
		@Override
		public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
			return new BasicExtractor<>( javaType, this ) {
				@Override
				public X extract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					return rs.getObject( paramIndex, javaType.getJavaTypeClass() );
				}

				@Override
				protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					return rs.getObject( paramIndex, javaType.getJavaTypeClass() );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
					return statement.getObject( index, javaType.getJavaTypeClass() );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
					return statement.getObject( name, javaType.getJavaTypeClass() );
				}
			};
		}
	}

	/// Reports as [ReportingDate] does, and renders literals by its own means.
	public static class OwnLiteralDate extends ReportingDate {
		static volatile boolean rendered;

		@Override
		public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
			return (appender, value, dialect, options) -> {
				rendered = true;
				appender.appendSql( "date '" + value + "'" );
			};
		}
	}

	public static class DistrustingDialect extends H2Dialect {
		@Override
		public TemporalPreservation getTemporalPreservation(JavaType<?> javaType, org.hibernate.type.descriptor.jdbc.JdbcType jdbcType) {
			return jdbcType.getClass() == ReportingDate.class
					? new TemporalPreservation(
							Level.UNSUPPORTED, Level.UNKNOWN, Level.UNKNOWN, Level.UNKNOWN, Level.UNKNOWN, null, null, null
					)
					: super.getTemporalPreservation( javaType, jdbcType );
		}
	}

	public static class VouchingDialect extends H2Dialect {
		@Override
		public TemporalPreservation getTemporalPreservation(JavaType<?> javaType, org.hibernate.type.descriptor.jdbc.JdbcType jdbcType) {
			return javaType.getClass() == LocalDateJavaType.class && jdbcType.getClass() == SilentDate.class
					? TemporalPreservation.ofDate( FIRST, LAST )
					: super.getTemporalPreservation( javaType, jdbcType );
		}
	}

	@Entity(name = "WithSilentDate")
	static class WithSilentDate {
		@Id
		Integer id;
		@JdbcType(SilentDate.class)
		LocalDate theValue;
	}

	@Entity(name = "WithReportingDate")
	static class WithReportingDate {
		@Id
		Integer id;
		@JdbcType(ReportingDate.class)
		LocalDate theValue;
	}

	@Entity(name = "WithDecliningDate")
	static class WithDecliningDate {
		@Id
		Integer id;
		@JdbcType(DecliningDate.class)
		LocalDate theValue;
	}

	@Entity(name = "WithLosingDate")
	static class WithLosingDate {
		@Id
		Integer id;
		@JdbcType(LosingDate.class)
		LocalDate theValue;
	}

	@Entity(name = "WithNullDate")
	static class WithNullDate {
		@Id
		Integer id;
		@JdbcType(NullDate.class)
		LocalDate theValue;
	}

	@Entity(name = "WithFailingDate")
	static class WithFailingDate {
		@Id
		Integer id;
		@JdbcType(FailingDate.class)
		LocalDate theValue;
	}

	@Entity(name = "WithOwnBinder")
	static class WithOwnBinder {
		@Id
		Integer id;
		@JdbcType(OwnBinderDate.class)
		LocalDate theValue;
	}

	@Entity(name = "WithBorrowedBinder")
	static class WithBorrowedBinder {
		@Id
		Integer id;
		@JdbcType(BorrowedBinderDate.class)
		LocalDate theValue;
	}

	@Entity(name = "WithBorrowedExtractor")
	static class WithBorrowedExtractor {
		@Id
		Integer id;
		@JdbcType(BorrowedExtractorDate.class)
		LocalDate theValue;
	}

	@Entity(name = "WithOwnExtract")
	static class WithOwnExtract {
		@Id
		Integer id;
		@JdbcType(OwnExtractDate.class)
		LocalDate theValue;
	}

	@Entity(name = "WithOwnLiteral")
	static class WithOwnLiteral {
		@Id
		Integer id;
		@JdbcType(OwnLiteralDate.class)
		LocalDate theValue;
	}

	@Entity(name = "WithInstantOnly")
	static class WithInstantOnly {
		@Id
		Integer id;
		@JdbcType(InstantOnly.class)
		OffsetDateTime theValue;
	}

	@Entity(name = "WithNormalizedInstantOnly")
	static class WithNormalizedInstantOnly {
		@Id
		Integer id;
		@JdbcType(InstantOnly.class)
		@TemporalPreservationPolicy(TemporalPreservationPolicy.Type.NORMALIZED)
		OffsetDateTime theValue;
	}
}
