/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embeddables;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Ignore;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@FailureExpected( jiraKey = "HHH-11302", message = "TBD")
@Ignore
public class EmbeddableWithOneToManyTest extends BaseCoreFunctionalTestCase {

	// Add your entities here.
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				ContactType.class,
				Person.class
		};
	}

	// Add your tests, using standard JUnit.
	@Test
	public void hhh11302Test() throws Exception {
		Person person = doInHibernate( this::sessionFactory, s -> {
			ContactType emailContactType = new ContactType();
			emailContactType.setType("EMAIL");

			ContactType twitterContactType = new ContactType();
			twitterContactType.setType("TWITTER");

			List<ContactType> contactTypes = Arrays.asList(emailContactType, twitterContactType);

			Person.ContactInformation contactInformation = new Person.ContactInformation();
			contactInformation.setContactType(contactTypes);

			Person _person = new Person();
			_person.setContacts(Arrays.asList(contactInformation));
			s.persist(_person);

			return _person;
		} );

		doInHibernate( this::sessionFactory, s -> {
			Person person2 = s.find(Person.class,person.getId());
			assertEquals(person,person2);
			assertTrue(person2.getContacts().size() > 0);
			assertEquals(person.getContacts(),person2.getContacts());
			assertTrue(person2.getContacts().get(0).getContactType().size() > 0);
			assertEquals(person.getContacts().get(0).getContactType(),person2.getContacts().get(0).getContactType());
		} );
	}

	@Entity
	@Table(name = "CONTACTTYPE")
	public static class ContactType implements Serializable {

		private static final long serialVersionUID = 1L;
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@Column(name = "id", updatable = false, nullable = false)
		private Long id;

		@Version
		@Column(name = "version")
		private int version;

		@Column(name = "contactType", nullable = false)
		private String type;

		public Long getId() {
			return this.id;
		}

		public void setId(final Long id) {
			this.id = id;
		}

		public int getVersion() {
			return this.version;
		}

		public void setVersion(final int version) {
			this.version = version;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof ContactType )) {
				return false;
			}
			ContactType other = (ContactType) obj;
			if (id != null) {
				if (!id.equals(other.id)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			return result;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		@Override
		public String toString() {
			String result = getClass().getSimpleName() + " ";
			if (id != null)
				result += "id: " + id;
			result += ", version: " + version;
			if (type != null && !type.trim().isEmpty())
				result += ", type: " + type;
			return result;
		}
	}

	@Entity
	@Table(name = "PERSON")
	public static class Person implements Serializable {

		private static final long serialVersionUID = 1L;

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@Column(name = "id", updatable = false, nullable = false)
		private Long id;

		@Version
		@Column(name = "version")
		private int version;

		@ElementCollection
		@CollectionTable(
				name = "CONTACT_INFO",
				joinColumns=@JoinColumn(name = "person_id")
		)
		private List<ContactInformation> contacts;

		public Long getId() {
			return this.id;
		}

		public void setId(final Long id) {
			this.id = id;
		}

		public int getVersion() {
			return this.version;
		}

		public void setVersion(final int version) {
			this.version = version;
		}

		@Override
		public String toString() {
			String result = getClass().getSimpleName() + " ";
			if (id != null)
				result += "id: " + id;
			return result;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			Person person = (Person) o;
			return version == person.version && Objects.equals( id, person.id);
		}

		@Override
		public int hashCode() {
			return Objects.hash(id, version, contacts);
		}

		public void setContacts(List<ContactInformation> contacts) {
			this.contacts = contacts;
		}

		public List<ContactInformation> getContacts() {
			return contacts;
		}

		@Embeddable
		public static class ContactInformation implements Serializable {

			@Column(name = "name")
			String name;

			@OneToMany(cascade = CascadeType.ALL)
			@JoinTable(
					name="CONTACT_TYPE",
					joinColumns = @JoinColumn( name="id"),
					inverseJoinColumns=@JoinColumn(name="id")
			)
			private List<ContactType> contactType = new ArrayList<>();

			public List<ContactType> getContactType() {
				return contactType;
			}

			public void setContactType(final List<ContactType> contactType) {
				this.contactType = contactType;
			}

			public void setName(String name) {
				this.name = name;
			}

			public String getName() {
				return name;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;
				ContactInformation that = (ContactInformation) o;
				return Objects.equals(name, that.name);
			}

			@Override
			public int hashCode() {
				return Objects.hash(name);
			}
		}


	}
}
