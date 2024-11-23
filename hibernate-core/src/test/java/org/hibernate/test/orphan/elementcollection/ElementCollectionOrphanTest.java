package org.hibernate.test.orphan.elementcollection;

import java.util.Collections;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Before;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-14597")
public class ElementCollectionOrphanTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] { "orphan/elementcollection/student.hbm.xml" };
	}

	@Test
	public void setCompositeElementTest() {
		TransactionUtil.doInHibernate(
			this::sessionFactory,
			session -> {
				EnrollableClass aClass = new EnrollableClass();
				aClass.setId("123");
				aClass.setName("Math");
				session.save(aClass);

				Student aStudent = new Student();
				aStudent.setId("s1");
				aStudent.setFirstName("John");
				aStudent.setLastName("Smith");

				EnrolledClassSeat seat = new EnrolledClassSeat();
				seat.setId("seat1");
				seat.setRow(10);
				seat.setColumn(5);

				StudentEnrolledClass enrClass = new StudentEnrolledClass();
				enrClass.setEnrolledClass(aClass);
				enrClass.setClassStartTime(130);
				enrClass.setSeat(seat);
				aStudent.setEnrolledClasses(Collections.singleton(enrClass));
				session.save(aStudent);
			}
		);
	}
}
