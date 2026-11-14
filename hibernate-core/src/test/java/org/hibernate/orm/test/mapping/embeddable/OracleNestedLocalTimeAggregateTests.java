package org.hibernate.orm.test.mapping.embeddable;

import java.time.LocalTime;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.orm.test.mapping.embeddable.OracleLocalTimeAggregateTestSupport.Clock;
import org.hibernate.orm.test.mapping.embeddable.OracleLocalTimeAggregateTestSupport.TimeValue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.SQLXML;

/// Unlike the ordinary embedded Clock fixture, each clock here is itself an
/// aggregate column nested in another aggregate. Raw paths include that node.
///
/// @author Steve Ebersole
@RequiresDialect(OracleDialect.class)
class OracleNestedLocalTimeAggregateTests {
	@Test
	@DomainModel(annotatedClasses = NestedJsonClock.class)
	@SessionFactory
	void nestedJson(SessionFactoryScope scope) {
		final var row = new NestedJsonClock();
		row.id = 1;
		row.payload = new JsonData();
		row.payload.clock = clock();
		verify( scope, row, "NestedJsonClock", false );
	}

	@Test
	@DomainModel(annotatedClasses = NestedXmlClock.class)
	@SessionFactory
	void nestedXml(SessionFactoryScope scope) {
		final var row = new NestedXmlClock();
		row.id = 1;
		row.payload = new XmlData();
		row.payload.clock = clock();
		verify( scope, row, "NestedXmlClock", true );
	}

	@Test
	@DomainModel(annotatedClasses = NestedXmlClock.class)
	@SessionFactory
	void clearedNestedXmlProjectsNull(SessionFactoryScope scope) {
		final var row = new NestedXmlClock();
		row.id = 1;
		row.payload = new XmlData();
		row.payload.clock = clock();
		try {
			scope.inTransaction( session -> session.persist( row ) );
			scope.inTransaction( session -> assertThat( session.createMutationQuery(
					"update NestedXmlClock e set e.payload.clock=null where e.id=1" ).executeUpdate() ).isEqualTo( 1 ) );
			check( scope, "NestedXmlClock", true, null, null );
		}
		finally {
			scope.dropData();
		}
	}

	@Test
	@DomainModel(annotatedClasses = NestedXmlClock.class)
	@SessionFactory
	void allNullXmlChildRetainsExistingAssembly(SessionFactoryScope scope) {
		final var row = new NestedXmlClock();
		row.id = 1;
		row.payload = new XmlData();
		row.payload.clock = new Clock();
		try {
			scope.inTransaction( session -> session.persist( row ) );
			// An all-null child is distinct from absent child content. Keep the existing
			// entity/projection assembly outside the SQL null-extraction repair.
			scope.inTransaction( session -> {
				final var loaded = session.find( NestedXmlClock.class, 1 );
				assertThat( loaded.payload.clock ).isNotNull();
				assertThat( loaded.payload.clock.time ).isNull();
				assertThat( loaded.payload.clock.converted ).isNull();
			} );
			scope.inTransaction( session -> {
				final var projected = session.createSelectionQuery(
						"select e.payload.clock from NestedXmlClock e", Clock.class ).getSingleResult();
				assertThat( projected ).isNotNull();
				assertThat( projected.time ).isNull();
				assertThat( projected.converted ).isNull();
			} );
			scope.inTransaction( session -> {
				assertThat( session.createSelectionQuery(
						"select e.id from NestedXmlClock e where e.payload.clock is null", Integer.class )
						.getResultList() ).isEmpty();
				assertThat( session.createSelectionQuery(
						"select e.id from NestedXmlClock e where e.payload.clock is not null", Integer.class )
						.getResultList() ).containsExactly( 1 );
			} );
		}
		finally {
			scope.dropData();
		}
	}

	private static Clock clock() {
		final var clock = new Clock();
		clock.time = LocalTime.of( 12, 34, 56, 123456789 );
		clock.converted = new TimeValue( LocalTime.of( 23, 59, 59, 987654321 ) );
		return clock;
	}

	private static void verify(SessionFactoryScope scope, Object row, String entity, boolean xml) {
		try {
			scope.inTransaction( session -> session.persist( row ) );
			check( scope, entity, xml, clock().time, clock().converted );
			final var next = LocalTime.of( 0, 0, 0, 123456789 );
			final var converted = new TimeValue( LocalTime.of( 1, 2, 3, 456789123 ) );
			scope.inTransaction( session -> assertThat( session.createMutationQuery(
					"update " + entity + " e set e.payload.clock.time=:time,e.payload.clock.converted=:converted where e.id=1" )
					.setParameter( "time", next ).setParameter( "converted", converted ).executeUpdate() ).isEqualTo( 1 ) );
			check( scope, entity, xml, next, converted );
			if ( !xml ) {
				scope.inTransaction( session -> session.createMutationQuery(
						"update " + entity + " e set e.payload.clock=null where e.id=1" ).executeUpdate() );
				check( scope, entity, false, null, null );
			}
		}
		finally {
			scope.dropData();
		}
	}

	private static void check(SessionFactoryScope scope, String entity, boolean xml, LocalTime time, TimeValue converted) {
		scope.inTransaction( session -> {
			final var clock = session.createSelectionQuery( "select e.payload.clock from " + entity + " e", Clock.class ).getSingleResult();
			if ( time == null ) assertThat( clock ).isNull();
			else {
				assertThat( clock.time ).isEqualTo( time );
				assertThat( clock.converted ).isEqualTo( converted );
			}
			assertThat( session.createSelectionQuery( "select e.payload.clock.time,e.payload.clock.converted from " + entity + " e", Object[].class )
					.getSingleResult() ).containsExactly( time, converted );
			if ( xml || scope.getSessionFactory().getJdbcServices().getDialect().getVersion().isBefore( 23 ) ) {
				session.doWork( connection -> {
					final var expression = xml
							? "xmlcast(xmlquery('/e/clock/clock_time/text()' passing payload returning content) as varchar2(35))"
							: "json_value(payload,'$.clock.clock_time')";
					try ( var statement = connection.createStatement(); var rows = statement.executeQuery(
							"select " + expression + " from " + entity + " where id=1" ) ) {
						assertThat( rows.next() ).isTrue();
						final var text = rows.getString( 1 );
						assertThat( text == null ? null : LocalTime.parse( text ) ).isEqualTo( time );
					}
				} );
			}
			if ( time != null ) assertThat( session.createSelectionQuery( "select e.id from " + entity
					+ " e where e.payload.clock.time=:time and e.payload.clock.converted=:converted", Integer.class )
					.setParameter( "time", time ).setParameter( "converted", converted ).getResultList() ).containsExactly( 1 );
		} );
	}

	@Entity(name = "NestedJsonClock")
	static class NestedJsonClock {
		@Id int id;
		@JdbcTypeCode(JSON) JsonData payload;
	}
	@Embeddable
	static class JsonData { @JdbcTypeCode(JSON) Clock clock; }
	@Entity(name = "NestedXmlClock")
	static class NestedXmlClock {
		@Id int id;
		@JdbcTypeCode(SQLXML) XmlData payload;
	}
	@Embeddable
	static class XmlData { @JdbcTypeCode(SQLXML) Clock clock; }
}
