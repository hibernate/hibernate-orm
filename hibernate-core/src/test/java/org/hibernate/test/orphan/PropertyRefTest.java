/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.orphan;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;


/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-565" )
public class PropertyRefTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "orphan/User.hbm.xml", "orphan/Mail.hbm.xml" };
	}

	@Test
	public void testDeleteParentWithBidirOrphanDeleteCollectionBasedOnPropertyRef() {
		Session session = openSession();
		Transaction txn = session.beginTransaction();
		User user = new User( "test" );
		user.addMail( "test" );
		user.addMail( "test" );
		session.save( user );
		txn.commit();
		session.close();

		session = openSession();
		txn = session.beginTransaction();
		user = ( User ) session.load( User.class, user.getId() );
		session.delete( user );
		txn.commit();
		session.close();

		session = openSession();
		txn = session.beginTransaction();
		session.createQuery( "delete from Mail where alias = :alias" ).setString( "alias", "test" ).executeUpdate();
		session.createQuery( "delete from User where userid = :userid" ).setString( "userid", "test" ).executeUpdate();
		txn.commit();
		session.close();
	}
	
}
