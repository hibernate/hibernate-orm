/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.quote.resultsetmappings;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class ExplicitSqlResultSetMappingTest extends BaseCoreFunctionalTestCase {
	private String queryString = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { MyEntity.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.setProperty( Environment.GLOBALLY_QUOTED_IDENTIFIERS, "true" );
	}

	private void prepareTestData() {
		char open = getDialect().openQuote();
		char close = getDialect().closeQuote();
		queryString="select t."+open+"NAME"+close+" as "+open+"QuotEd_nAMe"+close+" from "+open+"MY_ENTITY_TABLE"+close+" t";
		Session s = sessionFactory().openSession();
		s.beginTransaction();
		s.save( new MyEntity( "mine" ) );
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	public void testCompleteScalarAutoDiscovery() {
		prepareTestData();

		Session s = openSession();
		s.beginTransaction();
		s.createSQLQuery( queryString )
				.list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testPartialScalarAutoDiscovery() {
		prepareTestData();

		Session s = openSession();
		s.beginTransaction();
		s.createSQLQuery( queryString )
				.setResultSetMapping( "explicitScalarResultSetMapping" )
				.list();
		s.getTransaction().commit();
		s.close();
	}
}
