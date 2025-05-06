/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.tck2_2;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.TableGenerator;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-12157")
public class TableGeneratorVisibilityTest extends BaseCoreFunctionalTestCase {

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE, true );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				TestEntity1.class,
				TestEntity2.class,
				TestEntity3.class
		};
	}

	@Test
	public void testGeneratorIsVisible() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.persist( new TestEntity1() );
		} );

		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.persist( new TestEntity2() );
		} );

		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.persist( new TestEntity3() );
		} );
	}

	@Entity(name = "TestEntity1")

	public static class TestEntity1 {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "table-generator-2")
		public long id;
	}

	@Entity(name = "TestEntity2")
	public static class TestEntity2 {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "table-generator")
		public long id;
	}

	@Entity(name = "TestEntity3")
	@TableGenerator(
			name = "table-generator",
			table = "table_identifier",
			pkColumnName = "identifier",
			valueColumnName = "val",
			allocationSize = 5
	)
	public static class TestEntity3 {
		@Id
		@TableGenerator(
				name = "table-generator-2",
				table = "table_identifier_2",
				pkColumnName = "identifier",
				valueColumnName = "val",
				allocationSize = 5
		)
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "table-generator")
		public long id;
	}
}
