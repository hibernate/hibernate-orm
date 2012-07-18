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
package org.hibernate.jpa.test.cascade;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

/**
 * @author Max Rydahl Andersen
 */
public class CascadeTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testCascade() throws Exception {
		
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		
		Teacher teacher = new Teacher();

		Student student = new Student();

		teacher.setFavoriteStudent(student);
		student.setFavoriteTeacher(teacher);

		teacher.getStudents().add(student);
		student.setPrimaryTeacher(teacher);

		em.persist( teacher );
		em.getTransaction().commit();

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

	@Test
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


		tx = em.getTransaction();
		tx.begin();
		em.merge(e1);
		//em.refresh(e1);
		tx.commit();
		em.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Teacher.class,
				Student.class,
				Song.class,
				Author.class
		};
	}


}
