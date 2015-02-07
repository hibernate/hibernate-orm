/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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

package org.hibernate.test.flush;

import static org.junit.Assert.*;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.test.hql.SimpleEntityWithAssociation;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Etienne Miret
 */
public class NativeCriteriaSyncTest extends BaseCoreFunctionalTestCase {

	private Session session;

	@Before
	public void setUp() throws Exception {
		session = openSession();
	}

	@After
	public void tearDown() throws Exception {
		session.close();
	}

	/**
	 * Tests that the join table of a many-to-many relationship is properly flushed before making a related Criteria
	 * query.
	 */
	@Test
	@TestForIssue( jiraKey = "HHH-3813" )
	public void test() {
		final Transaction txn = session.beginTransaction();

		final SimpleEntityWithAssociation e1 = new SimpleEntityWithAssociation( "e1" );
		final SimpleEntityWithAssociation e2 = new SimpleEntityWithAssociation( "e2" );
		e1.getManyToManyAssociatedEntities().add( e2 );
		session.save( e1 );

		final Criteria criteria = session.createCriteria( SimpleEntityWithAssociation.class );
		criteria.createCriteria( "manyToManyAssociatedEntities" ).add( Restrictions.eq( "name", "e2" ) );
		assertEquals( 1, criteria.list().size() );

		txn.commit();
	}

	@Override
	protected String[] getMappings() {
		return new String[] { "hql/SimpleEntityWithAssociation.hbm.xml" };
	}

}
