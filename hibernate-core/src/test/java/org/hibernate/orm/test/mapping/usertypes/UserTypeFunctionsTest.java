/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.usertypes;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.YearMonth;
import java.util.Objects;

import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 *
 * @implNote Same as {@link org.hibernate.orm.test.mapping.converted.converter.YearMonthConverterTest}
 * but with {@link UserType} instead of {@link jakarta.persistence.AttributeConverter}
 */
@DomainModel( annotatedClasses = { UserTypeFunctionsTest.DemoEntity.class } )
@SessionFactory
public class UserTypeFunctionsTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new DemoEntity( 1L, YearMonth.of( 2022, 12 ) ) );
			session.persist( new DemoEntity( 2L, YearMonth.of( 2023, 1 ) ) );
			session.persist( new DemoEntity( 3L, YearMonth.of( 2023, 2 ) ) );
			session.persist( new DemoEntity( 4L, YearMonth.of( 2023, 3 ) ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from DemoEntity" ).executeUpdate() );
	}

	@Test
	public void testSimpleQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final YearMonth ym = session.createQuery(
					"select de.yearMonth from DemoEntity de where de.id = 1L",
					YearMonth.class
			).getSingleResult();
			assertThat( ym ).isEqualTo( YearMonth.of( 2022, 12 ) );
		} );
	}

	@Test
	public void testMax(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final YearMonth max = session.createQuery(
					"select max(de.yearMonth) from DemoEntity de",
					YearMonth.class
			).getSingleResult();
			assertThat( max ).isEqualTo( YearMonth.of( 2023, 3 ) );
		} );
	}

	@Test
	public void testMaxComparison(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final DemoEntity de = session.createQuery(
					"from DemoEntity de where de.yearMonth = (select max(sub.yearMonth) from DemoEntity sub)",
					DemoEntity.class
			).getSingleResult();
			assertThat( de.getId() ).isEqualTo( 4L );
		} );
	}

	@Entity( name = "DemoEntity" )
	public static class DemoEntity {
		@Id
		private Long id;
		@Type( YearMonthUserType.class )
		private YearMonth yearMonth;

		public DemoEntity() {
		}

		public DemoEntity(Long id, YearMonth yearMonth) {
			this.id = id;
			this.yearMonth = yearMonth;
		}

		public Long getId() {
			return id;
		}

		public YearMonth getYearMonth() {
			return yearMonth;
		}
	}

	public static class YearMonthUserType implements UserType<YearMonth> {
		@Override
		public int getSqlType() {
			return SqlTypes.INTEGER;
		}

		@Override
		public Class<YearMonth> returnedClass() {
			return YearMonth.class;
		}

		@Override
		public boolean equals(YearMonth x, YearMonth y) {
			return Objects.equals( x, y );
		}

		@Override
		public int hashCode(YearMonth x) {
			return Objects.hashCode( x );
		}

		@Override
		public YearMonth nullSafeGet(ResultSet rs, int position, WrapperOptions options)
				throws SQLException {
			int intValue = rs.getInt( position );
			if ( rs.wasNull() ) {
				return null;
			}
			return YearMonth.of( intValue / 100, intValue % 100 );
		}

		@Override
		public void nullSafeSet(
				PreparedStatement st,
				YearMonth value,
				int index,
				WrapperOptions options) throws SQLException {
			if ( value == null ) {
				st.setNull( index, Types.INTEGER );
			}
			else {
				st.setInt( index, ( value.getYear() * 100 ) + value.getMonth().getValue() );
			}
		}

		@Override
		public YearMonth deepCopy(YearMonth value) {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Serializable disassemble(YearMonth value) {
			return value;
		}

		@Override
		public YearMonth assemble(Serializable cached, Object owner) {
			return cached instanceof YearMonth ? (YearMonth) cached : null;
		}
	}
}
