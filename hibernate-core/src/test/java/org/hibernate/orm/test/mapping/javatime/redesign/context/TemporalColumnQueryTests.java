package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.*;
import java.util.List;
import jakarta.persistence.*;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.annotations.TimeZoneColumn;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Query expectations follow independently specified UTC-primary/offset tuples.
/// @author Steve Ebersole
class TemporalColumnQueryTests {
	static final OffsetDateTime FIRST = OffsetDateTime.parse("2026-07-01T12:00:45.123456+02:00:45");
	static final OffsetDateTime SECOND = OffsetDateTime.parse("2026-07-01T11:00:30.123456+01:00:30");
	static final OffsetDateTime THIRD = OffsetDateTime.parse("2026-07-01T10:30:00.123456Z");

	@ParameterizedTest @ValueSource(booleans={true,false})
	void tuplePredicatesAndOrdering(boolean metadata) {
		try (var scope=ContextScope.open(metadata,java.util.Map.of("hibernate.test.temporal.reuse_connection",true),Event.class)) {
			seed(scope);
			for (String field : List.of("offsetStamp","zonedStamp","offsetClock","adjustedStamp","adjustedZoned","adjustedClock")) {
				boolean adjusted=field.startsWith("adjusted");
				Object first=input(field,FIRST), third=input(field,THIRD);
				scope.tx(s->{
					assertThat(s.createSelectionQuery("select e."+field+" from ColumnQueryEvent e where e.id=1",Object.class).getSingleResult())
						.isEqualTo(expected(field,FIRST));
					assertThat(s.createSelectionQuery("select e.id from ColumnQueryEvent e where e."+field+"=:p order by e.id",Integer.class)
						.setParameter("p",first).getResultList()).containsExactlyElementsOf(adjusted?List.of(1,2):List.of(1));
					assertThat(s.createSelectionQuery("select e.id from ColumnQueryEvent e where e."+field+"<>:p order by e.id",Integer.class)
						.setParameter("p",first).getResultList()).containsExactlyElementsOf(adjusted?List.of(3):List.of(2,3));
					assertThat(s.createSelectionQuery("select e.id from ColumnQueryEvent e where e."+field+" is not null order by e."+field+",e.id",Integer.class)
						.getResultList()).containsExactlyElementsOf(adjusted?List.of(1,2,3):List.of(2,1,3));
					assertThat(s.createSelectionQuery("select e.id from ColumnQueryEvent e where e."+field+">:p order by e.id",Integer.class)
						.setParameter("p",first).getResultList()).containsExactly(3);
					assertThat(s.createSelectionQuery("select e.id from ColumnQueryEvent e where e."+field+" between :a and :b order by e.id",Integer.class)
						.setParameter("a",first).setParameter("b",third).getResultList()).containsExactlyElementsOf(adjusted?List.of(1,2,3):List.of(1,3));
					assertThat(s.createSelectionQuery("select e.id from ColumnQueryEvent e where e."+field+" not between :a and :b order by e.id",Integer.class)
						.setParameter("a",first).setParameter("b",third).getResultList()).containsExactlyElementsOf(adjusted?List.of():List.of(2));
					assertThat(s.createSelectionQuery("select e.id from ColumnQueryEvent e where e."+field+" in :values order by e.id",Integer.class)
						.setParameterList("values",List.of(first,third)).getResultList()).containsExactlyElementsOf(adjusted?List.of(1,2,3):List.of(1,3));
					assertThat(s.createSelectionQuery("select e.id from ColumnQueryEvent e where e."+field+" is null",Integer.class).getResultList()).containsExactly(4);
				});
			}
		}
	}

	@ParameterizedTest @ValueSource(booleans={true,false})
	void wholeValueBulkAssignments(boolean metadata) {
		try(var scope=ContextScope.open(metadata,java.util.Map.of("hibernate.test.temporal.reuse_connection",true),Event.class)) {
			seed(scope);
			for(String field:List.of("offsetStamp","zonedStamp","offsetClock","adjustedStamp","adjustedZoned","adjustedClock")) {
				Object value=input(field,THIRD);
				scope.tx(s->assertThat(s.createMutationQuery("update ColumnQueryEvent set "+field+"=:p where id=4").setParameter("p",value).executeUpdate()).isEqualTo(1));
				scope.tx(s->assertThat(s.createSelectionQuery("select "+field+" from ColumnQueryEvent where id=4",Object.class).getSingleResult()).isEqualTo(expected(field,THIRD)));
				storedTuple(scope,field,false);
				scope.tx(s->assertThat(s.createMutationQuery("update ColumnQueryEvent set "+field+"=:p where id=4").setParameter("p",null).executeUpdate()).isEqualTo(1));
				scope.tx(s->assertThat(s.createSelectionQuery("select "+field+" from ColumnQueryEvent where id=4",Object.class).getSingleResult()).isNull());
				storedTuple(scope,field,true);
				scope.tx(s->s.createMutationQuery("update ColumnQueryEvent set "+field+"=:p where id=4").setParameter("p",value).executeUpdate());
				storedTuple(scope,field,false);
			}
		}
	}

	@ParameterizedTest @ValueSource(booleans={true,false})
	void bulkParameterContext(boolean metadata) {
		try(var scope=ContextScope.open(metadata,java.util.Map.of("hibernate.test.temporal.reuse_connection",true),Event.class)) {
			seed(scope);
			scope.measure();
			scope.tx(s->assertThatThrownBy(()->s.createMutationQuery(
					"update ColumnQueryEvent e set e.adjustedStamp=:p where e.offsetStamp=:p")
					.setParameter("p",FIRST).executeUpdate())
					.hasMessageContaining("parameter").hasMessageContaining("adjustedStamp")
					.hasMessageContaining("offsetStamp").hasMessageContaining("separate parameter"));
			scope.noMutation();
			scope.tx(s->assertThat(s.createMutationQuery(
					"update ColumnQueryEvent e set e.offsetStamp=:p where e.offsetStamp=:p")
					.setParameter("p",FIRST).executeUpdate()).isEqualTo(1));
			scope.tx(s->assertThat(s.createMutationQuery(
					"update ColumnQueryEvent e set e.adjustedStamp=:target where e.offsetStamp=:source")
					.setParameter("target",THIRD).setParameter("source",FIRST).executeUpdate()).isEqualTo(1));
			scope.tx(s->assertThat(s.find(Event.class,1).adjustedStamp).isEqualTo(expected("adjustedStamp",THIRD)));
		}
	}

	@ParameterizedTest @ValueSource(booleans={true,false})
	void mappedCriteriaLiterals(boolean metadata) {
		try(var scope=ContextScope.open(metadata,java.util.Map.of("hibernate.test.temporal.reuse_connection",true),Event.class)) {
			seed(scope);
			for(String field:List.of("offsetStamp","zonedStamp","offsetClock","adjustedStamp","adjustedZoned","adjustedClock")) {
				scope.tx(s->{
					var cb=s.getCriteriaBuilder();var q=cb.createQuery(Integer.class);var e=q.from(Event.class);
					scope.measure();
					assertThatThrownBy(() -> cb.equal(e.get(field),cb.literal(input(field,FIRST))))
						.isInstanceOf(org.hibernate.query.SemanticException.class)
						.hasMessageContaining(field).hasMessageContaining("multi-column")
						.hasMessageContaining("parameter bound to the whole mapped value");
					scope.noSelect();
				});
			}
		}
	}

	static void storedTuple(ContextScope scope,String field,boolean isNull) {
		String offset=switch(field){case "offsetStamp"->"odt_offset";case "zonedStamp"->"zdt_offset";case "offsetClock"->"ot_offset";case "adjustedStamp"->"adjusted_stamp_offset";case "adjustedZoned"->"adjusted_zoned_offset";default->"adjusted_clock_offset";};
		scope.tx(s->s.doWork(c->{try(var st=c.createStatement();var rs=st.executeQuery("select "+field+","+offset+" from ColumnQueryEvent where id=4")) {
			assertThat(rs.next()).isTrue();
			if(isNull){assertThat(rs.getObject(1)).isNull();assertThat(rs.getObject(2)).isNull();}
			else {
				var utc=java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
				if(field.endsWith("Clock")&&rs.getMetaData().getColumnType(1)==java.sql.Types.TIME_WITH_TIMEZONE)
					assertThat(rs.getObject(1,OffsetTime.class)).isEqualTo(OffsetTime.parse("10:30:00.123456Z"));
				else if(field.endsWith("Clock")&&rs.getMetaData().getColumnType(1)==java.sql.Types.TIME)
					assertThat(rs.getObject(1,LocalTime.class)).isEqualTo(LocalTime.parse("10:30:00.123456"));
				else {
					var primary=rs.getTimestamp(1,utc).toInstant();
					if(field.endsWith("Clock"))assertThat(primary.atOffset(ZoneOffset.UTC).toLocalTime()).isEqualTo(LocalTime.parse("10:30:00.123456"));
					else assertThat(primary).isEqualTo(Instant.parse("2026-07-01T10:30:00.123456Z"));
				}
				assertThat(rs.getInt(2)).isEqualTo(field.equals("adjustedStamp")||field.equals("adjustedZoned")?-14400:0);
			}
		}}));
	}

	static Object input(String field,OffsetDateTime value) {
		return field.endsWith("Clock")?value.toOffsetTime():field.equals("zonedStamp")||field.equals("adjustedZoned")?value.toZonedDateTime():value;
	}
	static Object expected(String field,OffsetDateTime value) {
		if(field.equals("adjustedStamp"))return value.atZoneSameInstant(ZoneId.of("America/New_York")).toOffsetDateTime();
		if(field.equals("adjustedZoned"))return value.atZoneSameInstant(ZoneId.of("America/New_York"));
		if(field.equals("adjustedClock"))return value.toOffsetTime().withOffsetSameInstant(ZoneOffset.UTC);
		return input(field,value);
	}
	static void seed(ContextScope scope) {
		scope.tx(s->{int id=1;for(var value:List.of(FIRST,SECOND,THIRD)){s.persist(new Event(id++,value));}s.persist(new Event(4,null));});
	}
	@MappedSuperclass static class AdjustedBase {
		@Column(secondPrecision=6) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneColumn(name="adjusted_stamp_offset") @TimeZoneAdjustment("America/New_York") OffsetDateTime adjustedStamp;
	}

	@Entity(name="ColumnQueryEvent") static class Event extends AdjustedBase {
		@Id int id;
		@Column(secondPrecision=6) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneColumn(name="odt_offset") OffsetDateTime offsetStamp;
		@Column(secondPrecision=6) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneColumn(name="zdt_offset") ZonedDateTime zonedStamp;
		@Column(secondPrecision=6) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneColumn(name="ot_offset") OffsetTime offsetClock;

		@Column(secondPrecision=6) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneColumn(name="adjusted_zoned_offset") @TimeZoneAdjustment("America/New_York") ZonedDateTime adjustedZoned;
		@Column(secondPrecision=6) @TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN) @TimeZoneColumn(name="adjusted_clock_offset") @TimeZoneAdjustment("Z") OffsetTime adjustedClock;
		Event() {}
		Event(int id,OffsetDateTime value){this.id=id;offsetStamp=value;adjustedStamp=value;zonedStamp=value==null?null:value.toZonedDateTime();adjustedZoned=zonedStamp;offsetClock=value==null?null:value.toOffsetTime();adjustedClock=offsetClock;}
	}
}
