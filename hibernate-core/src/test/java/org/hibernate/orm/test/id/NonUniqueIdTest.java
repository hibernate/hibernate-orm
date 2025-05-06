/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.HibernateException;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = NonUniqueIdTest.Category.class
)
@SessionFactory
public class NonUniqueIdTest {

	@BeforeAll
	public void setup(SessionFactoryScope scope) {
		// Drop and recreate table so it has no primary key. The drop is done in a separate transaction because
		// some databases do not support dropping and recreating in the same transaction.
		scope.inTransaction(
				session -> {
					session.createNativeQuery(
							"DROP TABLE CATEGORY"
					).executeUpdate();
				}
		);

		scope.inTransaction(
				session -> {
					session.createNativeQuery(
							"create table CATEGORY( id integer not null, name varchar(255) )"
					).executeUpdate();
				}
		);

		scope.inTransaction(
				session -> {
					session.createNativeQuery( "insert into CATEGORY( id, name) VALUES( 1, 'clothes' )" )
							.executeUpdate();
					session.createNativeQuery( "insert into CATEGORY( id, name) VALUES( 1, 'shoes' )" )
							.executeUpdate();
				}
		);
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery( "delete from Category" ).executeUpdate()

		);
	}

	@Test
	@JiraKey(value = "HHH-12802")
	public void testLoadEntityWithNonUniqueId(SessionFactoryScope scope) {
		try {
			scope.inTransaction(
					session -> {
						session.get( Category.class, 1 );
						fail( "should have failed because there are 2 entities with id == 1" );
					}
			);
		}
		catch (HibernateException ex) {
			// expected
		}
	}

	@Entity(name = "Category")
	@Table(name = "CATEGORY")
	public static class Category {
		@Id
		private int id;

		private String name;
	}
}
