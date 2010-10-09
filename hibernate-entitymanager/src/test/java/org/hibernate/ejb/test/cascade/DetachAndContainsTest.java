package org.hibernate.ejb.test.cascade;

import java.util.Collection;
import java.util.ArrayList;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.OneToMany;
import javax.persistence.ManyToOne;
import javax.persistence.EntityManager;
import static javax.persistence.CascadeType.*;

import org.hibernate.ejb.test.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class DetachAndContainsTest extends TestCase {

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

	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Mouth.class,
				Tooth.class
		};
	}

	@Entity
	public static class Mouth {
		@Id
		@GeneratedValue
		public Integer id;
		@OneToMany(mappedBy = "mouth", cascade = { DETACH, REMOVE } )
		public Collection<Tooth> teeth;
	}

	@Entity
	public static class Tooth {
		@Id
		@GeneratedValue
		public Integer id;
		public String type;
		@ManyToOne
		public Mouth mouth;
	}
}
