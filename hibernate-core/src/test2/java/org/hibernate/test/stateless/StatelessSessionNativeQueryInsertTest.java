/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.stateless;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.StatelessSession;
import org.hibernate.query.NativeQuery;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Andrea Boriero
 */
public class StatelessSessionNativeQueryInsertTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12141")
	public void testInsertInStatelessSession() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			session.doWork( connection -> {
				StatelessSession sls = sessionFactory().openStatelessSession( connection );
				NativeQuery q = sls.createNativeQuery(
						"INSERT INTO TEST_ENTITY (ID,SIMPLE_ATTRIBUTE) values (1,'red')" );
				q.executeUpdate();
			} );
		} );
	}

	@Entity
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {
		@Id
		@Column(name = "ID")
		private Long id;

		@Column(name = "SIMPLE_ATTRIBUTE")
		private String simpleAttribute;
	}
}
