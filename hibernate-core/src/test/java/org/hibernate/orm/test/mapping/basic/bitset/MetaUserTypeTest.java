/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic.bitset;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.Type;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Period;
import java.util.Objects;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.sql.Types.VARCHAR;

@Jpa(annotatedClasses = MetaUserTypeTest.Thing.class)
public class MetaUserTypeTest {

	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			Period p = Period.of( 1, 2, 3 );
			Thing thing = new Thing();
			thing.period = p;
			em.persist( thing );
		} );
	}

	@Entity static class Thing {
		@Id @GeneratedValue
		long id;
		@TimePeriod
		Period period;
	}

	@Type(PeriodType. class)
	@Target({METHOD, FIELD})
	@Retention(RUNTIME)
	public @interface TimePeriod {}

	static class PeriodType implements UserType<Period> {

		PeriodType(TimePeriod timePeriod) {

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
			return rs. wasNull() ? null : Period.parse(string);
		}

		@Override
		public void nullSafeSet(PreparedStatement st, Period value, int index, WrapperOptions options)
				throws SQLException {
			if ( value == null ) {
				st.setNull(index, VARCHAR);
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
