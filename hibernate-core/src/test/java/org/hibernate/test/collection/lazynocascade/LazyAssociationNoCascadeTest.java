/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.collection.lazynocascade;

import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
