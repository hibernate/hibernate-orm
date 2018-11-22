/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idprops;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;

import org.hibernate.Session;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
public class IdPropertyInSubclassIdInMappedSuperclassTest extends BaseCoreFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { Human.class, Genius.class };
	}

	@org.junit.Test
	@TestForIssue( jiraKey = "HHH-13114")
	public void testHql() {
		Session s = openSession();
		s.beginTransaction();
		s.persist( new Genius() );
		s.persist( new Genius( 1L ) );
		s.persist( new Genius( 1L ) );
		s.flush();

		assertEquals(
				2, s.createQuery( "from Genius g where g.id = :id", Genius.class )
						.setParameter( "id", 1L )
						.list()
						.size()
		);

		assertEquals(
				1, s.createQuery( "from Genius g where g.id is null", Genius.class )
						.list()
						.size()
		);

		assertEquals( 3L, s.createQuery( "select count( g ) from Genius g" ).uniqueResult() );

		assertEquals(
				2, s.createQuery( "from Human h where h.id = :id", Human.class )
						.setParameter( "id", 1L )
						.list()
						.size()
		);

		assertEquals(
				1, s.createQuery( "from Human h where h.id is null", Human.class )
						.list()
						.size()
		);

		assertEquals( 3L, s.createQuery( "select count( h ) from Human h" ).uniqueResult() );

		s.createQuery( "delete from Genius" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Entity(name = "Genius")
	public static class Genius extends Human {
		private Long id;

		public Genius() {
		}

		public Genius(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

	}

	@Entity(name = "Human")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static class Human extends Animal {
	}

	@MappedSuperclass
	public static class Animal {

		private Long realId;

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@Column(name = "realId")
		public Long getRealId() {
			return realId;
		}

		public void setRealId(Long realId) {
			this.realId = realId;
		}
	}
}
