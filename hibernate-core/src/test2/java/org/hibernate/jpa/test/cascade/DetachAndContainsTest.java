/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.cascade;

import java.util.ArrayList;
import java.util.Collection;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import static javax.persistence.CascadeType.DETACH;
import static javax.persistence.CascadeType.REMOVE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
public class DetachAndContainsTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testDetach() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Tooth tooth = new Tooth();
		Mouth mouth = new Mouth();
		em.persist( mouth );
		em.persist( tooth );
		tooth.mouth = mouth;
		mouth.teeth = new ArrayList<Tooth>();
		mouth.teeth.add( tooth );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		mouth = em.find( Mouth.class, mouth.id );
		assertNotNull( mouth );
		assertEquals( 1, mouth.teeth.size() );
		tooth = mouth.teeth.iterator().next();
		em.detach( mouth );
		assertFalse( em.contains( tooth ) );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.remove( em.find( Mouth.class, mouth.id ) );

		em.getTransaction().commit();
		em.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Mouth.class,
				Tooth.class
		};
	}

	@Entity
	@Table(name = "mouth")
	public static class Mouth {
		@Id
		@GeneratedValue
		public Integer id;
		@OneToMany(mappedBy = "mouth", cascade = { DETACH, REMOVE } )
		public Collection<Tooth> teeth;
	}

	@Entity
	@Table(name = "tooth")
	public static class Tooth {
		@Id
		@GeneratedValue
		public Integer id;
		public String type;
		@ManyToOne
		public Mouth mouth;
	}
}
