/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance.discriminator.embeddable;

import javax.persistence.CollectionTable;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11037")
public class SingleTableWithEmbeddableTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Person.class, Employee.class};
	}

	@Test
	public void testSelectFromEmbeddedField() {
		doInHibernate( this::sessionFactory, session -> {
			session.createNativeQuery( "select * from employee_emb_person_map" ).getResultList();
		} );
	}

	@Test
	public void testSelectFromSubclass() {
		doInHibernate( this::sessionFactory, session -> {
			session.createNativeQuery( "select * from embeddable_person_map" ).getResultList();
		} );
	}

	@Test
	public void testSelectFromParent() {
		doInHibernate( this::sessionFactory, session -> {
			session.createNativeQuery( "select * from person_map" ).getResultList();
		} );
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
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

	@Entity
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
