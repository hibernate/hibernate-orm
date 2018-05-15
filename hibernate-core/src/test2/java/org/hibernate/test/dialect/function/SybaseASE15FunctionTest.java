/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.dialect.function;

import static java.util.Calendar.MONTH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * 
 * @author Richard H. Tingstad
 */
@RequiresDialect(value = { SybaseASE15Dialect.class })
public class SybaseASE15FunctionTest extends BaseCoreFunctionalTestCase {

	private Calendar calendar = Calendar.getInstance();

	@Override
	public String[] getMappings() {
		return new String[] { "dialect/function/Product.hbm.xml" };
	}

	@Override
	protected void prepareTest() throws Exception {
		final Session s = openSession();
		s.getTransaction().begin();
		Product product = new Product();
		product.setPrice(new BigDecimal(0.5));
		product.setDate( calendar.getTime() );
		s.save( product );
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected void cleanupTest() throws Exception {
		final Session s = openSession();
		s.getTransaction().begin();
		s.createQuery( "delete from Product" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testCharLengthFunction() {
		final Session s = openSession();
		s.getTransaction().begin();
		Query query = session.createQuery( "select char_length('123456') from Product" );
		assertEquals(6, ((Number) query.uniqueResult()).intValue());
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7070")
	public void testDateaddFunction() {
		final Session s = openSession();
		s.getTransaction().begin();
		Query query = session.createQuery( "select dateadd(dd, 1, p.date) from Product p" );
		assertTrue(calendar.getTime().before((Date) query.uniqueResult()));
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7070")
	public void testDatepartFunction() {
		final Session s = openSession();
		s.getTransaction().begin();
		Query query = session.createQuery( "select datepart(month, p.date) from Product p" );
		assertEquals(calendar.get(MONTH) + 1, ((Number) query.uniqueResult()).intValue());
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7070")
	public void testDatediffFunction() {
		final Session s = openSession();
		s.getTransaction().begin();
		Query query = session.createQuery( "SELECT DATEDIFF( DAY, '1999/07/19 00:00', '1999/07/23 23:59' ) from Product" );
		assertEquals(4, ((Number) query.uniqueResult()).intValue());
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7070")
	public void testAtn2Function() {
		final Session s = openSession();
		s.getTransaction().begin();
		Query query = session.createQuery("select atn2(p.price, .48) from Product p");
		assertEquals(0.805803, ((Number) query.uniqueResult()).doubleValue(), 0.000001 );
		s.getTransaction().commit();
		s.close();
	}

}
