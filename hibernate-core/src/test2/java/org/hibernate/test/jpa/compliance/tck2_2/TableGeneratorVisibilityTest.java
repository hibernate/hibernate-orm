/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.compliance.tck2_2;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.TableGenerator;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-12157")
public class TableGeneratorVisibilityTest extends BaseCoreFunctionalTestCase {

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE, "true" );
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
			valueColumnName = "value",
			allocationSize = 5
	)
	public static class TestEntity3 {
		@Id
		@TableGenerator(
				name = "table-generator-2",
				table = "table_identifier_2",
				pkColumnName = "identifier",
				valueColumnName = "value",
				allocationSize = 5
		)
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "table-generator")
		public long id;
	}
}
