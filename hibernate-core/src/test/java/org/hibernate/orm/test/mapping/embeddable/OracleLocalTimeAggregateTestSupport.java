package org.hibernate.orm.test.mapping.embeddable;

import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Whole/member paths must agree at the relational LocalTime boundary, including
/// nested values and user conversion. Each load uses a fresh persistence context.
///
/// @author Steve Ebersole
abstract class OracleLocalTimeAggregateTestSupport {
	abstract Class<? extends Holder> holderType();

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void wholeAndMemberOperationsAgree(SessionFactoryScope scope) throws Exception {
		final var row = holderType().getDeclaredConstructor().newInstance();
		row.id = 1;
		final var data = new Data();
		data.clock = new Clock();
		data.clock.time = LocalTime.of( 12, 34, 56, 123456789 );
		data.clock.converted = new TimeValue( LocalTime.of( 23, 59, 59, 987654321 ) );
		data.stamp = LocalDateTime.of( 2026, 7, 1, 12, 34, 56, 123456789 );
		row.data( data );
		scope.inTransaction( session -> session.persist( row ) );
		final String entityName = holderType().getSimpleName();
		assertValues( scope, entityName, data.clock.time, data.clock.converted, data.stamp );
		final var next = LocalTime.of( 0, 0, 0, 123456789 );
		final var converted = new TimeValue( LocalTime.of( 1, 2, 3, 456789123 ) );
		scope.inTransaction( session -> assertThat( session.createMutationQuery(
				"update " + entityName + " e set e.payload.clock.time=:time,e.payload.clock.converted=:converted where e.id=1" )
				.setParameter( "time", next ).setParameter( "converted", converted ).executeUpdate() ).isEqualTo( 1 ) );
		scope.getSessionFactory().getCache().evictAllRegions();
		assertValues( scope, entityName, next, converted, data.stamp );
		scope.inTransaction( session -> assertThat( session.createMutationQuery(
				"update " + entityName + " e set e.payload.clock.time=null,e.payload.clock.converted=null where e.id=1" )
				.executeUpdate() ).isEqualTo( 1 ) );
		assertValues( scope, entityName, null, null, data.stamp );
	}

	private void assertValues(SessionFactoryScope scope, String entityName, LocalTime time, TimeValue converted,
			LocalDateTime stamp) {
		scope.inTransaction( session -> {
			final var loaded = session.find( holderType(), 1 ).data();
			if ( time == null && converted == null ) {
				assertThat( loaded.clock ).isNull();
			}
			else {
				assertThat( loaded.clock.time ).isEqualTo( time );
				assertThat( loaded.clock.converted ).isEqualTo( converted );
			}
			assertThat( loaded.stamp ).isEqualTo( stamp );
			final var tuple = session.createSelectionQuery( "select e.payload.clock.time,e.payload.clock.converted,e.payload.stamp from "
					+ entityName + " e where e.id=1", Object[].class ).getSingleResult();
			assertThat( tuple ).containsExactly( time, converted, stamp );
			final boolean xml = entityName.startsWith( "Xml" );
			if ( xml || scope.getSessionFactory().getJdbcServices().getDialect().getVersion().isBefore( 23 ) ) {
				session.doWork( connection -> {
					final String valueExpression = xml
							? "xmlcast(xmlquery('/e/clock/clock_time/text()' passing payload returning content) as varchar2(35))"
							: "json_value(payload,'$.clock.clock_time')";
					try ( var statement = connection.createStatement();
							var result = statement.executeQuery( "select " + valueExpression + " from " + entityName + " where id=1" ) ) {
						assertThat( result.next() ).isTrue();
						final String text = result.getString( 1 );
						assertThat( text == null ? null : LocalTime.parse( text ) ).isEqualTo( time );
					}
				} );
			}
			if ( time != null ) {
				assertThat( session.createSelectionQuery( "select e.id from " + entityName
						+ " e where e.payload.clock.time=:time and e.payload.clock.converted=:converted", Integer.class )
						.setParameter( "time", time ).setParameter( "converted", converted ).getResultList() ).containsExactly( 1 );
			}
		} );
	}

	@MappedSuperclass
	abstract static class Holder {
		@Id int id;
		abstract Data data();
		abstract void data(Data data);
	}

	@Embeddable
	static class Data {
		@Embedded Clock clock;
		@Column(secondPrecision = 9) LocalDateTime stamp;
	}

	@Embeddable
	static class Clock {
		@Column(name = "clock_time", secondPrecision = 9) LocalTime time;
		@Column(secondPrecision = 9)
		@Convert(converter = TimeConverter.class)
		TimeValue converted;
	}

	record TimeValue(LocalTime time) {
	}

	@Converter
	public static class TimeConverter implements AttributeConverter<TimeValue, LocalTime> {
		@Override
		public LocalTime convertToDatabaseColumn(TimeValue value) {
			return value == null ? null : value.time();
		}
		@Override
		public TimeValue convertToEntityAttribute(LocalTime time) {
			return time == null ? null : new TimeValue( time );
		}
	}
}
