//$Id$
package org.hibernate.test.annotations.indexcoll;

import org.hibernate.test.annotations.TestCase;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * @author Emmanuel Bernard
 */
public class MapKeyTest extends TestCase {

	public void testMapKeyOnEmbeddedId() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Generation c = new Generation();
		c.setAge( "a" );
		c.setCulture( "b" );
		GenerationGroup r = new GenerationGroup();
		r.setGeneration( c );
		s.persist( r );
		GenerationUser m = new GenerationUser();
		s.persist( m );
		m.getRef().put( c, r );
		s.flush();
		s.clear();

		m = (GenerationUser) s.get( GenerationUser.class, m.getId() );
		assertEquals( "a", m.getRef().keySet().iterator().next().getAge() );
		tx.rollback();
		s.close();
	}

	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				GenerationUser.class,
				GenerationGroup.class
		};
	}
}
