/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.sql.hand.query;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.query.NativeQuery;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
public class NamedNativeQueryMementoBuilderTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "sql/hand/query/NativeSQLQueries.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@Test
	public void testRegisteredNamedSQLQueryWithScalar()
	{
		final NamedNativeQueryMementoBuilder builder = new NamedNativeQueryMementoBuilder();
		builder.setName("namedQuery");
		builder.setQuery("select count(*) AS c from ORGANIZATION");
		builder.setQueryReturns(new NativeSQLQueryReturn[1]);

		sessionFactory().registerNamedSQLQueryDefinition("namedQuery", builder.createNamedQueryDefinition());

		final Session s = openSession();
		s.beginTransaction();
		final NativeQuery query = (NativeQuery) s.getNamedQuery( "namedQuery");
		query.addScalar("c");
		final Number result = (Number) query.uniqueResult();
		s.getTransaction().commit();
		s.close();

		assertNotNull(result);
		assertTrue(0 == result.intValue());
	}
}
