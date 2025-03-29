/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.components;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-6562")
@Jpa(annotatedClasses = {
		MappedSuperclassComponentWithCollectionTest.Employee.class,
		MappedSuperclassComponentWithCollectionTest.Manager.class,
		MappedSuperclassComponentWithCollectionTest.Project.class,
		MappedSuperclassComponentWithCollectionTest.Person.class,
		MappedSuperclassComponentWithCollectionTest.Leader.class
})
public class MappedSuperclassComponentWithCollectionTest {
	private Projects projects;

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					projects = new Projects();
					projects.addPreviousProject( new Project( "First" ) );
					projects.addPreviousProject( new Project( "Second" ) );
					projects.setCurrentProject( new Project( "Third" ) );

					ContactDetail contactDetail = new ContactDetail();
					contactDetail.setEmail( "abc@mail.org" );
					contactDetail.addPhone( new Phone( "+4411111111" ) );

					final Manager manager = new Manager();
					manager.setProjects( projects );
					manager.setContactDetail( contactDetail );
					entityManager.persist( manager );

					final Leader leader = new Leader();
					leader.setInformation( new Information() );
					ContactDetail infoContactDetail = new ContactDetail();
					infoContactDetail.setEmail( "xyz@mail.org" );
					infoContactDetail.addPhone( new Phone( "999-999-9999" ) );
					leader.getInformation().setInfoContactDetail( infoContactDetail );
					entityManager.persist( leader );
				}
		);
	}

	@Test
	public void testSizeExpressionForTheOneToManyPropertyOfAComponent(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Manager> query = builder.createQuery( Manager.class );
					Root<Manager> root = query.from( Manager.class );

					query.where(
							builder.equal(
									builder.size( root.get( "projects" ).get( "previousProjects" ) )
									, 2 ) );

					final List<Manager> results = entityManager.createQuery( query ).getResultList();
					assertThat( results.size(), is( 1 ) );
				}
		);
	}

	@Test
	public void testSizeExpressionForTheElementCollectionPropertyOfAComponent(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Manager> query = builder.createQuery( Manager.class );
					Root<Manager> root = query.from( Manager.class );

					query.where(
							builder.equal(
									builder.size( root.get( "contactDetail" ).get( "phones" ) )
									, 1 )
					);

					final List<Manager> results = entityManager.createQuery( query ).getResultList();
					assertThat( results.size(), is( 1 ) );
				}
		);
	}

	@Test
	public void testSizeExpressionForTheElementCollectionPropertyOfASubComponent(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Leader> query = builder.createQuery( Leader.class );
					Root<Leader> root = query.from( Leader.class );

					query.where(
							builder.equal(
									builder.size( root.get( "information" ).get( "infoContactDetail" ).get( "phones" ) )
									, 1 )
					);

					final List<Leader> results = entityManager.createQuery( query ).getResultList();
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

	@MappedSuperclass
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

	@Entity(name = "Manager")
	@Table(name = "MANAGER")
	public static class Manager extends Employee {
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
		@Column(name = "phone_number")
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

	@MappedSuperclass
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

	@Entity
	public static class Dummy1 extends Person {
	}

	@MappedSuperclass
	public static class Dummy2 extends Dummy1 {
	}

	@Entity(name="Leader")
	@Table(name="LEADER")
	public static class Leader extends Person {
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
