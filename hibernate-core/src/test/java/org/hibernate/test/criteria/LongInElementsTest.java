/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.test.criteria;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.TeradataDialect;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.test.hql.StateProvince;

/**
 * 
 * HHH-2166 Long "in" lists in queries results in a Java stack overflow
 * exception. to reproduce this issue, you should add
 * "<argLine>-Xss128k</argLine>" to the surefire plugin (test on Fedora 12)
 * 
 * @author Strong Liu
 */
public class LongInElementsTest extends FunctionalTestCase {

	private static final int ELEMENTS_SIZE = 4000;

	public LongInElementsTest( String string ) {
		super( string );
	}

	public String[] getMappings() {
		return new String[] { "criteria/Animal.hbm.xml" };
	}

	// HHH-2166
	public void testLongInElementsByHQL() {
		if ( (getDialect() instanceof SQLServerDialect)
				|| (getDialect() instanceof Oracle8iDialect) 
				|| (getDialect() instanceof TeradataDialect)){
			skipExpectedFailure( new Exception("this test fails on oracle and ms sql server, for more info, see HHH-1123") );
			return;
		}
		Session session = openSession();
		Transaction t = session.beginTransaction();

		StateProvince beijing = new StateProvince();
		beijing.setIsoCode( "100089" );
		beijing.setName( "beijing" );
		session.persist( beijing );
		session.flush();
		session.clear();

		Query query = session
				.createQuery( "from org.hibernate.test.hql.StateProvince sp where sp.id in ( :idList )" );
		query.setParameterList( "idList" , createLotsOfElements() );
		List list = query.list();
		session.flush();
		session.clear();
		assertEquals( 1 , list.size() );
		session.delete( beijing );
		t.commit();
		session.close();

	}

	// HHH-2166
	public void testLongInElementsByCriteria() {
		if ( (getDialect() instanceof SQLServerDialect)
				|| (getDialect() instanceof Oracle8iDialect) 
				|| (getDialect() instanceof TeradataDialect)){
			skipExpectedFailure( new Exception("this test fails on oracle and ms sql server, for more info, see HHH-1123") );
			return;
		}
		Session session = openSession();
		Transaction t = session.beginTransaction();

		StateProvince beijing = new StateProvince();
		beijing.setIsoCode( "100089" );
		beijing.setName( "beijing" );
		session.persist( beijing );
		session.flush();
		session.clear();

		Criteria criteria = session.createCriteria( StateProvince.class );
		criteria.add( Restrictions.in( "id" , createLotsOfElements() ) );
		List list = criteria.list();
		session.flush();
		session.clear();
		assertEquals( 1 , list.size() );
		session.delete( beijing );
		t.commit();
		session.close();

	}

	private List createLotsOfElements() {
		List list = new ArrayList();
		for ( int i = 0; i < ELEMENTS_SIZE; i++ ) {
			list.add( Long.valueOf( i ) );
		}
		return list;
	}
}
