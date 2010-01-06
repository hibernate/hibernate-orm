package org.hibernate.ejb.test.ops;

import org.hibernate.ejb.test.TestCase;

import javax.persistence.EntityManager;
import java.util.HashSet;
import java.util.Iterator;

public class OrderByTest extends TestCase {

	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Products.class,
				Widgets.class
		};
	}

	/**
	 * Test @OrderBy on the Widgets.name field.
	 *
	 */
	public void testOrderByName() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Products p = new Products();
		HashSet<Widgets> set = new HashSet<Widgets>();

		Widgets widget = new Widgets();
		widget.setName("hammer");
		set.add(widget);
		em.persist(widget);

		widget = new Widgets();
		widget.setName("axel");
		set.add(widget);
		em.persist(widget);

		widget = new Widgets();
		widget.setName("screwdriver");
		set.add(widget);
		em.persist(widget);

		p.setWidgets(set);
		em.persist(p);
		em.getTransaction().commit();

		em.getTransaction().begin();
		em.clear();
		p = em.find(Products.class,p.getId());
		assertTrue("has three Widgets", p.getWidgets().size() == 3);
		Iterator iter = p.getWidgets().iterator();
		assertEquals( "axel", ((Widgets)iter.next()).getName() );
		assertEquals( "hammer", ((Widgets)iter.next()).getName() );
		assertEquals( "screwdriver", ((Widgets)iter.next()).getName() );
		em.getTransaction().commit();
		em.close();
	}


}
