/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.basic;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class DateWithTemporalDateTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				DateEvent.class
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::basic-datetime-temporal-date-persist-example[]
			DateEvent dateEvent = new DateEvent( new Date() );
			entityManager.persist( dateEvent );
			//end::basic-datetime-temporal-date-persist-example[]
		} );
	}

	//tag::basic-datetime-temporal-date-example[]
	@Entity(name = "DateEvent")
	public static class DateEvent {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "`timestamp`")
		@Temporal(TemporalType.DATE)
		private Date timestamp;

		//Getters and setters are omitted for brevity

	//end::basic-datetime-temporal-date-example[]
		public DateEvent() {
		}

		public DateEvent(Date timestamp) {
			this.timestamp = timestamp;
		}

		public Long getId() {
			return id;
		}

		public Date getTimestamp() {
			return timestamp;
		}
	//tag::basic-datetime-temporal-date-example[]
	}
	//end::basic-datetime-temporal-date-example[]
}
