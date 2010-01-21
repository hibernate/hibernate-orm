package org.hibernate.test.annotations.generics;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Paolo Perrotta
 */
public class UnresolvedTypeTest extends TestCase {

	public void testAcceptsUnresolvedPropertyTypesIfATargetEntityIsExplicitlySet() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Gene item = new Gene();
		s.persist( item );
		s.flush();
		tx.rollback();
		s.close();
	}

	public void testAcceptsUnresolvedPropertyTypesIfATypeExplicitlySet() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Gene item = new Gene();
		item.setState( State.DORMANT );
		s.persist( item );
		s.flush();
		s.clear();
		item = (Gene) s.get( Gene.class, item.getId() );
		assertEquals( State.DORMANT, item.getState() );
		tx.rollback();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Gene.class,
				DNA.class
		};
	}
}
