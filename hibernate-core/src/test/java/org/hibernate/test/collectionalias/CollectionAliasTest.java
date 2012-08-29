/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.collectionalias;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Dave Stephan
 * @author Gail Badner
 */
public class CollectionAliasTest extends BaseCoreFunctionalTestCase {

	@TestForIssue( jiraKey = "HHH-7545" )
	@Test
	public void test() {
		Session s = openSession();
		s.getTransaction().begin();
		ATable aTable = new ATable( 1 );
		TableB tableB = new TableB(
			new TableBId( 1, "a", "b" )
		);
		aTable.getTablebs().add( tableB );
		tableB.setTablea( aTable );
		s.save( aTable );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		aTable = (ATable) s.createQuery( "select distinct	tablea from ATable tablea LEFT JOIN FETCH tablea.tablebs " ).uniqueResult();
		assertEquals( new Integer( 1 ), aTable.getFirstId() );
		assertEquals( 1, aTable.getTablebs().size() );
		tableB = aTable.getTablebs().get( 0 );
		assertSame( aTable, tableB.getTablea() );
		assertEquals( new Integer( 1 ), tableB.getId().getFirstId() );
		assertEquals( "a", tableB.getId().getSecondId() );
		assertEquals( "b", tableB.getId().getThirdId() );
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				TableBId.class,
				TableB.class,
				TableA.class,
				ATable.class
		};
	}

}
