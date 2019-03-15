/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.hql;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.junit.jupiter.api.Test;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.transaction.TransactionUtil;

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

	private long timestamp1;
	private long timestamp2;
	private long timestamp3;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { DateEntity.class };
	}

	@Test
	public void testDateEntityQuery() throws Exception {
		try {
			timestamp1 = System.currentTimeMillis();

			TransactionUtil.doInHibernate(
					this::sessionFactory,
					session -> {

						final DateEntity entity = new DateEntity();
						entity.setTimestamp( new Date() );
						session.save( entity );
					}
			);

			timestamp2 = System.currentTimeMillis();
			Thread.sleep( 1100 );

			TransactionUtil.doInHibernate(
					this::sessionFactory,
					session -> {

						final DateEntity entity = new DateEntity();
						entity.setTimestamp( new Date() );
						session.save( entity );
					}
			);

			timestamp3 = System.currentTimeMillis();

			TransactionUtil.doInHibernate(
					this::sessionFactory,
					session -> {
						// Test nothing before timestamp1
						List<DateEntity> results = session
								.createQuery( "SELECT e FROM DateEntity e WHERE e.timestamp < :value", DateEntity.class )
								.setParameter( "value", new Date( timestamp1 ) )
								.getResultList();
						assertEquals( results.size(), 0 );

						// Test only one entry before timestamp2
						results = session
								.createQuery( "SELECT e FROM DateEntity e WHERE e.timestamp < : value", DateEntity.class )
								.setParameter( "value", new Date( timestamp2 ) )
								.getResultList();
						assertEquals( results.size(), 1 );

						// Test two entries before timestamp3
						results = session
								.createQuery( "SELECT e FROM DateEntity e WHERE e.timestamp < : value", DateEntity.class )
								.setParameter( "value", new Date( timestamp3 ) )
								.getResultList();
						assertEquals( results.size(), 2 );
					}
			);
		}
		catch ( InterruptedException e ) {
			throw new RuntimeException( "Failed to thread sleep properly", e );
		}
	}
}
