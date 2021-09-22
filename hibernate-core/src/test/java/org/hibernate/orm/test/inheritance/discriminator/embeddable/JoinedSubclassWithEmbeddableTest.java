/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.inheritance.discriminator.embeddable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11037")
@DomainModel(
		annotatedClasses = {
				JoinedSubclassWithEmbeddableTest.Person.class, JoinedSubclassWithEmbeddableTest.Employee.class
		}
)
@SessionFactory
public class JoinedSubclassWithEmbeddableTest {

	@Test
	public void testSelectFromEmbeddedField(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createNativeQuery( "select * from employee_emb_person_map" ).getResultList();
		} );
	}

	@Test
	public void testSelectFromSubclass(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createNativeQuery( "select * from embeddable_person_map" ).getResultList();
		} );
	}

	@Test
	public void testSelectFromParent(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createNativeQuery( "select * from person_map" ).getResultList();
		} );
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class Person implements Serializable {

		@Id
		@GeneratedValue
		private Long id;
		private String name;

		@Embedded
		private Contact contact;

		@ManyToOne
		@JoinColumn(name = "alert_contact")
		private Person alertContact;

		@OneToMany
		@JoinColumn(name = "alert_contact")
		private Set<Person> alerteeContacts = new HashSet<>();

		@ManyToMany
		@OrderColumn(name = "list_idx")
		@JoinTable(name = "person_list")
		private List<Person> personList = new ArrayList<>();

		@ManyToMany
		@CollectionTable(name = "person_map")
		@MapKeyColumn(name = "person_key", length = 20)
		private Map<String, Person> personMap = new HashMap<>();

		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Employee")
	@Table(name = "employees")
	public static class Employee extends Person {
		private Integer employeeNumber;

		@Embedded
		private EmployeeContact employeeContact;
	}

	@Embeddable
	public static class Contact implements Serializable {
		@ManyToOne
		@JoinColumn(name = "embeddable_alert_contact")
		private Person alertContact;

		@OneToMany
		@JoinColumn(name = "embeddable_alert_contact")
		private Set<Person> alerteeContacts = new HashSet<>();

		@ManyToMany
		@OrderColumn(name = "list_idx")
		@JoinTable(name = "embeddable_person_list")
		private List<Person> personList = new ArrayList<>();

		@ManyToMany
		@CollectionTable(name = "embeddable_person_map")
		@MapKeyColumn(name = "person_key", length = 20)
		private Map<String, Person> personMap = new HashMap<>();
	}

	@Embeddable
	public class EmployeeContact implements Serializable {

		@ManyToOne
		@JoinColumn(name = "employee_emb_alert_contact")
		private Person alertContact;

		@OneToMany
		@JoinColumn(name = "employee_emb_alert_contact")
		private Set<Employee> alerteeContacts = new HashSet<>();

		@ManyToMany
		@OrderColumn(name = "list_idx")
		@JoinTable(name = "employee_emb_person_list")
		private List<Person> personList = new ArrayList<>();

		@ManyToMany
		@CollectionTable(name = "employee_emb_person_map")
		@MapKeyColumn(name = "person_key", length = 20)
		private Map<String, Person> personMap = new HashMap<>();
	}
}
