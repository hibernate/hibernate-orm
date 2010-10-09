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
 */
package org.hibernate.test.entitymode.map.subclass;

import java.util.HashMap;

import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.Session;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class SubclassDynamicMapTest extends FunctionalTestCase {
	public SubclassDynamicMapTest(String string) {
		super( string );
	}

	public String[] getMappings() {
		return new String[] { "entitymode/map/subclass/Mappings.hbm.xml" };
	}

	public void testConcreateSubclassDeterminationOnEmptyDynamicMap() {
		Session s = openSession();
		s.beginTransaction();
		s.persist( "Superclass", new HashMap() );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete Superclass" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}
}
