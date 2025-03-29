/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.io.Serializable;
import java.time.YearMonth;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		YearMonthConverterTest.DemoEntity.class
} )
public class YearMonthConverterTest {
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
	public static class DemoEntity implements Serializable {
		@Id
		private Long id;

		@Convert( converter = YearMonthConverter.class )
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

	@Converter( autoApply = true )
	public static class YearMonthConverter implements AttributeConverter<YearMonth, Integer> {
		@Override
		public Integer convertToDatabaseColumn(YearMonth attribute) {
			return attribute == null ? null : ( attribute.getYear() * 100 ) + attribute.getMonth().getValue();
		}

		@Override
		public YearMonth convertToEntityAttribute(Integer dbData) {
			return dbData == null ? null : YearMonth.of( dbData / 100, dbData % 100 );
		}
	}
}
