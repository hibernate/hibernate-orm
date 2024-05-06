/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.hbm.propertyref;

import org.hibernate.annotations.PropertyRef;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */

@SuppressWarnings("JUnitMalformedDeclaration")
@FailureExpected(reason = "Support for composite property-ref is going to be very messy as annotation binding assumes join columns are knowable very early.")
public class CompositeManyToOnePropertyRefTests {
	@Test
	@DomainModel(annotatedClasses = {Name.class, Person.class, Account.class})
	@SessionFactory
	void testAnnotations(DomainModelScope modelScope, SessionFactoryScope sfScope) {
		verify( modelScope.getEntityBinding( Account.class ), sfScope );
		final PersistentClass entityBinding = modelScope.getEntityBinding( Account.class );
	}

	private void verify(PersistentClass entityBinding, SessionFactoryScope sfScope) {
		final Property ownerProperty = entityBinding.getProperty( "owner" );
		final ToOne ownerPropertyValue = (ToOne) ownerProperty.getValue();
		assertThat( ownerPropertyValue.getReferencedPropertyName() ).isEqualTo( "name" );

		sfScope.inTransaction( (session) -> {
			final Person john = new Person( 1, "John", "Doe" );
			final Account account = new Account( 1, "savings", john );
			session.persist( john );
			session.persist( account );
		} );
	}

	@Embeddable
	public static class Name {
		private String first;
		private String last;

		public Name() {
		}

		public Name(String first, String last) {
			this.first = first;
			this.last = last;
		}

		public void setFirst(String first) {
			this.first = first;
		}

		public void setLast(String last) {
			this.last = last;
		}
	}

	@Entity(name="Person")
	@Table(name="Person")
	public static class Person {
		@Id
		private Integer id;
		private Name name;

		public Person() {
		}

		public Person(Integer id, Name name) {
			this.id = id;
			this.name = name;
		}

		public Person(Integer id, String firstName, String lastName) {
			this.id = id;
			this.name = new Name( firstName, lastName );
		}

		public Integer getId() {
			return id;
		}

		public Name getName() {
			return name;
		}

		public void setName(Name name) {
			this.name = name;
		}
	}

	@Entity(name="Account")
	@Table(name="Account")
	public static class Account {
		@Id
		private Integer id;
		private String name;
		@ManyToOne
		@PropertyRef( "name" )
		private Person owner;

		public Account() {
		}

		public Account(Integer id, String name, Person owner) {
			this.id = id;
			this.name = name;
			this.owner = owner;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Person getOwner() {
			return owner;
		}

		public void setOwner(Person owner) {
			this.owner = owner;
		}
	}
}
