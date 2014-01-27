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
package org.hibernate.test.annotations.indexcoll;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 */
@FailureExpectedWithNewMetamodel
public class MapKeyTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testMapKeyOnEmbeddedId() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Generation c = new Generation();
		c.setAge( "a" );
		c.setCulture( "b" );
		c.setSubGeneration( new Generation.SubGeneration( "description" ) );
		GenerationGroup r = new GenerationGroup();
		r.setGeneration( c );
		s.persist( r );
		GenerationUser m = new GenerationUser();
		s.persist( m );
		m.getRef().put( c, r );
		s.flush();
		s.clear();

		m = (GenerationUser) s.get( GenerationUser.class, m.getId() );
		Generation cRead =  m.getRef().keySet().iterator().next();
		assertEquals( "a",cRead.getAge() );
		assertEquals( "description", cRead.getSubGeneration().getDescription() );
		tx.rollback();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				GenerationUser.class,
				GenerationGroup.class
		};
	}
}
