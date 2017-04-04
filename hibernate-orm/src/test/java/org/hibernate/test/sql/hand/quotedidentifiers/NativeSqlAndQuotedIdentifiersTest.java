/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.hand.quotedidentifiers;

import org.junit.Test;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.testing.DialectCheck;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * Test of various situations with native-sql queries and quoted identifiers
 *
 * @author Steve Ebersole
 */
@RequiresDialectFeature( value = NativeSqlAndQuotedIdentifiersTest.LocalDialectCheck.class )
public class NativeSqlAndQuotedIdentifiersTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "sql/hand/quotedidentifiers/Mappings.hbm.xml" };
	}

	public static class LocalDialectCheck implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return '\"' == dialect.openQuote();
		}
	}

	@Override
	protected void prepareTest() throws Exception {
		if( sessionFactory()==null)return;
		Session session = sessionFactory().openSession();
		session.beginTransaction();
		session.save( new Person( "me" ) );
		session.getTransaction().commit();
		session.close();
	}

	@Override
	protected void cleanupTest() throws Exception {
		if( sessionFactory()==null)return;
		Session session = sessionFactory().openSession();
		session.beginTransaction();
		session.createQuery( "delete Person" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testCompleteScalarDiscovery() {
		Session session = openSession();
		session.beginTransaction();
		session.getNamedQuery( "query-person" ).list();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testPartialScalarDiscovery() {
		Session session = openSession();
		session.beginTransaction();
		SQLQuery query = (SQLQuery) session.getNamedQuery( "query-person" );
		query.setResultSetMapping( "person-scalar" );
		query.list();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testBasicEntityMapping() {
		Session session = openSession();
		session.beginTransaction();
		SQLQuery query = (SQLQuery) session.getNamedQuery( "query-person" );
		query.setResultSetMapping( "person-entity-basic" );
		query.list();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testExpandedEntityMapping() {
		Session session = openSession();
		session.beginTransaction();
		SQLQuery query = (SQLQuery) session.getNamedQuery( "query-person" );
		query.setResultSetMapping( "person-entity-expanded" );
		query.list();
		session.getTransaction().commit();
		session.close();
	}
}
