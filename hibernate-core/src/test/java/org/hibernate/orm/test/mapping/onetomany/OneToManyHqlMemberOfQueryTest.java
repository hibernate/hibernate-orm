/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetomany;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				OneToManyHqlMemberOfQueryTest.Person.class,
				OneToManyHqlMemberOfQueryTest.Phone.class,
				OneToManyHqlMemberOfQueryTest.Call.class,
		}
)
@SessionFactory
public class OneToManyHqlMemberOfQueryTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person person1 = new Person( "John Doe" );
					person1.setNickName( "JD" );
					person1.setAddress( "Earth" );
					person1.setCreatedOn( Timestamp.from( LocalDateTime.of( 2000, 1, 1, 0, 0, 0 )
																.toInstant( ZoneOffset.UTC ) ) );
					person1.getAddresses().put( AddressType.HOME, "Home address" );
					person1.getAddresses().put( AddressType.OFFICE, "Office address" );
					session.persist( person1 );

					Person person2 = new Person( "Mrs. John Doe" );
					person2.setAddress( "Earth" );
					person2.setCreatedOn( Timestamp.from( LocalDateTime.of( 2000, 1, 2, 12, 0, 0 )
																.toInstant( ZoneOffset.UTC ) ) );
					session.persist( person2 );

					Person person3 = new Person( "Dr_ John Doe" );
					session.persist( person3 );

					Phone phone1 = new Phone( "123-456-7890" );
					phone1.setId( 1L );
					person1.addPhone( phone1 );
					phone1.getRepairTimestamps().add( Timestamp.from( LocalDateTime.of( 2005, 1, 1, 12, 0, 0 )
																			.toInstant( ZoneOffset.UTC ) ) );
					phone1.getRepairTimestamps().add( Timestamp.from( LocalDateTime.of( 2006, 1, 1, 12, 0, 0 )
																			.toInstant( ZoneOffset.UTC ) ) );

					Call call11 = new Call();
					call11.setDuration( 12 );
					call11.setTimestamp( Timestamp.from( LocalDateTime.of( 2000, 1, 1, 0, 0, 0 )
																.toInstant( ZoneOffset.UTC ) ) );

					Call call12 = new Call();
					call12.setDuration( 33 );
					call12.setTimestamp( Timestamp.from( LocalDateTime.of( 2000, 1, 1, 1, 0, 0 )
																.toInstant( ZoneOffset.UTC ) ) );

					phone1.addCall( call11 );
					phone1.addCall( call12 );

					Phone phone2 = new Phone( "098-765-4321" );
					phone2.setId( 2L );

					Phone phone3 = new Phone( "098-765-4320" );
					phone3.setId( 3L );

					person2.addPhone( phone2 );
					person2.addPhone( phone3 );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testMemberOf(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Call call = session.createQuery( "select c from Call c", Call.class ).getResultList().get( 0 );
					Phone phone = call.getPhone();

					List<Person> people = session.createQuery(
									"select p " +
											"from Person p " +
											"where :phone member of p.phones", Person.class )
							.setParameter( "phone", phone )
							.getResultList();
					assertEquals( 1, people.size() );
				}
		);
	}

	@Test
	public void testNegatedMemberOf(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Call call = session.createQuery( "select c from Call c", Call.class ).getResultList().get( 0 );
					Phone phone = call.getPhone();

					List<Person> people = session.createQuery(
									"select p " +
											"from Person p " +
											"where :phone not member of p.phones", Person.class )
							.setParameter( "phone", phone )
							.getResultList();
					assertEquals( 2, people.size() );
				}
		);
	}

	@Test
	public void testMember(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Call call = session.createQuery( "select c from Call c", Call.class ).getResultList().get( 0 );
					Phone phone = call.getPhone();

					List<Person> people = session.createQuery(
									"select p " +
											"from Person p " +
											"where :phone member p.phones", Person.class )
							.setParameter( "phone", phone )
							.getResultList();
					assertEquals( 1, people.size() );
				}
		);
	}

	@Test
	public void testNegatedMember(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Call call = session.createQuery( "select c from Call c", Call.class ).getResultList().get( 0 );
					Phone phone = call.getPhone();

					List<Person> people = session.createQuery(
									"select p " +
											"from Person p " +
											"where :phone not member p.phones", Person.class )
							.setParameter( "phone", phone )
							.getResultList();
					assertEquals( 2, people.size() );
				}
		);
	}

	@Test
	public void testMemberOf2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Call call = session.createQuery( "select c from Call c", Call.class ).getResultList().get( 0 );

					List<Person> people = session.createQuery(
									"select p " +
											"from Person p " +
											"join p.phones phone " +
											"where :call member of phone.calls", Person.class )
							.setParameter( "call", call )
							.getResultList();
					assertEquals( 1, people.size() );
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		private String nickName;

		private String address;

		@Temporal(TemporalType.TIMESTAMP)
		private Date createdOn;

		@OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
		@OrderColumn(name = "order_id")
		private List<Phone> phones = new ArrayList<>();

		@ElementCollection
		@MapKeyEnumerated(EnumType.STRING)
		private Map<AddressType, String> addresses = new HashMap<>();

		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getNickName() {
			return nickName;
		}

		public void setNickName(String nickName) {
			this.nickName = nickName;
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public Date getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(Date createdOn) {
			this.createdOn = createdOn;
		}

		public List<Phone> getPhones() {
			return phones;
		}

		public Map<AddressType, String> getAddresses() {
			return addresses;
		}

		public void addPhone(Phone phone) {
			phones.add( phone );
			phone.setPerson( this );
		}
	}

	public enum AddressType {
		HOME,
		OFFICE
	}

	@Entity(name = "Phone")
	public static class Phone {

		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Person person;

		@Column(name = "phone_number")
		private String number;

		@OneToMany(mappedBy = "phone", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<Call> calls = new ArrayList<>();

		@ElementCollection
		private List<Date> repairTimestamps = new ArrayList<>();

		public Phone() {
		}

		public Phone(String number) {
			this.number = number;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getNumber() {
			return number;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}

		public List<Call> getCalls() {
			return calls;
		}

		public List<Date> getRepairTimestamps() {
			return repairTimestamps;
		}

		public void addCall(Call call) {
			calls.add( call );
			call.setPhone( this );
		}
	}

	@Entity(name = "Call")
	@Table(name = "phone_call")
	public static class Call {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private Phone phone;

		@Column(name = "call_timestamp")
		private Date timestamp;

		private int duration;

		public Call() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Phone getPhone() {
			return phone;
		}

		public void setPhone(Phone phone) {
			this.phone = phone;
		}

		public Date getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(Date timestamp) {
			this.timestamp = timestamp;
		}

		public int getDuration() {
			return duration;
		}

		public void setDuration(int duration) {
			this.duration = duration;
		}
	}
}
