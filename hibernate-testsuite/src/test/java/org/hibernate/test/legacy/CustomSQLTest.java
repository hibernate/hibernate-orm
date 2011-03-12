//$Id: CustomSQLTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.legacy;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import org.hibernate.HibernateException;
import org.hibernate.id.PostInsertIdentifierGenerator;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.classic.Session;

/**
 * @author MAX
 *
 */
public class CustomSQLTest extends LegacyTestCase {

	public CustomSQLTest(String name) {
		super(name);
	}

	public String[] getMappings() {
		return new String[] { "legacy/CustomSQL.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( CustomSQLTest.class );
	}

	private boolean isUsingIdentity() {
		return PostInsertIdentifierGenerator.class.isAssignableFrom( getDialect().getNativeIdentifierGeneratorClass() );
	}

	public void testInsert() throws HibernateException, SQLException {
		if ( isUsingIdentity() ) {
			reportSkip( "hand sql expecting non-identity id gen", "Custom SQL" );
			return;
		}

		Role p = new Role();

		p.setName("Patient");

		Session s = openSession();

		s.save(p);
		s.flush();

		s.connection().commit();
		s.close();

		getSessions().evict(Role.class);
		s = openSession();

		Role p2 = (Role) s.get(Role.class, new Long(p.getId()));
		assertNotSame(p, p2);
		assertEquals(p2.getId(),p.getId());
		assertTrue(p2.getName().equalsIgnoreCase(p.getName()));
		s.delete(p2);
		s.flush();


		s.connection().commit();
		s.close();


	}

	public void testJoinedSubclass() throws HibernateException, SQLException {
		Medication m = new Medication();

		m.setPrescribedDrug(new Drug());

		m.getPrescribedDrug().setName("Morphine");


		Session s = openSession();

		s.save(m.getPrescribedDrug());
		s.save(m);

		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();

		Medication m2  = (Medication) s.get(Medication.class, m.getId());
		assertNotSame(m, m2);

		s.flush();
		s.connection().commit();
		s.close();

	}

	public void testCollectionCUD() throws HibernateException, SQLException {
		if ( isUsingIdentity() ) {
			reportSkip( "hand sql expecting non-identity id gen", "Custom SQL" );
			return;
		}

		Role role = new Role();

		role.setName("Jim Flanders");

		Intervention iv = new Medication();
		iv.setDescription("JF medical intervention");

		role.getInterventions().add(iv);

		List sx = new ArrayList();
		sx.add("somewhere");
		sx.add("somehow");
		sx.add("whatever");
		role.setBunchOfStrings(sx);

		Session s = openSession();

		s.save(role);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();

		Role r = (Role) s.get(Role.class,new Long(role.getId()));
		assertNotSame(role,r);

		assertEquals(1,r.getInterventions().size());

		assertEquals(3, r.getBunchOfStrings().size());

		r.getBunchOfStrings().set(1, "replacement");
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();

		r = (Role) s.get(Role.class,new Long(role.getId()));
		assertNotSame(role,r);

		assertEquals(r.getBunchOfStrings().get(1),"replacement");
		assertEquals(3, r.getBunchOfStrings().size());

		r.getBunchOfStrings().set(1, "replacement");

		r.getBunchOfStrings().remove(1);
		s.flush();

		r.getBunchOfStrings().clear();
		s.flush();

		s.connection().commit();
		s.close();

	}

	public void testCRUD() throws HibernateException, SQLException {
		if ( isUsingIdentity() ) {
			reportSkip( "hand sql expecting non-identity id gen", "Custom SQL" );
			return;
		}

		Person p = new Person();

		p.setName("Max");
		p.setLastName("Andersen");
		p.setNationalID("110974XYZ�");
		p.setAddress("P. P. Street 8");

		Session s = openSession();

		s.save(p);
		s.flush();

		s.connection().commit();
		s.close();

		getSessions().evict(Person.class);
		s = openSession();

		Person p2 = (Person) s.get(Person.class, p.getId());
		assertNotSame(p, p2);
		assertEquals(p2.getId(),p.getId());
		assertEquals(p2.getLastName(),p.getLastName());
		s.flush();

		List list = s.createQuery( "select p from Party as p" ).list();
		assertTrue(list.size() == 1);

		s.connection().commit();
		s.close();

		s = openSession();

		list = s.createQuery( "select p from Person as p where p.address = 'L�rkev�nget 1'" ).list();
		assertTrue(list.size() == 0);
		p.setAddress("L�rkev�nget 1");
		s.update(p);
		list = s.createQuery( "select p from Person as p where p.address = 'L�rkev�nget 1'" ).list();
		assertTrue(list.size() == 1);
		list = s.createQuery( "select p from Party as p where p.address = 'P. P. Street 8'" ).list();
		assertTrue(list.size() == 0);

		s.delete(p);
		list = s.createQuery( "select p from Person as p" ).list();
		assertTrue(list.size() == 0);

		s.connection().commit();
		s.close();
	}
}
