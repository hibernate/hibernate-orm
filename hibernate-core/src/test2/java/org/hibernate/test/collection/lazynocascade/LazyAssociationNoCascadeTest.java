/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.lazynocascade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Vasily Kochnev
 */
public class LazyAssociationNoCascadeTest extends BaseCoreFunctionalTestCase {
	
	public String[] getMappings() {
		return new String[] {"collection/lazynocascade/Parent.hbm.xml"};
	}

	@Test
	public void testNoCascadeCache() {
		Parent parent = new Parent();

		BaseChild firstChild = new BaseChild();
		parent.getChildren().add( firstChild );
		
		Session s = openSession();
		s.beginTransaction();
		s.save(parent);
		s.getTransaction().commit();
		s.clear();

		Child secondChild = new Child();
		secondChild.setName( "SecondChildName" );
		parent.getChildren().add( secondChild );

		firstChild.setDependency( secondChild );

		s.beginTransaction();
		Parent mergedParent = (Parent) s.merge( parent );
		s.getTransaction().commit();
		s.close();
		
		assertNotNull( mergedParent );
		assertEquals( mergedParent.getChildren().size(), 2 );
	}
}
