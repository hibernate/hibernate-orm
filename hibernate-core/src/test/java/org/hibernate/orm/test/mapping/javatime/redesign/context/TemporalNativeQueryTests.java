package org.hibernate.orm.test.mapping.javatime.redesign.context;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.type.BasicType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

/// Explicit native type handles retain each attribute's adjustment independently.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(OracleDialect.class)
class TemporalNativeQueryTests {
	static final OffsetDateTime INPUT = OffsetDateTime.parse("2026-07-01T12:00:45.123456+02:00:45");

	@ParameterizedTest @ValueSource(booleans={true,false})
	@SuppressWarnings("unchecked")
	void explicitTypesAndClassOnlyControl(boolean metadata) {
		try(var scope=ContextScope.open(metadata,java.util.Map.of("hibernate.test.temporal.reuse_connection",true),Event.class)) {
			scope.tx(s->{var e=new Event();e.id=1;e.east=INPUT;e.west=INPUT;e.instant=INPUT.toInstant();e.rawClock=java.time.OffsetTime.parse("01:00:00.123456+05:30");s.persist(e);var n=new Event();n.id=2;s.persist(n);});
			var entity=scope.factory.getMappingMetamodel().getEntityDescriptor(Event.class);
			for(String field:List.of("east","west","instant","rawClock")) {
				var type=(BasicType<Object>)entity.getPropertyType(field);
				Object input=field.equals("instant")?INPUT.toInstant():field.equals("rawClock")?java.time.OffsetTime.parse("01:00:00.123456+05:30"):INPUT;
				Object expected=switch(field){case "east"->OffsetDateTime.parse("2026-07-01T15:30:00.123456+05:30");case "west"->OffsetDateTime.parse("2026-07-01T06:00:00.123456-04:00");case "rawClock"->java.time.OffsetTime.parse("01:00:00.123456+05:30");default->Instant.parse("2026-07-01T10:00:00.123456Z");};
				scope.tx(s->assertThat(s.createNativeQuery("select "+field+" as result_value from NativePreservationEvent where "+field+"=:p")
					.addScalar("result_value",type).setParameter("p",input,type).getSingleResult()).isEqualTo(expected));
				scope.tx(s->assertThat(s.createNativeMutationQuery("update NativePreservationEvent set "+field+"=:p where id=2")
					.setParameter("p",null,type).executeUpdate()).isEqualTo(1));
				scope.tx(s->assertThat(s.createNativeQuery("select "+field+" as result_value from NativePreservationEvent where id=2")
					.addScalar("result_value",type).getSingleResult()).isNull());
				assertThat(input).isEqualTo(field.equals("instant")?INPUT.toInstant():field.equals("rawClock")?java.time.OffsetTime.parse("01:00:00.123456+05:30"):INPUT);
			}
			// A matching column name does not confer the attribute's region adjustment.
			scope.tx(s->{
				OffsetDateTime raw=s.doReturningWork(c->{try(var st=c.createStatement();var rs=st.executeQuery("select cast(west as timestamp with time zone) as west from NativePreservationEvent where id=1")){rs.next();return rs.getObject(1,OffsetDateTime.class);}});
				assertThat(s.createNativeQuery("select cast(west as timestamp with time zone) as west from NativePreservationEvent where id=1")
					.addScalar("west",OffsetDateTime.class).getSingleResult()).isEqualTo(raw);
			});
		}
	}

	@ParameterizedTest @ValueSource(booleans={true,false})
	@SuppressWarnings("unchecked")
	void matchedInstantFallback(boolean metadata) {
		try(var scope=ContextScope.open(metadata,true,Map.of("hibernate.type.preferred_instant_jdbc_type","TIMESTAMP_UTC","hibernate.test.temporal.reuse_connection",true),List.of(),InstantEvent.class)) {
			var value=Instant.parse("2026-07-01T10:00:00.123456Z");
			scope.tx(s->{var e=new InstantEvent();e.id=1;e.value=value;s.persist(e);});
			var type=(BasicType<Instant>)scope.factory.getMappingMetamodel().getEntityDescriptor(InstantEvent.class).getPropertyType("value");
			assertThat(type.getJdbcType()).isInstanceOf(org.hibernate.type.descriptor.jdbc.TimestampUtcAsPlainJdbcTimestampJdbcType.class);
			scope.tx(s->assertThat(s.createNativeQuery("select instant_value as result_value from NativeInstantEvent where instant_value=:p")
				.addScalar("result_value",type).setParameter("p",value,type).getSingleResult()).isEqualTo(value));
		}
	}
	@Entity(name="NativePreservationEvent") static class Event {
		@Id int id;
		@TimeZoneAdjustment("+05:30") OffsetDateTime east;
		@TimeZoneAdjustment("America/New_York") OffsetDateTime west;
		Instant instant;
		@jakarta.persistence.Column(secondPrecision=6) java.time.OffsetTime rawClock;
	}
	@Entity(name="NativeInstantEvent") static class InstantEvent {
		@Id int id;
		@jakarta.persistence.Column(name="instant_value") Instant value;
	}
}
