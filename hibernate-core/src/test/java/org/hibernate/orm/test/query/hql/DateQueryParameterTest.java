/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
public class DateQueryParameterTest extends SessionFactoryBasedFunctionalTest {
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

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { DateEntity.class };
	}

	@Test
	public void testDateEntityQuery() {

		long current = System.currentTimeMillis();
		long timestamp1 = current - 2222;
		long timestamp2 = current - 1111;
		long timestamp3 = current;
		long timestamp4 = current + 1111;
		long timestamp5 = current + 2222;

		inTransaction(
				session -> {
					final DateEntity entity = new DateEntity();
					entity.setTimestamp( new Date(timestamp2) );
					session.save( entity );
				}
		);

		inTransaction(
				session -> {
					final DateEntity entity = new DateEntity();
					entity.setTimestamp( new Date(timestamp4) );
					session.save( entity );
				}
		);

		inTransaction(
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
	public void cleanUpData() {
		inTransaction(
				session -> {
					session.createQuery( "delete DateEntity" ).executeUpdate();
				}
		);
	}
}
