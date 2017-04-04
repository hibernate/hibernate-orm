/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
