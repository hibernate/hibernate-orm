/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.basic;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class LocalDateTimeWithTemporalTimeTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				DateEvent.class
		};
	}

	@Test
	public void testLifecycle() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			DateEvent dateEvent = new DateEvent( LocalDateTime.now() );
			dateEvent.id = 1L;
			entityManager.persist( dateEvent );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			DateEvent dateEvent = entityManager.find( DateEvent.class, 1L );
			assertNotNull(dateEvent.getTimestamp());
		} );
	}

	@Entity(name = "DateEvent")
	public static class DateEvent {

		@Id
		private Long id;

		//throws org.hibernate.AnnotationException: @Temporal should only be set on a java.util.Date or java.util.Calendar property
		//@Temporal(TemporalType.TIME)
		@Column(name = "`timestamp`")
		private LocalDateTime timestamp;

		public DateEvent() {
		}

		public DateEvent(LocalDateTime timestamp) {
			this.timestamp = timestamp;
		}

		public Long getId() {
			return id;
		}

		public LocalDateTime getTimestamp() {
			return timestamp;
		}
	}
}
