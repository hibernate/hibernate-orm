/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version;

import java.time.ZonedDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.Session;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@JiraKey( value = "HHH-10026" )
public class ZonedDateTimeVersionTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { TheEntity.class };
	}

	@Test
	public void testInstantUsageAsVersion() {
		Session session = openSession();
		session.getTransaction().begin();
		TheEntity e = new TheEntity( 1 );
		session.persist( e );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		e = session.byId( TheEntity.class ).load( 1 );
		assertThat( e.getTs(), notNullValue() );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();
		e = session.byId( TheEntity.class ).load( 1 );
		session.remove( e );
		session.getTransaction().commit();
		session.close();
	}


	@Entity(name = "TheEntity")
	@Table(name="the_entity")
	public static class TheEntity {
		private Integer id;
		private ZonedDateTime ts;

		public TheEntity() {
		}

		public TheEntity(Integer id) {
			this.id = id;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Version
		public ZonedDateTime getTs() {
			return ts;
		}

		public void setTs(ZonedDateTime ts) {
			this.ts = ts;
		}
	}
}
