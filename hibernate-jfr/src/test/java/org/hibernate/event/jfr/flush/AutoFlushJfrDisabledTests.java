/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.jfr.flush;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;


@DomainModel(annotatedClasses = {
		AutoFlushJfrDisabledTests.TestEntity.class,
})
@SessionFactory
@JiraKey(value = "HHH-18770")
public class AutoFlushJfrDisabledTests {

	/**
	 * Execute a query (and a flush) with the jfr module on the classpath but jfr disabled
	 */
	@Test
	public void testFlushEventWithPartialFlushEventDisabled(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity entity = new TestEntity( 1, "name_1" );
					session.persist( entity );
					session.createQuery( "select t from TestEntity t" ).list();

					session.remove( entity );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Integer id;

		private String name;

		public TestEntity() {
		}

		public TestEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
