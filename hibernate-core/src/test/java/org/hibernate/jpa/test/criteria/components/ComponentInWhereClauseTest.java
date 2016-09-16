/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.components;

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.transaction.TransactionUtil;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-6562")
public class ComponentInWhereClauseTest extends BaseEntityManagerFunctionalTestCase {
	private Projects projects;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Employee.class, Project.class, Person.class};
	}

	@Before
	public void setUp() throws Exception {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
									 projects = new Projects();
									 projects.addPreviousProject( new Project( "First" ) );
									 projects.addPreviousProject( new Project( "Second" ) );
									 projects.setCurrentProject( new Project( "Third" ) );

									 ContactDetail contactDetail = new ContactDetail();
									 contactDetail.setEmail( "abc@mail.org" );
									 contactDetail.addPhone( new Phone( "+4411111111" ) );

									 final Employee employee = new Employee();
									 employee.setProjects( projects );
									 employee.setContactDetail( contactDetail );
									 entityManager.persist( employee );

									 final Person person = new Person();
									 person.setInformation( new Information() );
									 ContactDetail infoContactDetail = new ContactDetail();
									 infoContactDetail.setEmail( "xyz@mail.org" );
									 infoContactDetail.addPhone( new Phone( "999-999-9999" ) );
									 person.getInformation().setInfoContactDetail( infoContactDetail );
									 entityManager.persist( person );
								 }
		);
	}

	@Test
	public void testSizeExpressionForTheOneToManyPropertyOfAComponent() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
									 CriteriaBuilder builder = entityManager.getCriteriaBuilder();
									 CriteriaQuery<Employee> query = builder.createQuery( Employee.class );
									 Root<Employee> root = query.from( Employee.class );

									 query.where(
											 builder.equal(
													 builder.size( root.get( "projects" ).get( "previousProjects" ) )
													 , 2 ) );

									 final List<Employee> results = entityManager.createQuery( query ).getResultList();
									 assertThat( results.size(), is( 1 ) );
								 }
		);
	}

	@Test
	public void testSizeExpressionForTheElementCollectionPropertyOfAComponent() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
									 CriteriaBuilder builder = entityManager.getCriteriaBuilder();
									 CriteriaQuery<Employee> query = builder.createQuery( Employee.class );
									 Root<Employee> root = query.from( Employee.class );

									 query.where(
											 builder.equal(
													 builder.size( root.get( "contactDetail" ).get( "phones" ) )
													 , 1 )
									 );

									 final List<Employee> results = entityManager.createQuery( query ).getResultList();
									 assertThat( results.size(), is( 1 ) );
								 }
		);
	}

	@Test
	public void testSizeExpressionForTheElementCollectionPropertyOfASubComponent() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Person> query = builder.createQuery( Person.class );
					Root<Person> root = query.from( Person.class );

					query.where(
							builder.equal(
									builder.size( root.get( "information" ).get( "infoContactDetail" ).get( "phones" ) )
									, 1 )
					);

					final List<Person> results = entityManager.createQuery( query ).getResultList();
					assertThat( results.size(), is( 1 ) );
				}
		);
	}

	@Test
	public void testEqualExpressionForThePropertyOfTheElementCollectionPropertyOfAComponent() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
									 CriteriaBuilder builder = entityManager.getCriteriaBuilder();
									 CriteriaQuery<Employee> query = builder.createQuery( Employee.class );
									 Root<Employee> root = query.from( Employee.class );

									 query.where(
											 builder.equal(
													 root.join( "contactDetail" ).join( "phones" ).get( "number" )
													 , "+4411111111" )
									 );

									 final List<Employee> results = entityManager.createQuery( query ).getResultList();
									 assertThat( results.size(), is( 1 ) );
								 }
		);
	}

	@Test
	public void testEqualityForThePropertyOfAComponent() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
									 CriteriaBuilder builder = entityManager.getCriteriaBuilder();
									 CriteriaQuery<Employee> query = builder.createQuery( Employee.class );
									 Root<Employee> root = query.from( Employee.class );

									 query.where(
											 builder.equal(
													 root.join( "contactDetail" ).get( "email" )
													 , "abc@mail.org"
											 )
									 );

									 final List<Employee> results = entityManager.createQuery( query ).getResultList();
									 assertThat( results.size(), is( 1 ) );
								 }
		);
	}

	@Test
	public void testInExpressionForAComponent() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
									 CriteriaBuilder builder = entityManager.getCriteriaBuilder();
									 CriteriaQuery<Employee> query = builder.createQuery( Employee.class );
									 Root<Employee> root = query.from( Employee.class );

									 query.where( root.get( "projects" ).in( projects, new Projects() ) );

									 final List<Employee> results = entityManager.createQuery( query ).getResultList();
									 assertThat( results.size(), is( 1 ) );
								 }
		);
	}

	@Test
	public void testInExpressionForTheManyToOnePropertyOfAComponent() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
									 CriteriaBuilder builder = entityManager.getCriteriaBuilder();
									 CriteriaQuery<Employee> query = builder.createQuery( Employee.class );
									 Root<Employee> root = query.from( Employee.class );

									 query.where( root.get( "projects" )
														  .get( "currentProject" )
														  .in( projects.getCurrentProject() ) );

									 final List<Employee> results = entityManager.createQuery( query ).getResultList();
									 assertThat( results.size(), is( 1 ) );
								 }
		);
	}

	@MappedSuperclass
	public static abstract class AbstractEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		protected Long id;

		public Long getId() {
			return id;
		}
	}

	@Entity(name = "Employee")
	@Table(name = "EMPLOYEE")
	public static class Employee extends AbstractEntity {

		@Embedded
		private Projects projects;

		@Embedded
		private ContactDetail contactDetail;

		public void setProjects(Projects projects) {
			this.projects = projects;
		}

		public void setContactDetail(ContactDetail contactDetail) {
			this.contactDetail = contactDetail;
		}
	}

	@Embeddable
	public static class ContactDetail {
		private String email;

		@ElementCollection
		private List<Phone> phones = new ArrayList<>();

		public void addPhone(Phone phone) {
			this.phones.add( phone );
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}
	}

	@Embeddable
	public static class Projects {

		@OneToMany(cascade = CascadeType.PERSIST)
		private Set<Project> previousProjects = new HashSet<>();

		@ManyToOne(cascade = CascadeType.PERSIST)
		private Project currentProject;

		public void setCurrentProject(Project project) {
			this.currentProject = project;
		}

		public void addPreviousProject(Project project) {
			this.previousProjects.add( project );
		}

		public Set<Project> getPreviousProjects() {
			return previousProjects;
		}

		public Project getCurrentProject() {
			return currentProject;
		}
	}

	@Entity(name = "Project")
	@Table(name = "PROJECT")
	public static class Project extends AbstractEntity {

		public Project() {
		}

		public Project(String name) {
			this.name = name;
		}

		private String name;

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
	}

	@Embeddable
	public static class Phone {
		private String number;

		public Phone() {
		}

		public Phone(String number) {
			this.number = number;
		}

		public String getNumber() {
			return this.number;
		}
	}

	@Entity(name = "Person")
	@Table(name="PERSON")
	public static class Person extends AbstractEntity {
		@Embedded
		private Information information;

		public Information getInformation() {
			return information;
		}

		public void setInformation(Information information) {
			this.information = information;
		}
	}

	@Embeddable
	public static class Information {
		@Embedded
		private ContactDetail infoContactDetail;

		public ContactDetail getInfoContactDetail() {
			return infoContactDetail;
		}

		public void setInfoContactDetail(ContactDetail infoContactDetail) {
			this.infoContactDetail = infoContactDetail;
		}
	}


}
