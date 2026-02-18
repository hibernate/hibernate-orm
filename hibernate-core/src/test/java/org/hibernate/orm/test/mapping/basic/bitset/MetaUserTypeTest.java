/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic.bitset;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.AnnotationBasedUserType;
import org.hibernate.usertype.UserType;
import org.hibernate.usertype.UserTypeCreationContext;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.Integer.parseInt;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.sql.Types.VARCHAR;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = {MetaUserTypeTest.Thing.class, MetaUserTypeTest.SecondThing.class,
		MetaUserTypeTest.ThirdThing.class, MetaUserTypeTest.FourthThing.class, MetaUserTypeTest.Things.class})
public class MetaUserTypeTest {

	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			Thing thing = new Thing();
			thing.period = Period.of( 1, 2, 3 );
			thing.days = Period.ofDays( 42 );
			em.persist( thing );
		} );
		scope.inTransaction( em -> {
			Thing thing = em.find( Thing.class, 1 );
			assertEquals( Period.of( 1, 2, 3 ), thing.period );
			assertEquals( Period.ofDays( 42 ), thing.days );
		} );

		scope.inTransaction( em -> {
			SecondThing thing = new SecondThing();
			thing.period = Period.of( 1, 2, 3 );
			thing.days = Period.ofDays( 42 );
			em.persist( thing );
		} );
		scope.inTransaction( em -> {
			SecondThing thing = em.find( SecondThing.class, 1 );
			assertEquals( Period.of( 1, 2, 3 ), thing.period );
			assertEquals( Period.ofDays( 42 ), thing.days );
		} );

		scope.inTransaction( em -> {
			ThirdThing thing = new ThirdThing();
			thing.period = Period.of( 1, 2, 3 );
			thing.days = Period.ofDays( 42 );
			em.persist( thing );
		} );
		scope.inTransaction( em -> {
			ThirdThing thing = em.find( ThirdThing.class, 1 );
			assertEquals( Period.of( 1, 2, 3 ), thing.period );
			assertEquals( Period.ofDays( 42 ), thing.days );
		} );

		scope.inTransaction( em -> {
			FourthThing thing = new FourthThing();
			thing.period = Period.of( 1, 2, 3 );
			thing.days = Period.ofDays( 42 );
			em.persist( thing );
		} );
		scope.inTransaction( em -> {
			FourthThing thing = em.find( FourthThing.class, 1 );
			assertEquals( Period.of( 1, 2, 3 ), thing.period );
			assertEquals( Period.ofDays( 42 ), thing.days );
		} );
	}

	@Test void testCollection(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			Things things = new Things();
			things.periods.add( Period.of( 1, 2, 3 ) );
			things.days.add( Period.ofDays( 42 ) );
			em.persist( things );
		} );
		scope.inTransaction( em -> {
			Things things = em.find( Things.class, 1 );
			assertEquals( Period.of( 1, 2, 3 ), things.periods.get( 0 ) );
			assertEquals( Period.ofDays( 42 ), things.days.get( 0 ) );
		} );
	}

	@Entity(name = "Thing")
	static class Thing {
		@Id @GeneratedValue
		long id;
		@TimePeriod
		Period period;
		@TimePeriod(days = true)
		Period days;
	}

	@Entity(name = "SecondThing")
	static class SecondThing {
		@Id @GeneratedValue
		long id;
		@SecondTimePeriod
		Period period;
		@SecondTimePeriod(days = true)
		Period days;
	}

	@Entity(name = "ThirdThing")
	static class ThirdThing {
		@Id @GeneratedValue
		long id;
		@ThirdTimePeriod
		Period period;
		@ThirdTimePeriod(days = true)
		Period days;
	}

	@Entity(name = "FourthThing")
	static class FourthThing {
		@Id @GeneratedValue
		long id;
		@FourthTimePeriod
		Period period;
		@FourthTimePeriod(days = true)
		Period days;
	}

	@Entity(name = "Things")
	static class Things {
		@Id @GeneratedValue
		long id;
		@TimePeriod
		@ElementCollection
		@CollectionTable(name = "Periods")
		List<Period> periods = new ArrayList<>();
		@TimePeriod(days = true)
		@ElementCollection
		@CollectionTable(name = "Days")
		List<Period> days = new ArrayList<>();
	}

	@Type(PeriodType.class)
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	public @interface TimePeriod {
		boolean days() default false;
	}

	@Type(SecondPeriodType.class)
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	public @interface SecondTimePeriod {
		boolean days() default false;
	}

	@Type(ThirdPeriodType.class)
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	public @interface ThirdTimePeriod {
		boolean days() default false;
	}

	@Type(value = FourthPeriodType.class, parameters = @Parameter(name="foo", value ="bar"))
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	public @interface FourthTimePeriod {
		boolean days() default false;
	}

	static class PeriodType extends AbstractPeriodType {

		PeriodType(TimePeriod timePeriod) {
			super(timePeriod.days());
		}

	}

	static class SecondPeriodType extends AbstractPeriodType {

		SecondPeriodType(UserTypeCreationContext context) {
			super( ( (Field) context.getMemberDetails().toJavaMember() ).getAnnotation( SecondTimePeriod.class ).days() );
		}

	}

	static class ThirdPeriodType extends AbstractPeriodType {

		ThirdPeriodType(ThirdTimePeriod timePeriod, UserTypeCreationContext context) {
			super(timePeriod.days());
			if ( !timePeriod.equals( ( (Field) context.getMemberDetails().toJavaMember() ).getAnnotation( ThirdTimePeriod.class ) )) {
				throw new IllegalArgumentException(context.getMemberDetails().toJavaMember() + " should be annotated with " + timePeriod);
			}
		}

	}

	static class FourthPeriodType extends AbstractPeriodType implements AnnotationBasedUserType<FourthTimePeriod, Period> {

		FourthPeriodType() {
			super(false);
		}

		@Override
		public void initialize(FourthTimePeriod timePeriod, UserTypeCreationContext context) {
			days = timePeriod.days();
			if ( !timePeriod.equals( ( (Field) context.getMemberDetails().toJavaMember() ).getAnnotation( FourthTimePeriod.class ) )) {
				// only for validation
				throw new IllegalArgumentException(context.getMemberDetails().toJavaMember() + " should be annotated with " + timePeriod);
			}
			assertEquals( "bar", context.getParameters().get("foo") );
		}
	}

	static abstract class AbstractPeriodType implements UserType<Period> {
		boolean days;

		AbstractPeriodType(boolean days) {
			this.days = days;
		}

		@Override
		public int getSqlType() {
			return VARCHAR;
		}

		@Override
		public Class<Period> returnedClass() {
			return Period.class;
		}

		@Override
		public boolean equals(Period x, Period y) {
			return Objects.equals(x, y);
		}

		@Override
		public int hashCode(Period x) {
			return x.hashCode();
		}

		@Override
		public Period nullSafeGet(ResultSet rs, int position, WrapperOptions options)
				throws SQLException {
			String string = rs.getString(position);
			if ( rs.wasNull() ) {
				return null;
			}
			else if ( days ) {
				return Period.ofDays( parseInt( string ) );
			}
			else {
				return Period.parse( string );
			}
		}

		@Override
		public void nullSafeSet(PreparedStatement st, Period value, int index, WrapperOptions options)
				throws SQLException {
			if ( value == null ) {
				st.setNull(index, VARCHAR);
			}
			else if ( days ) {
				st.setString(index, Integer.toString(value.getDays()));
			}
			else {
				st.setString(index, value.toString());
			}
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Period deepCopy(Period value) {
			return value; // Period is immutable
		}

		@Override
		public Serializable disassemble(Period period) {
			return period; // Period is immutable
		}

		@Override
		public Period assemble(Serializable cached, Object owner) {
			return (Period) cached; // Period is immutable
		}
	}
}
