/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or visit http://www.apache.org/licenses/LICENSE-2.0
 */
package org.hibernate.test.idgen.identity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.FlushMode;
import org.hibernate.boot.SessionFactoryBuilder;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.jdbc.SQLStatementInterceptor;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-11019")
@RequiresDialectFeature( DialectChecks.SupportsIdentityColumns.class )
public class DelayIdentityInsertTest extends BaseNonConfigCoreFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		sqlStatementInterceptor = new SQLStatementInterceptor( sfb );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Company.class};
	}

	@Test
	public void testPersist() {
		assertDelayedInsert(false, FlushMode.AUTO);
		assertDelayedInsert(false, FlushMode.ALWAYS);
		assertDelayedInsert(true, FlushMode.COMMIT);
		assertDelayedInsert(true, FlushMode.MANUAL);
	}

	private void assertDelayedInsert(boolean delayed, FlushMode flushMode) {
		sqlStatementInterceptor.getSqlQueries().clear();
		assertEquals( delayed, doInHibernate( this::sessionFactory, session -> {
			session.setHibernateFlushMode( flushMode );
			Company company = new Company();
			session.persist( company );
			return sqlStatementInterceptor.getSqlQueries().isEmpty();
		} ));
	}

	@Entity
	@Table(name = "Company")
	public class Company {
		private Integer id;

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}
}
