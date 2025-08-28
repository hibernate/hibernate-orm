/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.indexcoll;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 */
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
