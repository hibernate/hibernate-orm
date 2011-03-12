//$Id: ComponentTest.java 11346 2007-03-26 17:24:58Z steve.ebersole@jboss.com $
package org.hibernate.test.component.basic;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Mappings;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.PersistentClass;

/**
 * @author Gavin King
 */
public class ComponentTest extends FunctionalTestCase {
	
	public ComponentTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "component/basic/User.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( ComponentTest.class );
	}

	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	public void afterConfigurationBuilt(Mappings mappings, Dialect dialect) {
		super.afterConfigurationBuilt( mappings, dialect );
		// Oracle and Postgres do not have year() functions, so we need to
		// redefine the 'User.person.yob' formula
		//
		// consider temporary until we add the capability to define
		// mapping foprmulas which can use dialect-registered functions...
		PersistentClass user = mappings.getClass( User.class.getName() );
		org.hibernate.mapping.Property personProperty = user.getProperty( "person" );
		Component component = ( Component ) personProperty.getValue();
		Formula f = ( Formula ) component.getProperty( "yob" ).getValue().getColumnIterator().next();

		SQLFunction yearFunction = ( SQLFunction ) dialect.getFunctions().get( "year" );
		if ( yearFunction == null ) {
			// the dialect not know to support a year() function, so rely on the
			// ANSI SQL extract function
			f.setFormula( "extract( year from dob )");
		}
		else {
			List args = new ArrayList();
			args.add( "dob" );
			f.setFormula( yearFunction.render( Hibernate.INTEGER, args, null ) );
		}
	}
	
	public void testUpdateFalse() {
		getSessions().getStatistics().clear();
		
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User u = new User( "gavin", "secret", new Person("Gavin King", new Date(), "Karbarook Ave") );
		s.persist(u);
		s.flush();
		u.getPerson().setName("XXXXYYYYY");
		t.commit();
		s.close();
		
		assertEquals( 1, getSessions().getStatistics().getEntityInsertCount() );
		assertEquals( 0, getSessions().getStatistics().getEntityUpdateCount() );

		s = openSession();
		t = s.beginTransaction();
		u = (User) s.get(User.class, "gavin");
		assertEquals( u.getPerson().getName(), "Gavin King" );
		s.delete(u);
		t.commit();
		s.close();
		
		assertEquals( 1, getSessions().getStatistics().getEntityDeleteCount() );
	}
	
	public void testComponent() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User u = new User( "gavin", "secret", new Person("Gavin King", new Date(), "Karbarook Ave") );
		s.persist(u);
		s.flush();
		u.getPerson().changeAddress("Phipps Place");
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		u = (User) s.get(User.class, "gavin");
		assertEquals( u.getPerson().getAddress(), "Phipps Place" );
		assertEquals( u.getPerson().getPreviousAddress(), "Karbarook Ave" );
		assertEquals( u.getPerson().getYob(), u.getPerson().getDob().getYear()+1900 );
		u.setPassword("$ecret");
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		u = (User) s.get(User.class, "gavin");
		assertEquals( u.getPerson().getAddress(), "Phipps Place" );
		assertEquals( u.getPerson().getPreviousAddress(), "Karbarook Ave" );
		assertEquals( u.getPassword(), "$ecret" );
		s.delete(u);
		t.commit();
		s.close();
	}

	public void testComponentStateChangeAndDirtiness() {
		// test for HHH-2366
		Session s = openSession();
		s.beginTransaction();
		User u = new User( "steve", "hibernater", new Person( "Steve Ebersole", new Date(), "Main St") );
		s.persist( u );
		s.flush();
		long intialUpdateCount = sfi().getStatistics().getEntityUpdateCount();
		u.getPerson().setAddress( "Austin" );
		s.flush();
		assertEquals( intialUpdateCount + 1, sfi().getStatistics().getEntityUpdateCount() );
		intialUpdateCount = sfi().getStatistics().getEntityUpdateCount();
		u.getPerson().setAddress( "Cedar Park" );
		s.flush();
		assertEquals( intialUpdateCount + 1, sfi().getStatistics().getEntityUpdateCount() );
		s.delete( u );
		s.getTransaction().commit();
		s.close();
	}

	public void testComponentQueries() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Employee emp = new Employee();
		emp.setHireDate( new Date() );
		emp.setPerson( new Person() );
		emp.getPerson().setName( "steve" );
		emp.getPerson().setDob( new Date() );
		s.save( emp );

		s.createQuery( "from Employee e where e.person = :p and 1 = 1 and 2=2" ).setParameter( "p", emp.getPerson() ).list();
		s.createQuery( "from Employee e where :p = e.person" ).setParameter( "p", emp.getPerson() ).list();
		// The following fails on Sybase due to HHH-3510. When HHH-3510 
		// is fixed, the check for SybaseASE15Dialect should be removed.
		if ( ! ( getDialect() instanceof SybaseASE15Dialect ) ) {
			s.createQuery( "from Employee e where e.person = ('steve', current_timestamp)" ).list();
		}

		s.delete( emp );
		t.commit();
		s.close();
	}

	// Sybase should translate "current_timestamp" in HQL to "getdate()";
	// This fails currently due to HHH-3510. The following test should be 
	// deleted and testComponentQueries() should be updated (as noted
	// in that test case) when HHH-3510 is fixed.
	public void testComponentQueryMethodNoParensFailureExpected() {
		if ( ! ( getDialect() instanceof SybaseASE15Dialect ) ) {
			fail( "Dialect does not apply to test that is expected to fail; force failure" );
		}
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Employee emp = new Employee();
		emp.setHireDate( new Date() );
		emp.setPerson( new Person() );
		emp.getPerson().setName( "steve" );
		emp.getPerson().setDob( new Date() );
		s.save( emp );
		s.createQuery( "from Employee e where e.person = ('steve', current_timestamp)" ).list();
		s.delete( emp );
		t.commit();
		s.close();
	}
	
	public void testComponentFormulaQuery() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.createQuery("from User u where u.person.yob = 1999").list();
		s.createCriteria(User.class)
			.add( Property.forName("person.yob").between( new Integer(1999), new Integer(2002) ) )
			.list();
		if ( getDialect().supportsRowValueConstructorSyntax() ) {
			s.createQuery("from User u where u.person = ('gavin', :dob, 'Peachtree Rd', 'Karbarook Ave', 1974, 34, 'Peachtree Rd')")
				.setDate("dob", new Date("March 25, 1974")).list();
			s.createQuery("from User where person = ('gavin', :dob, 'Peachtree Rd', 'Karbarook Ave', 1974, 34, 'Peachtree Rd')")
				.setDate("dob", new Date("March 25, 1974")).list();
		}
		t.commit();
		s.close();
	}
	
	public void testCustomColumnReadAndWrite() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User u = new User( "steve", "hibernater", new Person( "Steve Ebersole", new Date(), "Main St") );
		final double HEIGHT_INCHES = 73;
		final double HEIGHT_CENTIMETERS = HEIGHT_INCHES * 2.54d;
		u.getPerson().setHeightInches(HEIGHT_INCHES);
		s.persist( u );
		s.flush();
		
		// Test value conversion during insert
		Double heightViaSql = (Double)s.createSQLQuery("select height_centimeters from t_user where t_user.username='steve'").uniqueResult();
		assertEquals(HEIGHT_CENTIMETERS, heightViaSql, 0.01d);

		// Test projection
		Double heightViaHql = (Double)s.createQuery("select u.person.heightInches from User u where u.id = 'steve'").uniqueResult();
		assertEquals(HEIGHT_INCHES, heightViaHql, 0.01d);
		
		// Test restriction and entity load via criteria
		u = (User)s.createCriteria(User.class)
			.add(Restrictions.between("person.heightInches", HEIGHT_INCHES - 0.01d, HEIGHT_INCHES + 0.01d))
			.uniqueResult();
		assertEquals(HEIGHT_INCHES, u.getPerson().getHeightInches(), 0.01d);
		
		// Test predicate and entity load via HQL
		u = (User)s.createQuery("from User u where u.person.heightInches between ? and ?")
			.setDouble(0, HEIGHT_INCHES - 0.01d)
			.setDouble(1, HEIGHT_INCHES + 0.01d)
			.uniqueResult();
		assertEquals(HEIGHT_INCHES, u.getPerson().getHeightInches(), 0.01d);
		
		// Test update
		u.getPerson().setHeightInches(1);
		s.flush();
		heightViaSql = (Double)s.createSQLQuery("select height_centimeters from t_user where t_user.username='steve'").uniqueResult();
		assertEquals(2.54d, heightViaSql, 0.01d);
		s.delete(u);
		t.commit();
		s.close();
	}
	

	public void testNamedQuery() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.getNamedQuery("userNameIn")
			.setParameterList( "nameList", new Object[] {"1ovthafew", "turin", "xam"} )
			.list();
		t.commit();
		s.close();
	}

	public void testMergeComponent() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Employee emp = new Employee();
		emp.setHireDate( new Date() );
		emp.setPerson( new Person() );
		emp.getPerson().setName( "steve" );
		emp.getPerson().setDob( new Date() );
		s.persist( emp );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		emp = (Employee)s.get( Employee.class, emp.getId() );
		t.commit();
		s.close();

		assertNull(emp.getOptionalComponent());
		emp.setOptionalComponent( new OptionalComponent() );
		emp.getOptionalComponent().setValue1( "emp-value1" );
		emp.getOptionalComponent().setValue2( "emp-value2" );

		s = openSession();
		t = s.beginTransaction();
		emp = (Employee)s.merge( emp );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		emp = (Employee)s.get( Employee.class, emp.getId() );
		t.commit();
		s.close();

		assertEquals("emp-value1", emp.getOptionalComponent().getValue1());
		assertEquals("emp-value2", emp.getOptionalComponent().getValue2());
		emp.getOptionalComponent().setValue1( null );
		emp.getOptionalComponent().setValue2( null );

		s = openSession();
		t = s.beginTransaction();
		emp = (Employee)s.merge( emp );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		emp = (Employee)s.get( Employee.class, emp.getId() );
		Hibernate.initialize(emp.getDirectReports());
		t.commit();
		s.close();

		assertNull(emp.getOptionalComponent());

		Employee emp1 = new Employee();
		emp1.setHireDate( new Date() );
		emp1.setPerson( new Person() );
		emp1.getPerson().setName( "bozo" );
		emp1.getPerson().setDob( new Date() );
		emp.getDirectReports().add( emp1 );

		s = openSession();
		t = s.beginTransaction();
		emp = (Employee)s.merge( emp );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		emp = (Employee)s.get( Employee.class, emp.getId() );
		Hibernate.initialize(emp.getDirectReports());
		t.commit();
		s.close();

		assertEquals(1, emp.getDirectReports().size());
		emp1 = (Employee)emp.getDirectReports().iterator().next();
		assertNull( emp1.getOptionalComponent() );
		emp1.setOptionalComponent( new OptionalComponent() );
		emp1.getOptionalComponent().setValue1( "emp1-value1" );
		emp1.getOptionalComponent().setValue2( "emp1-value2" );

		s = openSession();
		t = s.beginTransaction();
		emp = (Employee)s.merge( emp );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		emp = (Employee)s.get( Employee.class, emp.getId() );
		Hibernate.initialize(emp.getDirectReports());
		t.commit();
		s.close();

		assertEquals(1, emp.getDirectReports().size());
		emp1 = (Employee)emp.getDirectReports().iterator().next();
		assertEquals( "emp1-value1", emp1.getOptionalComponent().getValue1());
		assertEquals( "emp1-value2", emp1.getOptionalComponent().getValue2());
		emp1.getOptionalComponent().setValue1( null );
		emp1.getOptionalComponent().setValue2( null );

		s = openSession();
		t = s.beginTransaction();
		emp = (Employee)s.merge( emp );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		emp = (Employee)s.get( Employee.class, emp.getId() );
		Hibernate.initialize(emp.getDirectReports());
		t.commit();
		s.close();

		assertEquals(1, emp.getDirectReports().size());
		emp1 = (Employee)emp.getDirectReports().iterator().next();
		assertNull(emp1.getOptionalComponent());

		s = openSession();
		t = s.beginTransaction();
		s.delete( emp );
		t.commit();
		s.close();
	}

}

