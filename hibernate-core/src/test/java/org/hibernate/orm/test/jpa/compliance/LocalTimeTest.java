/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.type.descriptor.DateTimeUtils;

import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.TypedQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = LocalTimeTest.TestEntity.class,
		properties = @Setting(name = AvailableSettings.TIMEZONE_DEFAULT_STORAGE, value = "NORMALIZE")
)
public class LocalTimeTest {

	private static final LocalTime LOCAL_TIME = DateTimeUtils.adjustToPrecision(
			LocalTime.now(), 0,
			DialectContext.getDialect()
	);

	private static final OffsetTime OFFSET_TIME = OffsetTime.of( LOCAL_TIME, ZoneOffset.ofHours( 2 ) );

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					TestEntity d1 = new TestEntity(
							1L,
							LOCAL_TIME,
							OffsetTime.of( LocalTime.of( 12, 18, 21 ), ZoneOffset.ofHours( 3 ) )
					);
					TestEntity d2 = new TestEntity(
							2L,
							LocalTime.of( 12, 18, 21 ),
							OFFSET_TIME
					);
					entityManager.persist( d1 );
					entityManager.persist( d2 );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testLocalTimeParameterQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					TypedQuery<TestEntity> query = entityManager.createQuery(
							"select t from TestEntity t where t.time = :time",
							TestEntity.class
					);
					query.setParameter( "time", LOCAL_TIME );

					List<TestEntity> result = query.getResultList();
					assertEquals( 1, result.size() );
					assertEquals( 1, result.get( 0 ).getId() );
				}
		);
	}

	@Test
	public void testOffsetTimeParameterQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					TypedQuery<TestEntity> query = entityManager.createQuery(
							"select t from TestEntity t where t.offset = :offset",
							TestEntity.class
					);
					query.setParameter( "offset", OFFSET_TIME );
					List<TestEntity> result = query.getResultList();
					assertEquals( 1, result.size() );
					assertEquals( 2, result.get( 0 ).getId() );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {

		@Id
		private Long id;

		@Column(name = "TIME_ATTRIBUTE")
		private LocalTime time;

		@Column(name = "OFFSET_ATTRIBUTE")
		private OffsetTime offset;

		public TestEntity() {
		}

		public TestEntity(Long id, LocalTime time, OffsetTime offset) {
			this.id = id;
			this.time = time;
			this.offset = offset;
		}

		public Long getId() {
			return id;
		}
	}

}
