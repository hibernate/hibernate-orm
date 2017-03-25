/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.generated;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class CreationTimestampTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Event.class
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::mapping-generated-CreationTimestamp-persist-example[]
			Event dateEvent = new Event( );
			entityManager.persist( dateEvent );
			//end::mapping-generated-CreationTimestamp-persist-example[]
		} );
	}

	//tag::mapping-generated-CreationTimestamp-example[]
	@Entity(name = "Event")
	public static class Event {

		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "`timestamp`")
		@CreationTimestamp
		private Date timestamp;

		public Event() {}

		public Long getId() {
			return id;
		}

		public Date getTimestamp() {
			return timestamp;
		}
	}
	//end::mapping-generated-CreationTimestamp-example[]
}
