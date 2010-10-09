package org.hibernate.test.annotations.derivedidentities.e5.a;

import org.hibernate.Session;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.util.SchemaUtil;

/**
 * @author Emmanuel Bernard
 */
public class DerivedIdentityIdClassParentSameIdTypeIdClassDepTest extends TestCase {
	private static final String FIRST_NAME = "Emmanuel";
	private static final String LAST_NAME = "Bernard";

	public void testOneToOneExplicitJoinColumn() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK1", getCfg() ) );
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK2", getCfg() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "MedicalHistory", "firstname", getCfg() ) );

		Session s = openSession();
		s.getTransaction().begin();
		Person e = new Person( FIRST_NAME, LAST_NAME );
		s.persist( e );
		MedicalHistory d = new MedicalHistory( e );
		s.persist( d );
		s.flush();
		s.refresh( d );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		PersonId pId = new PersonId( FIRST_NAME, LAST_NAME );
		MedicalHistory d2 = (MedicalHistory) s.get( MedicalHistory.class, pId );
		Person p2 = (Person) s.get( Person.class, pId );
		assertEquals( pId.firstName, d2.patient.firstName );
		assertEquals( pId.firstName, p2.firstName );
		s.delete( d2 );
		s.delete( p2 );
		s.getTransaction().commit();
		s.close();
	}

	public void testTckLikeBehavior() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK1", getCfg() ) );
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK2", getCfg() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "MedicalHistory", "firstname", getCfg() ) );

		Session s = openSession();
		s.getTransaction().begin();
		Person e = new Person( FIRST_NAME, LAST_NAME );
		s.persist( e );
		MedicalHistory d = new MedicalHistory( e );
		s.persist( d );
		s.flush();
		s.refresh( d );
		s.getTransaction().commit();

		// NOTE THAT WE LEAVE THE SESSION OPEN!

		s.getTransaction().begin();
		PersonId pId = new PersonId( FIRST_NAME, LAST_NAME );
		MedicalHistory d2 = (MedicalHistory) s.get( MedicalHistory.class, pId );
		Person p2 = (Person) s.get( Person.class, pId );
		assertEquals( pId.firstName, d2.patient.firstName );
		assertEquals( pId.firstName, p2.firstName );
		s.delete( d2 );
		s.delete( p2 );
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				MedicalHistory.class,
				Person.class
		};
	}
}
