/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.component.basic;

import java.util.Date;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.query.common.TemporalUnit;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Gavin King
 */
public class ComponentTest extends BaseSessionFactoryFunctionalTest {

	@Override
	public String[] getOrmXmlFiles() {
		return new String[] { "org/hibernate/orm/test/component/basic/User.hbm.xml" };
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builder) {
		builder.applySetting( Environment.GENERATE_STATISTICS, "true" );
	}

	@Override
	public MetadataImplementor produceModel(StandardServiceRegistry serviceRegistry) {
		MetadataImplementor metadata = super.produceModel( serviceRegistry );
		// Oracle and Postgres do not have year() functions, so we need to
		// redefine the 'User.person.yob' formula
		//
		// consider temporary until we add the capability to define
		// mapping formulas which can use dialect-registered functions...
		PersistentClass user = metadata.getEntityBinding( User.class.getName() );
		org.hibernate.mapping.Property personProperty = user.getProperty( "person" );
		Component component = ( Component ) personProperty.getValue();
		Formula f = ( Formula ) component.getProperty( "yob" ).getValue().getSelectables().get( 0 );

		String pattern = metadata.getDatabase().getJdbcEnvironment().getDialect().extractPattern( TemporalUnit.YEAR );
		String formula = pattern.replace( "?1", "YEAR" ).replace( "?2", "dob" );
		f.setFormula( formula );
		return metadata;
	}

	@Test
	public void testUpdateFalse() {
		sessionFactory().getStatistics().clear();

		inTransaction(
				s -> {
					User u = new User( "gavin", "secret", new Person("Gavin King", new Date(), "Karbarook Ave") );
					s.persist(u);
					s.flush();
					u.getPerson().setName("XXXXYYYYY");
				}
		);

		assertEquals( 1, sessionFactory().getStatistics().getEntityInsertCount() );
		assertEquals( 0, sessionFactory().getStatistics().getEntityUpdateCount() );

		inTransaction(
				s -> {
					User u = s.get(User.class, "gavin");
					assertEquals( u.getPerson().getName(), "Gavin King" );
					s.remove(u);
				}
		);

		assertEquals( 1, sessionFactory().getStatistics().getEntityDeleteCount() );
	}
	

	@Test
	public void testComponent() {
		inTransaction(
				s -> {
					User u = new User( "gavin", "secret", new Person("Gavin King", new Date(), "Karbarook Ave") );
					s.persist(u);
					s.flush();
					u.getPerson().changeAddress("Phipps Place");
				}
		);

		inTransaction(
				s -> {
					User u = s.get(User.class, "gavin");
					assertEquals( u.getPerson().getAddress(), "Phipps Place" );
					assertEquals( u.getPerson().getPreviousAddress(), "Karbarook Ave" );
					assertEquals( u.getPerson().getYob(), u.getPerson().getDob().getYear()+1900 );
					u.setPassword("$ecret");
				}
		);

		inTransaction(
				s -> {
					User u = s.get(User.class, "gavin");
					assertEquals( u.getPerson().getAddress(), "Phipps Place" );
					assertEquals( u.getPerson().getPreviousAddress(), "Karbarook Ave" );
					assertEquals( u.getPassword(), "$ecret" );
					s.remove(u);
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-2366" )
	public void testComponentStateChangeAndDirtiness() {
		inTransaction(
				s -> {
					User u = new User( "steve", "hibernater", new Person( "Steve Ebersole", new Date(), "Main St") );
					s.persist( u );
					s.flush();
					long intialUpdateCount = sessionFactory().getStatistics().getEntityUpdateCount();
					u.getPerson().setAddress( "Austin" );
					s.flush();
					assertEquals( intialUpdateCount + 1, sessionFactory().getStatistics().getEntityUpdateCount() );
					intialUpdateCount = sessionFactory().getStatistics().getEntityUpdateCount();
					u.getPerson().setAddress( "Cedar Park" );
					s.flush();
					assertEquals( intialUpdateCount + 1, sessionFactory().getStatistics().getEntityUpdateCount() );
					s.remove( u );
				}
		);
	}

	@Test
	public void testComponentQueries() {
		inTransaction(
				s -> {
					Employee emp = new Employee();
					emp.setHireDate( new Date() );
					emp.setPerson( new Person() );
					emp.getPerson().setName( "steve" );
					emp.getPerson().setDob( new Date() );
					s.persist( emp );

					s.createQuery( "from Employee e where e.person = :p and 1 = 1 and 2=2" ).setParameter( "p", emp.getPerson() ).list();
					s.createQuery( "from Employee e where :p = e.person" ).setParameter( "p", emp.getPerson() ).list();
					s.createQuery( "from Employee e where e.person = ('', '', current_timestamp, 0.0, 'steve', '', 0)" ).list();

					s.remove( emp );
				}
		);
	}

	@Test
	@RequiresDialect( value = SybaseASEDialect.class )
	@FailureExpected( jiraKey = "HHH-3150" )
	public void testComponentQueryMethodNoParensFailureExpected() {
		// Sybase should translate "current_timestamp" in HQL to "getdate()";
		// This fails currently due to HHH-3510. The following test should be
		// deleted and testComponentQueries() should be updated (as noted
		// in that test case) when HHH-3510 is fixed.
		inTransaction(
				s -> {
					Employee emp = new Employee();
					emp.setHireDate( new Date() );
					emp.setPerson( new Person() );
					emp.getPerson().setName( "steve" );
					emp.getPerson().setDob( new Date() );
					s.persist( emp );
					s.createQuery( "from Employee e where e.person = (current_timestamp, 'steve')" ).list();
					s.remove( emp );
				}
		);
	}

	@Test
	public void testComponentFormulaQuery() {
		inTransaction(
				s -> {
					s.createQuery("from User u where u.person.yob = 1999").list();
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<User> criteria = criteriaBuilder.createQuery( User.class );
					Root<User> root = criteria.from( User.class );
					Join<Object, Object> person = root.join( "person", JoinType.INNER );
					criteria.where( criteriaBuilder.between( person.get( "yob" ), new Integer(1999), new Integer(2002) ) );
					s.createQuery( criteria ).list();

//		s.createCriteria(User.class)
//			.add( Property.forName("person.yob").between( new Integer(1999), new Integer(2002) ) )
//			.list();

					s.createQuery("from User u where u.person = ('Peachtree Rd', 'Peachtree Rd', :dob, 34, 'gavin', 'Karbarook Ave', 1974)")
							.setParameter("dob", new Date("March 25, 1974")).list();
					s.createQuery("from User where person = ('Peachtree Rd', 'Peachtree Rd', :dob, 34, 'gavin', 'Karbarook Ave', 1974)")
							.setParameter("dob", new Date("March 25, 1974")).list();
				}
		);
	}
	
	@Test
	public void testCustomColumnReadAndWrite() {
		inTransaction(
				s -> {
					User u = new User( "steve", "hibernater", new Person( "Steve Ebersole", new Date(), "Main St") );
					final double HEIGHT_INCHES = 73;
					final double HEIGHT_CENTIMETERS = HEIGHT_INCHES * 2.54d;
					u.getPerson().setHeightInches(HEIGHT_INCHES);
					s.persist( u );
					s.flush();

					// Test value conversion during insert
					// Value returned by Oracle native query is a Types.NUMERIC, which is mapped to a BigDecimalType;
					// Cast returned value to Number then call Number.doubleValue() so it works on all dialects.
					Double heightViaSql =
							( (Number)s.createNativeQuery("select height_centimeters from T_USER where T_USER.userName='steve'").uniqueResult())
									.doubleValue();
					assertEquals(HEIGHT_CENTIMETERS, heightViaSql, 0.01d);

					// Test projection
					Double heightViaHql = (Double)s.createQuery("select u.person.heightInches from User u where u.id = 'steve'").uniqueResult();
					assertEquals(HEIGHT_INCHES, heightViaHql, 0.01d);

					// Test restriction and entity load via criteria
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<User> criteria = criteriaBuilder.createQuery( User.class );
					Root<User> root = criteria.from( User.class );
					Join<Object, Object> person = root.join( "person", JoinType.INNER );
					criteria.where( criteriaBuilder.between( person.get( "heightInches" ), HEIGHT_INCHES - 0.01d, HEIGHT_INCHES + 0.01d) );
					u = s.createQuery( criteria ).uniqueResult();
//		u = (User)s.createCriteria(User.class)
//			.add(Restrictions.between("person.heightInches", HEIGHT_INCHES - 0.01d, HEIGHT_INCHES + 0.01d))
//			.uniqueResult();
					assertEquals(HEIGHT_INCHES, u.getPerson().getHeightInches(), 0.01d);

					// Test predicate and entity load via HQL
					u = (User)s.createQuery("from User u where u.person.heightInches between ?1 and ?2")
							.setParameter(1, HEIGHT_INCHES - 0.01d)
							.setParameter(2, HEIGHT_INCHES + 0.01d)
							.uniqueResult();
					assertEquals(HEIGHT_INCHES, u.getPerson().getHeightInches(), 0.01d);

					// Test update
					u.getPerson().setHeightInches(1);
					s.flush();
					heightViaSql =
							( (Number)s.createNativeQuery("select height_centimeters from T_USER where T_USER.userName='steve'").uniqueResult() )
									.doubleValue();
					assertEquals(2.54d, heightViaSql, 0.01d);
					s.remove(u);
				}
		);
	}
	
	@Test
	public void testNamedQuery() {
		inTransaction(
				s -> s.getNamedQuery( "userNameIn")
						.setParameterList( "nameList", new Object[] {"1ovthafew", "turin", "xam"} )
						.list()
		);
	}

	@Test
	public void testMergeComponent() {
		Long empId = fromTransaction(
				s -> {
					Employee e = new Employee();
					e.setHireDate( new Date() );
					e.setPerson( new Person() );
					e.getPerson().setName( "steve" );
					e.getPerson().setDob( new Date() );
					s.persist( e );
					return e.getId();
				}
		);

		Employee emp = fromTransaction(
				s -> s.get( Employee.class, empId )
		);

		assertNull(emp.getOptionalComponent());
		emp.setOptionalComponent( new OptionalComponent() );
		emp.getOptionalComponent().setValue1( "emp-value1" );
		emp.getOptionalComponent().setValue2( "emp-value2" );

		inTransaction(
				s -> s.merge( emp )
		);

		Employee emp2 = fromTransaction(
				s -> s.get( Employee.class, empId )
		);

		assertEquals("emp-value1", emp2.getOptionalComponent().getValue1());
		assertEquals("emp-value2", emp2.getOptionalComponent().getValue2());
		emp2.getOptionalComponent().setValue1( null );
		emp2.getOptionalComponent().setValue2( null );

		inTransaction(
				s -> s.merge( emp2 )
		);

		Employee emp3 = fromTransaction(
				s -> {
					Employee _emp = s.get( Employee.class, empId );
					Hibernate.initialize(_emp.getDirectReports());
					return _emp;
				}
		);

		assertNull(emp3.getOptionalComponent());

		Employee dr = new Employee();
		dr.setHireDate( new Date() );
		dr.setPerson( new Person() );
		dr.getPerson().setName( "bozo" );
		dr.getPerson().setDob( new Date() );
		emp3.getDirectReports().add( dr );

		inTransaction(
				s -> s.merge( emp3 )
		);

		Employee emp4 = fromTransaction(
				s -> {
					Employee _emp = s.get( Employee.class, empId );
					Hibernate.initialize(_emp.getDirectReports());
					return _emp;
				}
		);

		assertEquals(1, emp4.getDirectReports().size());
		dr = (Employee)emp4.getDirectReports().iterator().next();
		assertNull( dr.getOptionalComponent() );
		dr.setOptionalComponent( new OptionalComponent() );
		dr.getOptionalComponent().setValue1( "dr-value1" );
		dr.getOptionalComponent().setValue2( "dr-value2" );

		inTransaction(
				s -> s.merge( emp4 )
		);

		Employee emp5 = fromTransaction(
				s -> {
					Employee _emp = s.get( Employee.class, empId );
					Hibernate.initialize(_emp.getDirectReports());
					return _emp;
				}
		);

		assertEquals(1, emp5.getDirectReports().size());
		dr = (Employee)emp5.getDirectReports().iterator().next();
		assertEquals( "dr-value1", dr.getOptionalComponent().getValue1());
		assertEquals( "dr-value2", dr.getOptionalComponent().getValue2());
		dr.getOptionalComponent().setValue1( null );
		dr.getOptionalComponent().setValue2( null );

		inTransaction(
				s -> s.merge( emp5 )
		);

		Employee emp6 = fromTransaction(
				s -> {
					Employee _emp = s.get( Employee.class, empId );
					Hibernate.initialize(_emp.getDirectReports());
					return _emp;
				}
		);

		assertEquals(1, emp6.getDirectReports().size());
		dr = (Employee)emp6.getDirectReports().iterator().next();
		assertNull(dr.getOptionalComponent());

		inTransaction(
				s -> s.remove( emp6 )
		);
	}
}
