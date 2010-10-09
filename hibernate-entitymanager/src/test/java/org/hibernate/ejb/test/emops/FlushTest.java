//$Id$
package org.hibernate.ejb.test.emops;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import javax.persistence.Query;
import javax.persistence.EntityManager;

import org.hibernate.ejb.test.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class FlushTest extends TestCase {
	private static Set<String> names= new HashSet<String>();
	static {
		names.add("Toonses");
		names.add("Sox");
		names.add("Winnie");
		names.add("Junior");
	}

	//Test for EJBTHREE-722
	public void testFlushOnDetached() throws Exception {
		EntityManager manager = getOrCreateEntityManager( );

		manager.getTransaction().begin();
		Pet p1 = createCat("Toonses", 15.0, 9, manager);
		manager.flush();
		manager.clear();

		Pet p2 = createCat("Sox", 10.0, 5, manager);
		manager.flush();
		manager.clear();

		Pet p3 = createDog("Winnie", 70.0, 5, manager);
		manager.flush();
		manager.clear();

		Pet p4 = createDog("Junior", 11.0, 1, manager);
		manager.flush();
		manager.clear();

		Decorate d1 = createDecorate("Test", p1, manager);
		manager.flush();
		manager.clear();

		Decorate d2 = createDecorate("Test2", p2, manager);
		manager.flush();
		manager.clear();

		List l = findByWeight(14.0, manager);
		manager.flush();
		manager.clear();
		for (Object o : l) {
			assertTrue( names.contains( ( (Pet) o).getName() ) );
		}

		Collection<Decorate> founds = getDecorate(manager);
		manager.flush();
		manager.clear();
		for (Decorate value : founds) {
			assertTrue( names.contains( value.getPet().getName() ) );
		}
		manager.getTransaction().rollback();
		
		manager.close();
		
	}

	public Dog createDog(String name, double weight, int bones, EntityManager manager) {
		Dog dog = new Dog();
		dog.setName(name);
		dog.setWeight(weight);
		dog.setNumBones(bones);
		manager.persist(dog);
		return dog;
	}

	public Cat createCat(String name, double weight, int lives, EntityManager manager) {
		Cat cat = new Cat();
		cat.setName(name);
		cat.setWeight(weight);
		cat.setLives(lives);
		manager.persist(cat);
		return cat;
	}

	public List findByWeight(double weight, EntityManager manager) {
		return manager.createQuery(
				"select p from Pet p where p.weight < :weight").setParameter(
				"weight", weight).getResultList();
	}

	public Decorate createDecorate(String name, Pet pet, EntityManager manager) {
		Decorate dec = new Decorate();
		dec.setName(name);
		dec.setPet(pet);
		manager.persist(dec);
		return dec;
	}

	public Collection<Decorate> getDecorate(EntityManager manager) {
		Collection<Decorate> founds = new ArrayList<Decorate>();
		Query query = manager.createQuery("SELECT o FROM Decorate o");
		List list = query.getResultList();
		for (Object obj : list) {
			if (obj instanceof Decorate) {
				Decorate decorate = (Decorate) obj;
				founds.add( decorate );
				decorate.getPet().getName(); //load
			}
		}
		return founds;
	}

	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Pet.class,
				Dog.class,
				Cat.class,
				Decorate.class
		};
	}
}
