/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.locking;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.MULTILINE;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Bin Chen (bin.chen@team.neustar)
 */
public class PessimisticWriteLockWithAliasTest
		extends BaseNonConfigCoreFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ A.class, B.class };
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		sqlStatementInterceptor = new SQLStatementInterceptor( sfb );
	}

	private A entityA;
	private B entityB;

	@Before
	public void createTestData() {
		Session session = sessionFactory().openSession();
		session.beginTransaction();
		try {
			entityA = new A();
			session.persist( entityA );
			entityB = new B( "foo" );
			entityB.setA( entityA );
			session.persist( entityB );
		}
		finally {
			session.getTransaction().commit();
			session.close();
		}
	}

	@Test
	@RequiresDialect({ Oracle8iDialect.class, Oracle9iDialect.class,
			Oracle10gDialect.class, Oracle12cDialect.class })
	public void testSetLockModeWithAlias() {

		Session session = sessionFactory().openSession();
		session.beginTransaction();
		try {
			session.createQuery(
					"select b from B b left join fetch b.a", B.class )
					.unwrap( org.hibernate.query.Query.class )
					.setLockMode( "b", LockMode.PESSIMISTIC_WRITE )
					.list();

			/*
			 * The generated SQL would be like: <pre> select b0_.id as id1_1_0_, a1_.id as id1_0_1_, b0_.a_id as
			 * a_id3_1_0_, b0_.b_value as b_value2_1_0_, a1_.a_value as a_value2_0_1_ from T_LOCK_B b0_ left outer join
			 * T_LOCK_A a1_ on b0_.a_id=a1_.id for update of b0_.id </pre>
			 */
			String lockingQuery = sqlStatementInterceptor.getSqlQueries().getLast().toLowerCase();

			// attempt to get the alias that is specified in the from clause
			Pattern fromTableAliasPattern = Pattern.compile( "from t_lock_b (\\S+)", CASE_INSENSITIVE | MULTILINE );
			Matcher aliasGroup = fromTableAliasPattern.matcher( lockingQuery );
			assertTrue( "Fail to locate alias in the from clause: " + lockingQuery, aliasGroup.find() );
			assertTrue( "Actual query: " + lockingQuery,
					lockingQuery.endsWith( " for update of " + aliasGroup.group( 1 ) + ".id" ) );
		}
		finally {
			session.getTransaction().commit();
			session.close();
		}
	}

}
