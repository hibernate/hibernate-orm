//$Id: CustomSQLTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.legacy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.id.PostInsertIdentifierGenerator;
import org.hibernate.testing.DialectCheck;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.junit.Test;

/**
 * @author MAX
 *
 */
@FailureExpectedWithNewMetamodel
public class CustomSQLTest extends LegacyTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "legacy/CustomSQL.hbm.xml" };
	}

    public static class NonIdentityGeneratorChecker implements DialectCheck {
        @Override
        public boolean isMatch(Dialect dialect) {
            return !PostInsertIdentifierGenerator.class.isAssignableFrom( getDialect().getNativeIdentifierGeneratorClass() );
        }
    }

	@Test
    @RequiresDialectFeature( NonIdentityGeneratorChecker.class )
    @SkipForDialect( value = {PostgreSQL81Dialect.class}, jiraKey = "HHH-6704")
	public void testInsert() throws HibernateException, SQLException {
		Session s = openSession();
		s.beginTransaction();
		Role p = new Role();
		p.setName("Patient");
		s.save( p );
		s.getTransaction().commit();
		s.close();

		sessionFactory().getCache().evictEntityRegion( Role.class );

		s = openSession();
		s.beginTransaction();
		Role p2 = (Role) s.get(Role.class, Long.valueOf(p.getId()));
		assertNotSame(p, p2);
		assertEquals(p2.getId(),p.getId());
		assertTrue(p2.getName().equalsIgnoreCase(p.getName()));
		s.delete(p2);
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testJoinedSubclass() throws HibernateException, SQLException {
		Session s = openSession();
		s.beginTransaction();
		Medication m = new Medication();
		m.setPrescribedDrug(new Drug());
		m.getPrescribedDrug().setName( "Morphine" );
		s.save( m.getPrescribedDrug() );
		s.save( m );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		Medication m2  = (Medication) s.get(Medication.class, m.getId());
		assertNotSame(m, m2);
		s.getTransaction().commit();
		s.close();
	}

//	@Test
//    @RequiresDialectFeature( NonIdentityGeneratorChecker.class )
	public void testCollectionCUD() throws HibernateException, SQLException {
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
		s.beginTransaction();
		s.save(role);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		Role r = (Role) s.get(Role.class, Long.valueOf(role.getId()));
		assertNotSame(role,r);
		assertEquals(1,r.getInterventions().size());
		assertEquals(3, r.getBunchOfStrings().size());
		r.getBunchOfStrings().set(1, "replacement");
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		r = (Role) s.get(Role.class,new Long(role.getId()));
		assertNotSame(role,r);

		assertEquals(r.getBunchOfStrings().get(1),"replacement");
		assertEquals(3, r.getBunchOfStrings().size());

		r.getBunchOfStrings().set(1, "replacement");

		r.getBunchOfStrings().remove(1);
		s.flush();

		r.getBunchOfStrings().clear();
		s.getTransaction().commit();
		s.close();
	}

//	@Test
//    @RequiresDialectFeature( NonIdentityGeneratorChecker.class )
	public void testCRUD() throws HibernateException, SQLException {
		Person p = new Person();
		p.setName("Max");
		p.setLastName("Andersen");
		p.setNationalID("110974XYZ");
		p.setAddress("P. P. Street 8");

		Session s = openSession();
		s.beginTransaction();
		s.save(p);
		s.getTransaction().commit();
		s.close();

		sessionFactory().getCache().evictEntityRegion( Person.class );

		s = openSession();
		s.beginTransaction();
		Person p2 = (Person) s.get(Person.class, p.getId());
		assertNotSame(p, p2);
		assertEquals(p2.getId(),p.getId());
		assertEquals(p2.getLastName(),p.getLastName());
		s.flush();

		List list = s.createQuery( "select p from Party as p" ).list();
		assertTrue(list.size() == 1);

		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		list = s.createQuery( "select p from Person as p where p.address = 'Lrkevnget 1'" ).list();
		assertTrue(list.size() == 0);
		p.setAddress("Lrkevnget 1");
		s.update(p);
		list = s.createQuery( "select p from Person as p where p.address = 'Lrkevnget 1'" ).list();
		assertTrue(list.size() == 1);
		list = s.createQuery( "select p from Party as p where p.address = 'P. P. Street 8'" ).list();
		assertTrue(list.size() == 0);

		s.delete(p);
		list = s.createQuery( "select p from Person as p" ).list();
		assertTrue(list.size() == 0);

		s.getTransaction().commit();
		s.close();
	}
}
