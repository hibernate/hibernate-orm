/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.StatelessSession;
import org.hibernate.query.NativeQuery;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = StatelessSessionNativeQueryInsertTest.TestEntity.class
)
@SessionFactory
public class StatelessSessionNativeQueryInsertTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-12141")
	public void testInsertInStatelessSession(SessionFactoryScope scope) {
		scope.inSession(
				session ->
						session.doWork(
								connection -> {
									StatelessSession sls = scope.getSessionFactory().openStatelessSession( connection );
									NativeQuery q = sls.createNativeQuery(
											"INSERT INTO TEST_ENTITY (ID,SIMPLE_ATTRIBUTE) values (1,'red')" );
									q.executeUpdate();
								} )
		);
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {
		@Id
		@Column(name = "ID")
		private Long id;

		@Column(name = "SIMPLE_ATTRIBUTE")
		private String simpleAttribute;
	}
}
