//$Id$
package org.hibernate.ejb.test.cascade;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.hibernate.ejb.test.TestCase;


/**
 * @author Max Rydahl Andersen
 */
public class CascadeTest extends TestCase {

	public void testCascade() throws Exception {
		
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		
		Teacher teacher = null;
		
		teacher = new Teacher();

		Student student = new Student();

		teacher.setFavoriteStudent(student);
		student.setFavoriteTeacher(teacher);

		teacher.getStudents().add(student);
		student.setPrimaryTeacher(teacher);

		em.persist( teacher );
		em.getTransaction().commit();
	
		System.out.println("***************************");
		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		
		Teacher foundTeacher = (Teacher) em.createQuery( "select t from Teacher as t" ).getSingleResult();
		
		System.out.println(foundTeacher);
		System.out.println(foundTeacher.getFavoriteStudent());
		
		for (Student fstudent : foundTeacher.getStudents()) {
			System.out.println(fstudent);			
			System.out.println(fstudent.getFavoriteTeacher());
			System.out.println(fstudent.getPrimaryTeacher());
		}
		
		em.getTransaction().commit(); // here *alot* of flushes occur on an object graph that has *Zero* changes.
		em.close();
		
		
	}

	public void testNoCascadeAndMerge() throws Exception {
		Song e1 = new Song();
		Author e2 = new Author();

		e1.setAuthor(e2);

		EntityManager em = getOrCreateEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		em.persist(e2);
		em.persist(e1);
		tx.commit();
		em.close();

		em = getOrCreateEntityManager();

		e1 = em.find(Song.class, e1.getId());
		e2 = null;


		tx = em.getTransaction();
		tx.begin();
		em.merge(e1);
		//em.refresh(e1);
		tx.commit();
		em.close();

	}


	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Teacher.class,
				Student.class,
				Song.class,
				Author.class
		};
	}
	
	
}
