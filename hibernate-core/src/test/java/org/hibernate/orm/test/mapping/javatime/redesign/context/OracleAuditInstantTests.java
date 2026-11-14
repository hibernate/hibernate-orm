package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.Session;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.JdbcTypeRegistration;
import org.hibernate.annotations.JavaTypeRegistration;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.temporaltype.spi.StandardTemporalPreservation;
import org.hibernate.dialect.temporaltype.spi.TemporalPreservation;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.junit.jupiter.api.Test;
import org.hibernate.type.descriptor.jdbc.TemporalJdbcType;
import org.hibernate.mapping.BasicValue;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsOffsetDateTimeJdbcType;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Audit keys retain UTC clock fields across timezone changes, including existing
/// rows with independently established UTC encoding. This does not certify an
/// arbitrary application's historical encoding or rewrite its data.
///
/// @author Steve Ebersole
@RequiresDialect(OracleDialect.class)
class OracleAuditInstantTests {
	static final Instant FIRST = Instant.parse( "2024-01-15T12:00:00.123456Z" );
	static final Instant SECOND = Instant.parse( "2024-07-15T12:00:00.123456Z" );
	static final Instant THIRD = SECOND.plusSeconds( 86400 );

	static Stream<Arguments> modes() {
		return Stream.of( true, false ).flatMap( preservation -> Stream.of( true, false )
				.flatMap( metadata -> Stream.of( "DEFAULT", "VALIDITY" )
						.map( strategy -> Arguments.of( preservation, metadata, strategy ) ) ) );
	}

	@ParameterizedTest
	@MethodSource("modes")
	void utcKeysAndHistoricalBoundaries(boolean preservation, boolean metadata, String strategy) throws Exception {
		var supplier = new TemporalPeriodFixture.Fixed();
		try ( var scope = ContextScope.open( metadata, false, Map.of(
				"hibernate.test.temporal.reuse_connection", true,
				"hibernate.type.temporal_preservation", preservation,
				"hibernate.temporal.changeset_id_supplier", supplier,
				"hibernate.audit.strategy", strategy ), List.of(), Row.class, ApplicationInstant.class ) ) {
			var model = scope.metadata.getEntityBinding( Row.class.getName() );
			var revision = model.getAuxiliaryColumn( "changesetId" );
			assertThat( ((BasicValue) revision.getValue()).resolve().getJdbcType() )
					.isSameAs( TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE );
			assertThat( revision.getSqlType( scope.metadata ) ).isEqualToIgnoringCase( "timestamp(9)" );
			var application = scope.metadata.getEntityBinding( ApplicationInstant.class.getName() );
			var applicationType = ((BasicValue) application.getProperty( "value" ).getValue()).resolve().getJdbcType();
			assertThat( applicationType ).isNotSameAs( TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE );

			supplier.value = FIRST;
			scope.tx( session -> { zone( session, "America/Denver" ); var row = new Row(); row.id = 1; session.persist( row ); } );
			supplier.value = SECOND;
			scope.tx( session -> { zone( session, "Asia/Tokyo" ); var row = session.find( Row.class, 1 ); row.payload = "second"; row.tags.add( "two" ); } );
			try ( var connection = scope.provider.getConnection(); var statement = connection.createStatement();
					var rows = statement.executeQuery( "select rev from oracle_audit_instant_AUD where id=1 order by rev" ) ) {
				assertThat( rows.getMetaData().getColumnTypeName( 1 ).toLowerCase() ).doesNotContain( "time zone" );
				assertThat( rows.next() ).isTrue();
				assertThat( rows.getObject( 1, LocalDateTime.class ) ).isEqualTo( LocalDateTime.ofInstant( FIRST, ZoneOffset.UTC ) );
				assertThat( rows.next() ).isTrue();
				assertThat( rows.getObject( 1, LocalDateTime.class ) ).isEqualTo( LocalDateTime.ofInstant( SECOND, ZoneOffset.UTC ) );
				assertThat( rows.next() ).isFalse();
			}
			for ( String zone : List.of( "UTC", "America/Denver", "Asia/Tokyo" ) ) {
				at( scope, zone, FIRST.minusNanos( 1 ), null, List.of() );
				at( scope, zone, FIRST, "first", List.of( "one" ) );
				at( scope, zone, FIRST.plusNanos( 1 ), "first", List.of( "one" ) );
				at( scope, zone, SECOND.minusNanos( 1 ), "first", List.of( "one" ) );
				at( scope, zone, SECOND, "second", List.of( "one", "two" ) );
				scope.tx( session -> {
					zone( session, zone );
					try ( var audit = AuditLogFactory.create( session ) ) {
						assertThat( audit.getChangesets( Row.class, 1 ) ).containsExactly( FIRST, SECOND );
					}
				} );
			}
			// Seed a separate source row using SQL UTC fields, independently of the
			// repaired binder. Reading it must not modify or reinterpret the fields.
			scope.tx( session -> session.doWork( connection -> {
				try ( var statement = connection.createStatement() ) {
					statement.executeUpdate( "insert into oracle_audit_instant_AUD (id,payload,rev,revtype) values (2,'seeded',timestamp '2024-01-15 12:00:00.123456',0)" );
				}
			} ) );
			try ( var session = scope.factory.withOptions().atChangeset( FIRST ).open() ) {
				session.beginTransaction(); zone( session, "America/Denver" );
				assertThat( session.find( Row.class, 2 ).payload ).isEqualTo( "seeded" );
				session.getTransaction().commit();
			}
			supplier.value = THIRD;
			scope.tx( session -> { zone( session, "America/Denver" ); session.remove( session.find( Row.class, 1 ) ); } );
			at( scope, "Asia/Tokyo", THIRD.minusNanos( 1 ), "second", List.of( "one", "two" ) );
			at( scope, "Asia/Tokyo", THIRD, null, List.of() );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void customUtcDescriptorRemainsAuthoritative(boolean preservation) {
		try ( var scope = ContextScope.boot( false, false, Map.of(
				"hibernate.dialect", OracleDialect.class,
				"jakarta.persistence.database-product-version", "23.26",
				"hibernate.type.temporal_preservation", preservation ), CustomRow.class ) ) {
			var column = scope.metadata.getEntityBinding( CustomRow.class.getName() ).getAuxiliaryColumn( "changesetId" );
			assertThat( ((BasicValue) column.getValue()).resolve().getJdbcType() ).isInstanceOf( CustomUtc.class );
			assertThat( scope.provider.attempts ).isZero();
		}
	}

	@Test
	void customJavaTypeRetainsItsRecommendation() {
		try ( var scope = ContextScope.boot( false, false, Map.of(
				"hibernate.dialect", OracleDialect.class,
				"jakarta.persistence.database-product-version", "23.26",
				"hibernate.type.temporal_preservation", false ), CustomJavaRow.class ) ) {
			var value = (BasicValue) scope.metadata.getEntityBinding( CustomJavaRow.class.getName() )
					.getAuxiliaryColumn( "changesetId" ).getValue();
			assertThat( value.resolve().getDomainJavaType() ).isInstanceOf( CustomInstant.class );
			assertThat( value.resolve().getJdbcType() ).isNotSameAs( TimestampUtcAsPlainJdbcTimestampJdbcType.INSTANCE );
			assertThat( scope.provider.attempts ).isZero();
		}
	}

	public static class CustomInstant extends InstantJavaType {}
	@Entity(name = "OracleCustomJavaAuditInstant") @Audited
	@JavaTypeRegistration(javaType = Instant.class, descriptorClass = CustomInstant.class)
	static class CustomJavaRow { @Id int id; }

	public static class CustomUtc extends TimestampUtcAsOffsetDateTimeJdbcType implements TemporalJdbcType {
		@Override
		public Optional<TemporalPreservation> getTemporalPreservation(JavaType<?> javaType, Dialect dialect) {
			return Optional.of( StandardTemporalPreservation.instantOnly( javaType ) );
		}
	}
	@Entity(name = "OracleCustomAuditInstant") @Audited
	@JdbcTypeRegistration(value = CustomUtc.class, registrationCode = SqlTypes.TIMESTAMP_UTC)
	static class CustomRow { @Id int id; }

	private static void at(ContextScope scope, String zone, Instant point, String payload, List<String> tags) {
		try ( var session = scope.factory.withOptions().atChangeset( point ).open() ) {
			session.beginTransaction(); zone( session, zone );
			var row = session.find( Row.class, 1 );
			if ( payload == null ) assertThat( row ).isNull();
			else { assertThat( row.payload ).isEqualTo( payload ); assertThat( row.tags ).containsExactlyInAnyOrderElementsOf( tags ); }
			session.getTransaction().commit();
		}
	}

	private static void zone(Session session, String zone) {
		session.doWork( connection -> { try ( var statement = connection.createStatement() ) {
			statement.execute( "alter session set time_zone='" + zone + "'" );
		} } );
	}

	@Entity(name = "OracleAuditInstant") @Table(name = "oracle_audit_instant") @Audited
	static class Row {
		@Id int id;
		String payload = "first";
		@ElementCollection List<String> tags = new ArrayList<>( List.of( "one" ) );
	}
	@Entity(name = "OracleAuditApplicationInstant")
	static class ApplicationInstant { @Id int id; Instant value; }
}
