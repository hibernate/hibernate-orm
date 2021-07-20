/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.transaction.TransactionUtil;

import org.junit.Assert;
import org.junit.Test;

@RequiresDialect(PostgreSQL82Dialect.class)
public class PostgresNullWorkaroundTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {LocalDateEvent.class};
	}

	@Test(expected = javax.persistence.PersistenceException.class)
	public void testNoOverride() {
		TransactionUtil.doInJPA(this::entityManagerFactory, em -> {
			javax.persistence.Query q = em.createNativeQuery("INSERT INTO local_date_event(id, event_date, event_name) VALUES (:id, :ed, :en)"
					+ " ON CONFLICT (id) DO UPDATE SET event_date = EXCLUDED.event_date, event_name = EXCLUDED.event_name");
			q.setParameter("id", 1L);
			q.setParameter("ed", null);
			q.setParameter("en", "Not relevant");
			q.executeUpdate();
			Assert.fail("Unless PostgreSQL has fixed their servers to check for nulls independent of type, this test has failed");
		} );
	}

	@Test
	public void testOverride() {
		TransactionUtil.doInJPA(this::entityManagerFactory, em -> {
			javax.persistence.Query q = em.createNativeQuery("INSERT INTO local_date_event(id, event_date, event_name) VALUES (:id, :ed, :en)"
					+ " ON CONFLICT (id) DO UPDATE SET event_date = EXCLUDED.event_date, event_name = EXCLUDED.event_name");
			Hibernate.setQueryParameterType(q, "ed", LocalDate.class);
			q.setParameter("id", 1L);
			q.setParameter("ed", null);
			q.setParameter("en", "Not relevant");
			q.executeUpdate();
		} );
	}

	@Entity(name = "LocalDateEvent")
	@Table(name = "local_date_event")
	public static class LocalDateEvent {

		@Id
		private Long id;

		@Column(name = "event_date")
		private LocalDate date;

		@Column(name = "event_name")
		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public LocalDate getDate() {
			return date;
		}

		public void setDate(LocalDate date) {
			this.date = date;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
