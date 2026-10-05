package org.hibernate.orm.test.annotations.indexcoll.eager;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// An eager inverse map derives its entity-valued key from a select-fetched
/// association that also forms part of the value entity's composite identifier.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		EagerMapKeyCompositeIdTest.Employee.class,
		EagerMapKeyCompositeIdTest.Phone.class,
		EagerMapKeyCompositeIdTest.PhoneType.class
})
@SessionFactory
@JiraKey("HHH-10448")
public class EagerMapKeyCompositeIdTest {
	@Test
	void testEagerMapKeyInCompositeIdentifier(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			PhoneType home = new PhoneType( 1L, "home" );
			PhoneType work = new PhoneType( 2L, "work" );
			session.persist( home );
			session.persist( work );
			Employee first = new Employee( 1L );
			first.addPhone( home, "111" );
			first.addPhone( work, "222" );
			session.persist( first );
			Employee second = new Employee( 2L );
			second.addPhone( home, "333" );
			session.persist( second );
		} );

		scope.inTransaction( session -> {
			Employee first = session.find( Employee.class, 1L );
			assertTrue( Hibernate.isInitialized( first.phones ) );
			assertEquals( 2, first.phones.size() );
			assertPhone( first, session.find( PhoneType.class, 1L ), "111" );
			assertPhone( first, session.find( PhoneType.class, 2L ), "222" );
		} );

		scope.inTransaction( session -> {
			var employees = session.createQuery(
					"from EagerMapEmployee e order by e.id", Employee.class
			).getResultList();
			assertEquals( 2, employees.size() );
			Employee first = employees.get( 0 );
			Employee second = employees.get( 1 );
			assertTrue( Hibernate.isInitialized( first.phones ) );
			assertTrue( Hibernate.isInitialized( second.phones ) );
			assertEquals( 2, first.phones.size() );
			assertEquals( 1, second.phones.size() );
			PhoneType home = session.find( PhoneType.class, 1L );
			assertPhone( first, home, "111" );
			assertPhone( first, session.find( PhoneType.class, 2L ), "222" );
			assertPhone( second, home, "333" );
		} );
	}

	private static void assertPhone(Employee employee, PhoneType type, String number) {
		Phone phone = employee.phones.get( type );
		assertEquals( number, phone.phoneNumber );
		assertSame( employee, phone.owner );
		assertSame( type, phone.phoneType );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "EagerMapEmployee")
	@Table(name = "eager_map_employee")
	static class Employee {
		@Id
		@Column(name = "employee_id")
		private long id;

		@OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		@MapKey(name = "phoneType")
		private Map<PhoneType, Phone> phones = new HashMap<>();

		public Employee() {
		}

		Employee(long id) {
			this.id = id;
		}

		void addPhone(PhoneType type, String number) {
			Phone phone = new Phone();
			phone.owner = this;
			phone.phoneType = type;
			phone.phoneNumber = number;
			phones.put( type, phone );
		}
	}

	@Entity(name = "EagerMapPhone")
	@Table(name = "eager_map_phone")
	static class Phone implements Serializable {
		@Id
		@ManyToOne
		@JoinColumn(name = "employee_id", referencedColumnName = "employee_id", nullable = false)
		private Employee owner;

		@Id
		@ManyToOne
		@JoinColumn(name = "phone_type_id", nullable = false)
		@Fetch(FetchMode.SELECT)
		private PhoneType phoneType;

		@Column(name = "phone_number", nullable = false)
		private String phoneNumber;

		@Override
		public boolean equals(Object other) {
			return this == other || other instanceof Phone phone
					&& owner.id == phone.owner.id && phoneType.id == phone.phoneType.id;
		}

		@Override
		public int hashCode() {
			return Objects.hash( owner.id, phoneType.id );
		}
	}

	@Entity(name = "EagerMapPhoneType")
	@Table(name = "eager_map_phone_type")
	static class PhoneType {
		@Id
		@Column(name = "phone_type_id")
		private long id;

		@Column(name = "phone_type", nullable = false)
		private String name;

		public PhoneType() {
		}

		PhoneType(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
