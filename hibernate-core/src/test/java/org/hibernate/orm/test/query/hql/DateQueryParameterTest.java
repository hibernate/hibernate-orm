/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.Date;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@DomainModel( annotatedClasses = DateQueryParameterTest.DateEntity.class )
@SessionFactory
public class DateQueryParameterTest {

	@Test
	public void testDateEntityQuery(SessionFactoryScope scope) {
		long current = System.currentTimeMillis();
		long timestamp1 = current - 2222;
		long timestamp2 = current - 1111;
		long timestamp3 = current;
		long timestamp4 = current + 1111;
		long timestamp5 = current + 2222;

		scope.inTransaction(
				session -> {
					final DateEntity entity = new DateEntity();
					entity.setTimestamp( new Date(timestamp2) );
					session.persist( entity );
				}
		);

		scope.inTransaction(
				session -> {
					final DateEntity entity = new DateEntity();
					entity.setTimestamp( new Date(timestamp4) );
					session.persist( entity );
				}
		);

		scope.inTransaction(
				session -> {
					// Test nothing before timestamp1
					List<DateEntity> results = session
							.createQuery( "SELECT e FROM DateEntity e WHERE e.timestamp < :value", DateEntity.class )
							.setParameter( "value", new Date( timestamp1 ) )
							.getResultList();
					assertEquals( 0, results.size() );

					// Test only one entry before timestamp2
					results = session
							.createQuery( "SELECT e FROM DateEntity e WHERE e.timestamp < : value", DateEntity.class )
							.setParameter( "value", new Date( timestamp3 ) )
							.getResultList();
					assertEquals( 1, results.size() );

					// Test two entries before timestamp3
					results = session
							.createQuery( "SELECT e FROM DateEntity e WHERE e.timestamp < : value", DateEntity.class )
							.setParameter( "value", new Date( timestamp5 ) )
							.getResultList();
					assertEquals( 2, results.size() );
				}
		);
	}

	@AfterEach
	public void cleanUpData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "DateEntity")
	public static class DateEntity {
		@Id
		@GeneratedValue
		private Integer id;
		private Date timestamp;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Date getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(Date timestamp) {
			this.timestamp = timestamp;
		}
	}
}
