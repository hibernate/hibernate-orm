package org.hibernate.query.hhh14112;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.annotations.Where;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Ganesh Tiwari
 * @author Nathan Xu
 */
@TestForIssue(jiraKey = "HHH-14112")
public class HHH14112Test extends BaseCoreFunctionalTestCase {

	@Test
	public void testCountSubObjectNotThrownExceptionBecauseOfWhere() {
		doInJPA(this::sessionFactory, em -> {
			em.createQuery( "SELECT count(*) FROM SubObject", Long.class).getSingleResult();
		});
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { HHH14112Test.Super.class, HHH14112Test.SubObject.class };
	}

	@Entity(name = "Super")
	@Inheritance(strategy = InheritanceType.JOINED)
	@Where(clause = "DELETED = false")
	public static class Super {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Long id;

		public boolean deleted;
	}

	@Entity(name = "SubObject")
	public static class SubObject extends Super {

		public String name;

		public int age;

		public SubObject() {
		}

		public SubObject(String name, int age) {
			this.name = name;
			this.age = age;
		}

	}
}
