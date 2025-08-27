/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hqlfetchscroll;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.dialect.Dialect;
import org.hibernate.query.Query;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				QueryScrollingWithInheritanceTest.Employee.class,
				QueryScrollingWithInheritanceTest.OtherEntity.class
		}
)
@SessionFactory(generateStatistics = true)
public class QueryScrollingWithInheritanceTest {

	@Test
	public void testScrollableWithStatelessSession(SessionFactoryScope scope) {
		final StatisticsImplementor stats = scope.getSessionFactory().getStatistics();
		stats.clear();
		scope.inStatelessTransaction(
				statelessSession -> {
					ScrollableResults scrollableResults = null;
					try {
						Query<Employee> query = statelessSession.createQuery(
								"select distinct e from Employee e left join fetch e.otherEntities order by e.dept",
								Employee.class
						);
						final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
						scrollableResults = query.scroll( ScrollMode.FORWARD_ONLY );
						while ( scrollableResults.next() ) {
							final Employee employee = (Employee) scrollableResults.get();
							assertThat( Hibernate.isPropertyInitialized( employee, "otherEntities" ), is( true ) );
							assertThat( Hibernate.isInitialized( employee.getOtherEntities() ), is( true ) );
							if ( "ENG1".equals( employee.getDept() ) ) {
								assertThat( employee.getOtherEntities().size(), is( 2 ) );
								for ( OtherEntity otherEntity : employee.getOtherEntities() ) {
									if ( "test1".equals( otherEntity.id ) ) {
										assertThat(
												Hibernate.isPropertyInitialized( otherEntity, "employee" ),
												is( true )
										);
										assertThat( otherEntity.employee, is( employee ) );
										assertThat(
												Hibernate.isPropertyInitialized( otherEntity, "employeeParent" ),
												is( true )
										);
										assertThat( otherEntity.employeeParent, is( employee ) );
									}
									else {
										assertThat(
												Hibernate.isPropertyInitialized( otherEntity, "employee" ),
												is( true )
										);
										assertThat( otherEntity.employee, is( employee ) );
										assertThat(
												Hibernate.isPropertyInitialized( otherEntity, "employeeParent" ),
												is( true )
										);
										assertThat(
												Hibernate.isInitialized( otherEntity.employeeParent ),
												is( false )
										);
									}
								}
							}
							else {
								assertThat( employee.getOtherEntities().size(), is( 0 ) );
							}
						}
						assertThat( stats.getPrepareStatementCount(), is( 1L ) );
					}
					finally {
						if ( scrollableResults != null ) {
							scrollableResults.close();
						}
					}
				}
		);

	}

	@Test
	public void testScrollableWithSession(SessionFactoryScope scope) {
		final StatisticsImplementor stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		scope.inTransaction(
				session -> {
					ScrollableResults scrollableResults = null;
					try {
						Query<Employee> query = session.createQuery(
								"select distinct e from Employee e left join fetch e.otherEntities order by e.dept",
								Employee.class
						);
						final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
						scrollableResults = query.scroll( ScrollMode.FORWARD_ONLY );
						while ( scrollableResults.next() ) {
							final Employee employee = (Employee) scrollableResults.get();
							assertThat( Hibernate.isPropertyInitialized( employee, "otherEntities" ), is( true ) );
							assertThat( Hibernate.isInitialized( employee.getOtherEntities() ), is( true ) );
							if ( "ENG1".equals( employee.getDept() ) ) {
								assertThat( employee.getOtherEntities().size(), is( 2 ) );
								for ( OtherEntity otherEntity : employee.getOtherEntities() ) {
									if ( "test1".equals( otherEntity.id ) ) {
										assertThat(
												Hibernate.isPropertyInitialized( otherEntity, "employee" ),
												is( true )
										);
										assertThat( otherEntity.employee, is( employee ) );
										assertThat(
												Hibernate.isPropertyInitialized( otherEntity, "employeeParent" ),
												is( true )
										);
										assertThat( otherEntity.employeeParent, is( employee ) );
									}
									else {
										assertThat(
												Hibernate.isPropertyInitialized( otherEntity, "employee" ),
												is( true )
										);
										assertThat( otherEntity.employee, is( employee ) );
										assertThat(
												Hibernate.isPropertyInitialized( otherEntity, "employeeParent" ),
												is( true )
										);
										assertThat(
												Hibernate.isInitialized( otherEntity.employeeParent ),
												is( false )
										);
									}
								}
							}
							else {
								assertThat( employee.getOtherEntities().size(), is( 0 ) );
							}
						}
						assertThat( stats.getPrepareStatementCount(), is( 1L ) );
					}
					finally {
						if ( scrollableResults != null ) {
							scrollableResults.close();
						}
					}
				}
		);

	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
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

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
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
		@JoinColumn(name = "Employee_Id")
		protected Employee employee = null;

		@ManyToOne(fetch = FetchType.LAZY)
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
