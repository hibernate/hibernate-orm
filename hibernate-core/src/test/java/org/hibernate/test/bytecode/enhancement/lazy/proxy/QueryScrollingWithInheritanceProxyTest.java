/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.query.Query;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@RunWith(BytecodeEnhancerRunner.class)
public class QueryScrollingWithInheritanceProxyTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		super.configureSessionFactoryBuilder( sfb );
		sfb.applyStatisticsSupport( true );
		sfb.applySecondLevelCacheSupport( false );
		sfb.applyQueryCacheSupport( false );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( EmployeeParent.class );
		sources.addAnnotatedClass( Employee.class );
		sources.addAnnotatedClass( OtherEntity.class );
	}

	@Test
	public void testScrollableWithStatelessSession() {
		final StatisticsImplementor stats = sessionFactory().getStatistics();
		stats.clear();
		ScrollableResults scrollableResults = null;
		final StatelessSession statelessSession = sessionFactory().openStatelessSession();

		try {
			statelessSession.beginTransaction();
			Query<Employee> query = statelessSession.createQuery(
					"select distinct e from Employee e left join fetch e.otherEntities order by e.dept",
					Employee.class
			);
			if ( getDialect() instanceof DB2Dialect ) {
				/*
					FetchingScrollableResultsImp#next() in order to check if the ResultSet is empty calls ResultSet#isBeforeFirst()
					but the support for ResultSet#isBeforeFirst() is optional for ResultSets with a result
					set type of TYPE_FORWARD_ONLY and db2 does not support it.
			 	*/
				scrollableResults = query.scroll( ScrollMode.SCROLL_INSENSITIVE );
			}
			else {
				scrollableResults = query.scroll( ScrollMode.FORWARD_ONLY );
			}

			while ( scrollableResults.next() ) {
				final Employee employee = (Employee) scrollableResults.get( 0 );
				assertThat( Hibernate.isPropertyInitialized( employee, "otherEntities" ), is( true ) );
				assertThat( Hibernate.isInitialized( employee.getOtherEntities() ), is( true ) );
				if ( "ENG1".equals( employee.getDept() ) ) {
					assertThat( employee.getOtherEntities().size(), is( 2 ) );
					for ( OtherEntity otherEntity : employee.getOtherEntities() ) {
						if ( "test1".equals( otherEntity.id ) ) {
							assertThat( Hibernate.isPropertyInitialized( otherEntity, "employee" ), is( true ) );
							assertThat( otherEntity.employee, is( employee ) );
							assertThat( Hibernate.isPropertyInitialized( otherEntity, "employeeParent" ), is( true ) );
							assertThat( otherEntity.employeeParent, is( employee ) );
						}
						else {
							assertThat( Hibernate.isPropertyInitialized( otherEntity, "employee" ), is( true ) );
							assertThat( otherEntity.employee, is( employee ) );
							assertThat( Hibernate.isPropertyInitialized( otherEntity, "employeeParent" ), is( true ) );
							assertThat( Hibernate.isInitialized( otherEntity.employeeParent ), is( false ) );
						}
					}
				}
				else {
					assertThat( employee.getOtherEntities().size(), is( 0 ) );
				}
			}
			statelessSession.getTransaction().commit();
			assertThat( stats.getPrepareStatementCount(), is( 1L ) );
		}
		finally {
			if ( scrollableResults != null ) {
				scrollableResults.close();
			}
			if ( statelessSession.getTransaction().isActive() ) {
				statelessSession.getTransaction().rollback();
			}
			statelessSession.close();
		}
	}

	@Test
	public void testScrollableWithSession() {
		final StatisticsImplementor stats = sessionFactory().getStatistics();
		stats.clear();
		ScrollableResults scrollableResults = null;
		final Session session = sessionFactory().openSession();

		try {
			session.beginTransaction();
			Query<Employee> query = session.createQuery(
					"select distinct e from Employee e left join fetch e.otherEntities order by e.dept",
					Employee.class
			);
			if ( getDialect() instanceof DB2Dialect ) {
				/*
					FetchingScrollableResultsImp#next() in order to check if the ResultSet is empty calls ResultSet#isBeforeFirst()
					but the support for ResultSet#isBeforeFirst() is optional for ResultSets with a result
					set type of TYPE_FORWARD_ONLY and db2 does not support it.
			 	*/
				scrollableResults = query.scroll( ScrollMode.SCROLL_INSENSITIVE );
			}
			else {
				scrollableResults = query.scroll( ScrollMode.FORWARD_ONLY );
			}

			while ( scrollableResults.next() ) {
				final Employee employee = (Employee) scrollableResults.get( 0 );
				assertThat( Hibernate.isPropertyInitialized( employee, "otherEntities" ), is( true ) );
				assertThat( Hibernate.isInitialized( employee.getOtherEntities() ), is( true ) );
				if ( "ENG1".equals( employee.getDept() ) ) {
					assertThat( employee.getOtherEntities().size(), is( 2 ) );
					for ( OtherEntity otherEntity : employee.getOtherEntities() ) {
						if ( "test1".equals( otherEntity.id ) ) {
							assertThat( Hibernate.isPropertyInitialized( otherEntity, "employee" ), is( true ) );
							assertThat( otherEntity.employee, is( employee ) );
							assertThat( Hibernate.isPropertyInitialized( otherEntity, "employeeParent" ), is( true ) );
							assertThat( otherEntity.employeeParent, is( employee ) );
						}
						else {
							assertThat( Hibernate.isPropertyInitialized( otherEntity, "employee" ), is( true ) );
							assertThat( otherEntity.employee, is( employee ) );
							assertThat( Hibernate.isPropertyInitialized( otherEntity, "employeeParent" ), is( true ) );
							assertThat( Hibernate.isInitialized( otherEntity.employeeParent ), is( false ) );
						}
					}
				}
				else {
					assertThat( employee.getOtherEntities().size(), is( 0 ) );
				}
			}
			session.getTransaction().commit();
			assertThat( stats.getPrepareStatementCount(), is( 1L ) );
		}
		finally {
			if ( scrollableResults != null ) {
				scrollableResults.close();
			}
			if ( session.getTransaction().isActive() ) {
				session.getTransaction().rollback();
			}
			session.close();
		}
	}

	@Before
	public void prepareTestData() {
		inTransaction(
				session -> {
					Employee e1 = new Employee( "ENG1" );
					Employee e2 = new Employee( "ENG2" );
					OtherEntity other1 = new OtherEntity( "test1" );
					OtherEntity other2 = new OtherEntity( "test2" );
					e1.getOtherEntities().add( other1 );
					e1.getOtherEntities().add( other2 );
					e1.getParentOtherEntities().add( other1 );
					e1.getParentOtherEntities().add( other2 );
					other1.employee = e1;
					other2.employee = e1;
					other1.employeeParent = e1;
					other2.employeeParent = e2;
					session.persist( other1 );
					session.persist( other2 );
					session.persist( e1 );
					session.persist( e2 );
				}
		);
	}

	@After
	public void cleanUpTestData() {
		inTransaction(
				session -> {
					session.createQuery( "delete from OtherEntity" ).executeUpdate();
					session.createQuery( "delete from Employee" ).executeUpdate();
					session.createQuery( "delete from EmployeeParent" ).executeUpdate();
				}
		);
	}

	@Entity(name = "EmployeeParent")
	@Table(name = "EmployeeParent")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static abstract class EmployeeParent {

		@Id
		private String dept;

		@OneToMany(targetEntity = OtherEntity.class, mappedBy = "employeeParent", fetch = FetchType.LAZY)
		protected Set<OtherEntity> parentOtherEntities = new HashSet<>();

		public Set<OtherEntity> getParentOtherEntities() {
			if ( parentOtherEntities == null ) {
				parentOtherEntities = new LinkedHashSet();
			}
			return parentOtherEntities;
		}

		public void setOtherEntities(Set<OtherEntity> pParentOtherEntites) {
			parentOtherEntities = pParentOtherEntites;
		}

		public String getDept() {
			return dept;
		}

		protected void setDept(String dept) {
			this.dept = dept;
		}

	}

	@Entity(name = "Employee")
	@Table(name = "Employee")
	public static class Employee extends EmployeeParent {

		@OneToMany(targetEntity = OtherEntity.class, mappedBy = "employee", fetch = FetchType.LAZY)
		protected Set<OtherEntity> otherEntities = new HashSet<>();

		public Employee(String dept) {
			this();
			setDept( dept );
		}

		protected Employee() {
			// this form used by Hibernate
		}

		public Set<OtherEntity> getOtherEntities() {
			if ( otherEntities == null ) {
				otherEntities = new LinkedHashSet();
			}
			return otherEntities;
		}

		public void setOtherEntities(Set<OtherEntity> pOtherEntites) {
			otherEntities = pOtherEntites;
		}
	}

	@Entity(name = "OtherEntity")
	@Table(name = "OtherEntity")
	public static class OtherEntity {

		@Id
		private String id;

		@ManyToOne(fetch = FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		@JoinColumn(name = "Employee_Id")
		protected Employee employee = null;

		@ManyToOne(fetch = FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		@JoinColumn(name = "EmployeeParent_Id")
		protected EmployeeParent employeeParent = null;

		protected OtherEntity() {
			// this form used by Hibernate
		}

		public OtherEntity(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}
	}

}
