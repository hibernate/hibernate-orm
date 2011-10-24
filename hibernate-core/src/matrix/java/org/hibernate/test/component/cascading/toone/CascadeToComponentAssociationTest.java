/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.component.cascading.toone;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Steve Ebersole
 */
public class CascadeToComponentAssociationTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "component/cascading/toone/Mappings.hbm.xml" };
	}

	@Test
	public void testMerging() {
		// step1, we create a document with owner
		Session session = openSession();
		session.beginTransaction();
		User user = new User();
		Document document = new Document();
		document.setOwner( user );
		session.persist( document );
		session.getTransaction().commit();
		session.close();

		// step2, we verify that the document has owner and that owner has no personal-info; then we detach
		session = openSession();
		session.beginTransaction();
		document = ( Document ) session.get( Document.class, document.getId() );
		assertNotNull( document.getOwner() );
		assertNull( document.getOwner().getPersonalInfo() );
		session.getTransaction().commit();
		session.close();

		// step3, try to specify the personal-info during detachment
		Address addr = new Address();
		addr.setStreet1( "123 6th St" );
		addr.setCity( "Austin" );
		addr.setState( "TX" );
		document.getOwner().setPersonalInfo( new PersonalInfo( addr ) );

		// step4 we merge the document
		session = openSession();
		session.beginTransaction();
		session.merge( document );
		session.getTransaction().commit();
		session.close();

		// step5, final test
		session = openSession();
		session.beginTransaction();
		document = ( Document ) session.get( Document.class, document.getId() );
		assertNotNull( document.getOwner() );
		assertNotNull( document.getOwner().getPersonalInfo() );
		assertNotNull( document.getOwner().getPersonalInfo().getHomeAddress() );
		session.getTransaction().commit();
		session.close();
	}
}
