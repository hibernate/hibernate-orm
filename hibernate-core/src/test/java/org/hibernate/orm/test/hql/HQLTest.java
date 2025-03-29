/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;

import org.hibernate.CacheMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.community.dialect.FirebirdDialect;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.query.QueryProducer;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.testing.orm.domain.userguide.Account;
import org.hibernate.testing.orm.domain.userguide.AddressType;
import org.hibernate.testing.orm.domain.userguide.Call;
import org.hibernate.testing.orm.domain.userguide.CreditCardPayment;
import org.hibernate.testing.orm.domain.userguide.Payment;
import org.hibernate.testing.orm.domain.userguide.Person;
import org.hibernate.testing.orm.domain.userguide.PersonNames;
import org.hibernate.testing.orm.domain.userguide.Phone;
import org.hibernate.testing.orm.domain.userguide.PhoneType;
import org.hibernate.testing.orm.domain.userguide.WireTransferPayment;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("unused")
public class HQLTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
			Phone.class,
			Call.class,
			Account.class,
			CreditCardPayment.class,
			WireTransferPayment.class
		};
	}

	@Before
	public void init() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Person person1 = new Person("John Doe");
			person1.setNickName("JD");
			person1.setAddress("Earth");
			person1.setCreatedOn(LocalDateTime.of(2000, 1, 1, 0, 0, 0)) ;
			person1.getAddresses().put(AddressType.HOME, "Home address");
			person1.getAddresses().put(AddressType.OFFICE, "Office address");
			entityManager.persist(person1);

			Person person2 = new Person("Mrs. John Doe");
			person2.setAddress("Earth");
			person2.setCreatedOn(LocalDateTime.of(2000, 1, 2, 12, 0, 0)) ;
			entityManager.persist(person2);

			Person person3 = new Person("Dr_ John Doe");
			entityManager.persist(person3);

			Phone phone1 = new Phone("123-456-7890");
			phone1.setId(1L);
			phone1.setType(PhoneType.MOBILE);
			person1.addPhone(phone1);
			phone1.getRepairTimestamps().add(LocalDateTime.of(2005, 1, 1, 12, 0, 0));
			phone1.getRepairTimestamps().add(LocalDateTime.of(2006, 1, 1, 12, 0, 0));

			Call call11 = new Call();
			call11.setDuration(12);
			call11.setTimestamp(LocalDateTime.of(2000, 1, 1, 0, 0, 0));

			Call call12 = new Call();
			call12.setDuration(33);
			call12.setTimestamp(LocalDateTime.of(2000, 1, 1, 1, 0, 0));

			phone1.addCall(call11);
			phone1.addCall(call12);

			Phone phone2 = new Phone("098-765-4321");
			phone2.setId(2L);
			phone2.setType(PhoneType.LAND_LINE);

			Phone phone3 = new Phone("098-765-4320");
			phone3.setId(3L);
			phone3.setType(PhoneType.LAND_LINE);

			person2.addPhone(phone2);
			person2.addPhone(phone3);

			Account account1 = new Account();
			account1.setOwner(person1);
			entityManager.persist(account1);

			Account account2 = new Account();
			account1.setOwner(person2);
			entityManager.persist(account2);

			CreditCardPayment creditCardPayment = new CreditCardPayment();
			creditCardPayment.setCompleted(true);
			creditCardPayment.setAmount(BigDecimal.ZERO);
			creditCardPayment.setPerson(person1);
			creditCardPayment.setCardNumber("1234-1234-1234-1234");
			creditCardPayment.setAccount(account1);
			call11.setPayment(creditCardPayment);

			WireTransferPayment wireTransferPayment = new WireTransferPayment();
			wireTransferPayment.setCompleted(true);
			wireTransferPayment.setAmount(BigDecimal.valueOf(100));
			wireTransferPayment.setPerson(person2);
			wireTransferPayment.setAccount(account2);
			call12.setPayment(wireTransferPayment);

			entityManager.persist(creditCardPayment);
			entityManager.persist(wireTransferPayment);
		});
	}

	@Test
	public void test_hql_select_simplest_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			List<Object> objects = session.createQuery(
				"from java.lang.Object",
				Object.class)
			.getResultList();

			//tag::hql-select-simplest-example[]
			List<Person> persons = session.createQuery(
				"from Person", Person.class)
			.getResultList();
			//end::hql-select-simplest-example[]

			//tag::hql-select-simplest-example-alt[]
			LocalDateTime datetime = session.createQuery(
				"select local datetime",
				LocalDateTime.class)
			.getSingleResult();
			//end::hql-select-simplest-example-alt[]
		});
	}

	@Test
	public void test_hql_select_simplest_jpql_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-select-simplest-jpql-example[]
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p",
				Person.class)
			.getResultList();
			//end::hql-select-simplest-jpql-example[]

			Session session = entityManager.unwrap(Session.class);
			//tag::hql-select-last-example[]
			List<String> datetimes = session.createQuery(
				"from Person p select p.name",
				String.class)
			.getResultList();
			//end::hql-select-last-example[]
		});
	}

	@Test
	public void test_hql_select_no_from() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::hql-select-no-from[]

			// result type Person, only the Person selected
			List<Person> persons = session.createQuery(
				"from Person join phones", Person.class)
				.getResultList();
			for (Person person: persons) {
				//...
			}

			// result type Object[], both Person and Phone selected
			List<Object[]> personsWithPhones = session.createQuery(
				"from Person join phones", Object[].class)
				.getResultList();
			for (Object[] personWithPhone: personsWithPhones) {
				Person p = (Person) personWithPhone[0];
				Phone ph = (Phone) personWithPhone[1];
				//...
			}
			//end::hql-select-no-from[]
		});
	}

	@Test
	public void hql_update_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-update-example[]
			entityManager.createQuery(
				"update Person set nickName = 'Nacho' " +
				"where name = 'Ignacio'")
			.executeUpdate();
			//end::hql-update-example[]
		});
	}

	@Test
	public void hql_insert_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-insert-example[]
			entityManager.createQuery(
				"insert Person (id, name) " +
				"values (100L, 'Jane Doe')")
			.executeUpdate();
			//end::hql-insert-example[]
		});
	}

	@Test
	public void hql_multi_insert_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-insert-example[]
			entityManager.createQuery(
				"insert Person (id, name) " +
				"values (101L, 'J A Doe III'), " +
				"(102L, 'J X Doe'), " +
				"(103L, 'John Doe, Jr')")
			.executeUpdate();
			//end::hql-insert-example[]
		});
	}

	@Test
	public void hql_insert_with_sequence_example() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.createQuery(
				"insert Person (name) values ('Jane Doe2')" )
			.executeUpdate();
		});
	}

	@Test
	public void hql_select_simplest_jpql_fqn_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-select-simplest-jpql-fqn-example[]
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from org.hibernate.testing.orm.domain.userguide.Person p",
				Person.class)
			.getResultList();
			//end::hql-select-simplest-jpql-fqn-example[]
		});
	}

	@Test
	public void test_hql_multiple_root_reference_jpql_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-multiple-root-reference-jpql-example[]
			List<Object[]> persons = entityManager.createQuery(
				"select distinct pr, ph " +
				"from Person pr, Phone ph " +
				"where ph.person = pr and ph is not null",
				Object[].class)
			.getResultList();
			//end::hql-multiple-root-reference-jpql-example[]
			assertEquals(3, persons.size());
		});
	}

	@Test
	public void test_hql_cross_join_jpql_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-cross-join-jpql-example[]
			List<Object[]> persons = entityManager.createQuery(
				"select distinct pr, ph " +
				"from Person pr cross join Phone ph " +
				"where ph.person = pr and ph is not null",
				Object[].class)
			.getResultList();
			//end::hql-cross-join-jpql-example[]
			assertEquals(3, persons.size());
		});
	}

	@Test
	public void test_hql_multiple_same_root_reference_jpql_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-multiple-same-root-reference-jpql-example[]
			List<Person> persons = entityManager.createQuery(
				"select distinct pr1 " +
				"from Person pr1, Person pr2 " +
				"where pr1.id <> pr2.id " +
				"  and pr1.address = pr2.address " +
				"  and pr1.createdOn < pr2.createdOn",
				Person.class)
			.getResultList();
			//end::hql-multiple-same-root-reference-jpql-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_explicit_root_join_example_1() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-explicit-root-join-example[]
			List<Person> persons = entityManager.createQuery(
				"select distinct pr " +
				"from Person pr " +
				"join Phone ph on ph.person = pr " +
				"where ph.type = :phoneType",
				Person.class)
			.setParameter("phoneType", PhoneType.MOBILE)
			.getResultList();
			//end::hql-explicit-root-join-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_explicit_inner_join_example_1() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-explicit-inner-join-example[]
			List<Person> persons = entityManager.createQuery(
				"select distinct pr " +
				"from Person pr " +
				"join pr.phones ph " +
				"where ph.type = :phoneType",
				Person.class)
			.setParameter("phoneType", PhoneType.MOBILE)
			.getResultList();
			//end::hql-explicit-inner-join-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_explicit_inner_join_example_2() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-explicit-inner-join-example[]

			// same query, but specifying join type as 'inner' explicitly
			List<Person> persons = entityManager.createQuery(
				"select distinct pr " +
				"from Person pr " +
				"inner join pr.phones ph " +
				"where ph.type = :phoneType",
				Person.class)
			.setParameter("phoneType", PhoneType.MOBILE)
			.getResultList();
			//end::hql-explicit-inner-join-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_explicit_outer_join_example_1() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-explicit-outer-join-example[]
			List<Person> persons = entityManager.createQuery(
				"select distinct pr " +
				"from Person pr " +
				"left join pr.phones ph " +
				"where ph is null " +
				"   or ph.type = :phoneType",
				Person.class)
			.setParameter("phoneType", PhoneType.LAND_LINE)
			.getResultList();
			//end::hql-explicit-outer-join-example[]
			assertEquals(2, persons.size());
		});
	}

	@Test
	public void test_hql_explicit_outer_join_example_2() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-explicit-outer-join-example[]

			// same query, but specifying join type as 'outer' explicitly
			List<Person> persons = entityManager.createQuery(
				"select distinct pr " +
				"from Person pr " +
				"left outer join pr.phones ph " +
				"where ph is null " +
				"   or ph.type = :phoneType",
				Person.class)
			.setParameter("phoneType", PhoneType.LAND_LINE)
			.getResultList();
			//end::hql-explicit-outer-join-example[]
			assertEquals(2, persons.size());
		});
	}

	@Test
	public void test_hql_explicit_fetch_join_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-explicit-fetch-join-example[]
			List<Person> persons = entityManager.createQuery(
				"select distinct pr " +
				"from Person pr " +
				"left join fetch pr.phones ",
				Person.class)
			.getResultList();
			//end::hql-explicit-fetch-join-example[]
			assertEquals(3, persons.size());
		});
	}

	@Test
	public void test_hql_explicit_join_with_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::hql-explicit-join-with-example[]
			List<Object[]> personsAndPhones = session.createQuery(
				"select pr.name, ph.number " +
				"from Person pr " +
				"left join pr.phones ph with ph.type = :phoneType ",
				Object[].class)
			.setParameter("phoneType", PhoneType.LAND_LINE)
			.getResultList();
			//end::hql-explicit-join-with-example[]
			assertEquals(4, personsAndPhones.size());
		});
	}

	@Test
	public void test_jpql_explicit_join_on_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-explicit-join-jpql-on-example[]
			List<Object[]> personsAndPhones = entityManager.createQuery(
				"select pr.name, ph.number " +
				"from Person pr " +
				"left join pr.phones ph on ph.type = :phoneType ",
				Object[].class)
			.setParameter("phoneType", PhoneType.LAND_LINE)
			.getResultList();
			//end::hql-explicit-join-jpql-on-example[]
			assertEquals(4, personsAndPhones.size());
		});
	}

	@Test
	public void test_hql_implicit_join_example_1() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			String address = "Earth";
			//tag::hql-implicit-join-example[]
			List<Phone> phones = entityManager.createQuery(
				"select ph " +
				"from Phone ph " +
				"where ph.person.address = :address ",
				Phone.class)
			.setParameter("address", address)
			.getResultList();
			//end::hql-implicit-join-example[]
			assertEquals(3, phones.size());
		});
	}

	@Test
	public void test_hql_implicit_join_example_2() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			String address = "Earth";
			//tag::hql-implicit-join-example[]

			// same as
			List<Phone> phones = entityManager.createQuery(
				"select ph " +
				"from Phone ph " +
				"join ph.person pr " +
				"where pr.address = :address ",
				Phone.class)
			.setParameter("address", address)
			.getResultList();
			//end::hql-implicit-join-example[]
			assertEquals(3, phones.size());
		});
	}

	@Test
	public void test_hql_implicit_join_alias_example_1() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			String address = "Earth";
			LocalDateTime timestamp = LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
			//tag::hql-implicit-join-alias-example[]
			List<Phone> phones = entityManager.createQuery(
				"select ph " +
				"from Phone ph " +
				"where ph.person.address = :address " +
				"  and ph.person.createdOn > :timestamp",
				Phone.class)
			.setParameter("address", address)
			.setParameter("timestamp", timestamp)
			.getResultList();
			//end::hql-implicit-join-alias-example[]
			assertEquals(3, phones.size());
		});
	}

	@Test
	public void test_hql_implicit_join_alias_example_2() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			String address = "Earth";
			LocalDateTime timestamp = LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
			//tag::hql-implicit-join-alias-example[]

			//same as
			List<Phone> phones = entityManager.createQuery(
				"select ph " +
				"from Phone ph " +
				"inner join ph.person pr " +
				"where pr.address = :address " +
				"  and pr.createdOn > :timestamp",
				Phone.class)
			.setParameter("address", address)
			.setParameter("timestamp", timestamp)
			.getResultList();
			//end::hql-implicit-join-alias-example[]
			assertEquals(3, phones.size());
		});
	}

	@Test
	public void test_hql_collection_valued_associations_1() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			String address = "Earth";
			int duration = 20;
			//tag::hql-collection-valued-associations[]
			List<Phone> phones = entityManager.createQuery(
				"select ph " +
				"from Person pr " +
				"join pr.phones ph " +
				"join ph.calls c " +
				"where pr.address = :address " +
				"  and c.duration > :duration",
				Phone.class)
			.setParameter("address", address)
			.setParameter("duration", duration)
			.getResultList();
			//end::hql-collection-valued-associations[]
			assertEquals(1, phones.size());
			assertEquals( "123-456-7890", phones.get( 0).getNumber());
		});

	}

	@Test
	// we do not need to document this syntax, which is a
	// legacy of EJB entity beans, prior to JPA / EJB 3
	public void test_hql_collection_valued_associations_2() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			String address = "Earth";
			int duration = 20;
			//tag::ejb-collection-valued-associations[]

			// alternate syntax
			List<Phone> phones = session.createQuery(
				"select ph " +
				"from Person pr, " +
				"in (pr.phones) ph, " +
				"in (ph.calls) c " +
				"where pr.address = :address " +
				"  and c.duration > :duration",
				Phone.class)
			.setParameter("address", address)
			.setParameter("duration", duration)
			.getResultList();
			//end::ejb-collection-valued-associations[]
			assertEquals(1, phones.size());
			assertEquals( "123-456-7890", phones.get( 0).getNumber());
		});
	}

	@Test
	public void test_hql_collection_qualification_associations_1() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			Long id = 1L;
			//tag::hql-collection-qualification-example[]

			// select all the calls (the map value) for a given Phone
			// note that here we don't need to use value() or element()
			// since it is implicit
			List<Call> calls = entityManager.createQuery(
				"select ch " +
				"from Phone ph " +
				"join ph.callHistory ch " +
				"where ph.id = :id ",
				Call.class)
			.setParameter("id", id)
			.getResultList();
			//end::hql-collection-qualification-example[]
			assertEquals(2, calls.size());
		});

	}

	@Test
	public void test_hql_collection_qualification_associations_2() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Long id = 1L;
			//tag::hql-collection-qualification-example[]

			// same as above, but with value() explicit
			List<Call> calls = entityManager.createQuery(
				"select value(ch) " +
				"from Phone ph " +
				"join ph.callHistory ch " +
				"where ph.id = :id ",
				Call.class)
			.setParameter("id", id)
			.getResultList();
			//end::hql-collection-qualification-example[]
			assertEquals(2, calls.size());
		});
	}

	@Test
	public void test_hql_collection_qualification_associations_3() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Long id = 1L;
			//tag::hql-collection-qualification-example[]

			// select all the Call timestamps (the map key) for a given Phone
			// note that here we *do* need to explicitly specify key()
			List<LocalDateTime> timestamps = entityManager.createQuery(
				"select key(ch) " +
				"from Phone ph " +
				"join ph.callHistory ch " +
				"where ph.id = :id ",
				LocalDateTime.class)
			.setParameter("id", id)
			.getResultList();
			//end::hql-collection-qualification-example[]
			assertEquals(2, timestamps.size());
		});
	}

	@Test
	public void test_hql_collection_qualification_associations_4() {
		doInJPA(this::entityManagerFactory, entityManager -> {

			Long id = 1L;
			//tag::hql-collection-qualification-example[]

			// select all the Call and their timestamps (the 'Map.Entry') for a given Phone
			List<Map.Entry<Date, Call>> callHistory = entityManager.createQuery(
				"select entry(ch) " +
				"from Phone ph " +
				"join ph.callHistory ch " +
				"where ph.id = :id ")
			.setParameter("id", id)
			.getResultList();
			//end::hql-collection-qualification-example[]

		});
	}

	@Test
	public void test_hql_collection_qualification_associations_5() {
		doInJPA(this::entityManagerFactory, entityManager -> {

			Long id = 1L;
			Integer phoneIndex = 0;
			//tag::hql-collection-qualification-example[]

			// Sum all call durations for a given Phone of a specific Person
			Long duration = entityManager.createQuery(
				"select sum(ch.duration) " +
				"from Person pr " +
				"join pr.phones ph " +
				"join ph.callHistory ch " +
				"where ph.id = :id " +
				"  and index(ph) = :phoneIndex",
				Long.class)
			.setParameter("id", id)
			.setParameter("phoneIndex", phoneIndex)
			.getSingleResult();
			//end::hql-collection-qualification-example[]
			assertEquals(45, duration.intValue());

		});
	}

	@Test
	public void test_hql_collection_implicit_join_1() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Long id = 1L;
			//tag::hql-collection-implicit-join-example[]

			// implicit join to a map value()
			List<Call> calls = entityManager.createQuery(
				"select value(ph.callHistory) " +
				"from Phone ph " +
				"where ph.id = :id ",
				Call.class)
			.setParameter("id", id)
			.getResultList();
			//end::hql-collection-implicit-join-example[]
			assertEquals(2, calls.size());
		});
	}

	@Test
	public void test_hql_collection_implicit_join_2() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Long id = 1L;
			//tag::hql-collection-implicit-join-example[]

			// implicit join to a map key()
			List<LocalDateTime> timestamps = entityManager.createQuery(
				"select key(ph.callHistory) " +
				"from Phone ph " +
				"where ph.id = :id ",
				LocalDateTime.class)
			.setParameter("id", id)
			.getResultList();
			//end::hql-collection-implicit-join-example[]
			assertEquals(2, timestamps.size());
		});
	}

	@Test
	public void test_projection_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::jpql-projection-example[]
			List<Object[]> results = entityManager.createQuery(
				"select p.name, p.nickName " +
				"from Person p ",
				Object[].class
			).getResultList();
			for (Object[] result : results) {
				String name = (String) result[0];
				String nickName = (String) result[1];
			}

			List<Tuple> tuples = entityManager.createQuery(
				"select p.name as name, p.nickName as nickName " +
				"from Person p ",
				Tuple.class
			).getResultList();
			for (Tuple tuple : tuples) {
				String name = tuple.get("name", String.class);
				String nickName = tuple.get("nickName", String.class);
			}
			//end::jpql-projection-example[]
		});
	}

	@Test
	public void test_union_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-union-example[]
			List<String> results = entityManager.createQuery(
				"select p.name from Person p " +
				"union " +
				"select p.nickName from Person p where p.nickName is not null",
				String.class
			).getResultList();
			//end::hql-union-example[]
			assertEquals(4, results.size());
	});
}
	@Test
	public void test_jpql_api_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::jpql-api-example[]
			Query query = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name"
			);

			TypedQuery<Person> typedQuery = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name",
				Person.class
			);
			//end::jpql-api-example[]
		});
	}

	@Test
	public void test_jpql_api_named_query_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::jpql-api-named-query-example[]
			Query query = entityManager.createNamedQuery("get_person_by_name");

			TypedQuery<Person> typedQuery = entityManager.createNamedQuery("get_person_by_name", Person.class);
			//end::jpql-api-named-query-example[]
		});
	}

	@Test
	public void test_jpql_api_hibernate_named_query_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::jpql-api-hibernate-named-query-example[]
			Phone phone = entityManager
				.createNamedQuery("get_phone_by_number", Phone.class)
				.setParameter("number", "123-456-7890")
				.getSingleResult();
			//end::jpql-api-hibernate-named-query-example[]
			assertNotNull(phone);
		});
	}

	@Test
	public void test_jpql_api_basic_usage_example() {
		int page = 1;
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::jpql-api-basic-usage-example[]
			Person person = entityManager.createQuery(
					"select p " +
					"from Person p " +
					"where p.name = :name",
					Person.class)
				.setParameter("name", "John Doe")
				.setMaxResults(1)
				.getSingleResult();

			List<Person> people = entityManager.createQuery(
					"select p " +
					"from Person p " +
					"where p.name like :name",
					Person.class)
				.setParameter("name", "J%")
				.setFirstResult(page*10)
				.setMaxResults(10)
				.getResultList();
			//end::jpql-api-basic-usage-example[]
		});
	}

	@Test
	public void test_jpql_api_hint_usage_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::jpql-api-hint-usage-example[]
			Person query = entityManager.createQuery(
					"select p " +
					"from Person p " +
					"where p.name like :name",
					Person.class)
				// timeout - in milliseconds
				.setHint("jakarta.persistence.query.timeout", 2000)
				// flush only at commit time
				.setFlushMode(FlushModeType.COMMIT)
				.setParameter("name", "J%")
				.getSingleResult();
			//end::jpql-api-hint-usage-example[]
		});
	}

	@Test
	public void test_jpql_api_parameter_example_1() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::jpql-api-parameter-example[]
			Query query = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name")
			.setParameter("name", "J%");
			//end::jpql-api-parameter-example[]
		});
	}

	@Test
	public void test_jpql_api_parameter_example_2() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			LocalDateTime timestamp = LocalDateTime.now();
			//tag::jpql-api-parameter-example[]

			Query query = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.createdOn > :timestamp")
			.setParameter("timestamp", timestamp);
			//end::jpql-api-parameter-example[]
		});
	}

	@Test
	public void test_jpql_api_positional_parameter_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::jpql-api-ordinal-parameter-example[]
			TypedQuery<Person> query = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like ?1",
				Person.class)
			.setParameter(1, "J%");
			//end::jpql-api-ordinal-parameter-example[]
		});
	}

	@Test
	public void test_jpql_api_list_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::jpql-api-list-example[]
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name",
				Person.class)
			.setParameter("name", "J%")
			.getResultList();
			//end::jpql-api-list-example[]
		});
	}

	@Test
	public void test_jpql_api_stream_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::jpql-api-stream-example[]
			try(Stream<Person> personStream = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name",
				Person.class)
			.setParameter("name", "J%")
			.getResultStream()) {
				List<Person> persons = personStream
					.skip(5)
					.limit(5)
					.collect(Collectors.toList());
			}
			//end::jpql-api-stream-example[]
		});
	}

	@Test
	public void test_jpql_api_single_result_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::jpql-api-single-result-example[]
			Person person = (Person) entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name")
			.setParameter("name", "J%")
			.getSingleResult();
			//end::jpql-api-single-result-example[]
		});
	}

	@Test
	public void test_hql_api_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			QueryProducer session = entityManager.unwrap(Session.class);
			//tag::hql-api-example[]
			org.hibernate.query.Query<Person> query = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name",
				Person.class);
			//end::hql-api-example[]
		});
	}

	@Test
	public void test_hql_api_named_query_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			QueryProducer session = entityManager.unwrap(Session.class);
			//tag::hql-api-named-query-example[]
			org.hibernate.query.Query<Person> query = session.createNamedQuery(
				"get_person_by_name",
				Person.class);
			//end::hql-api-named-query-example[]
		});
	}

	@Test
	public void test_hql_api_basic_usage_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			QueryProducer session = entityManager.unwrap(Session.class);
			//tag::hql-api-basic-usage-example[]
			org.hibernate.query.Query<Person> query = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name",
				Person.class)
			// timeout - in seconds
			.setTimeout(2)
			// write to L2 caches, but do not read from them
			.setCacheMode(CacheMode.REFRESH)
			// assuming query cache was enabled for the SessionFactory
			.setCacheable(true)
			// add a comment to the generated SQL if enabled via the hibernate.use_sql_comments configuration property
			.setComment("+ INDEX(p idx_person_name)");
			//end::hql-api-basic-usage-example[]
		});
	}

	@Test
	public void test_hql_api_parameter_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			QueryProducer session = entityManager.unwrap(Session.class);
			//tag::hql-api-parameter-example[]
			org.hibernate.query.Query<Person> query = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name",
				Person.class)
			.setParameter("name", "J%", StandardBasicTypes.STRING);
			//end::hql-api-parameter-example[]
		});
	}

	@Test
	public void test_hql_api_parameter_inferred_type_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			QueryProducer session = entityManager.unwrap(Session.class);
			//tag::hql-api-parameter-inferred-type-example[]
			org.hibernate.query.Query<Person> query = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name",
				Person.class)
			.setParameter("name", "J%");
			//end::hql-api-parameter-inferred-type-example[]
		});
	}

	@Test
	public void test_hql_api_parameter_short_form_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			LocalDateTime timestamp = LocalDateTime.now();
			QueryProducer session = entityManager.unwrap(Session.class);
			//tag::hql-api-parameter-short-form-example[]
			org.hibernate.query.Query<Person> query = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name " +
				"  and p.createdOn > :timestamp",
				Person.class)
			.setParameter("name", "J%")
			.setParameter("timestamp", timestamp);
			//end::hql-api-parameter-short-form-example[]
		});
	}

	@Test(expected = IllegalArgumentException.class)
	public void test_hql_api_positional_parameter_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Date timestamp = new Date();
			QueryProducer session = entityManager.unwrap(Session.class);
			//tag::hql-api-ordinal-parameter-example[]
			org.hibernate.query.Query<Person> query = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like ?",
				Person.class)
			.setParameter(1, "J%");
			//end::hql-api-ordinal-parameter-example[]
		});
	}

	@Test
	public void test_hql_api_list_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			QueryProducer session = entityManager.unwrap(Session.class);
			//tag::hql-api-list-example[]
			List<Person> persons = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name",
				Person.class)
			.setParameter("name", "J%")
			.getResultList();
			//end::hql-api-list-example[]
		});
	}

	@Test
	public void test_hql_api_stream_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			QueryProducer session = entityManager.unwrap(Session.class);
			//tag::hql-api-stream-example[]
			try(Stream<Person> persons = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name",
				Person.class)
			.setParameter("name", "J%")
			.getResultStream()) {

				Map<Phone, List<Call>> callRegistry = persons
						.flatMap(person -> person.getPhones().stream())
						.flatMap(phone -> phone.getCalls().stream())
						.collect(Collectors.groupingBy(Call::getPhone));

				process(callRegistry);
			}
			//end::hql-api-stream-example[]
		});
	}

	private void process(Map<Phone, List<Call>> callRegistry) {
	}

	@Test
	public void test_hql_api_stream_projection_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			QueryProducer session = entityManager.unwrap(Session.class);
			//tag::hql-api-stream-projection-example[]
			try (Stream<Object[]> persons = session.createQuery(
				"select p.name, p.nickName " +
				"from Person p " +
				"where p.name like :name",
				Object[].class)
			.setParameter("name", "J%")
			.getResultStream()) {
				persons.map(row -> new PersonNames((String) row[0], (String) row[1]))
					.forEach(this::process);
			}
			//end::hql-api-stream-projection-example[]
		});
	}

	@Test
	public void test_hql_api_scroll_projection_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			QueryProducer session = entityManager.unwrap(Session.class);
			//tag::hql-api-scroll-example[]
			try (ScrollableResults<Person> scrollableResults = session.createQuery(
					"select p " +
					"from Person p " +
					"where p.name like :name",
					Person.class)
					.setParameter("name", "J%")
					.scroll()
			) {
				while (scrollableResults.next()) {
					Person person = scrollableResults.get();
					process(person);
				}
			}
			//end::hql-api-scroll-example[]
		});
	}

	@Test
	public void test_hql_api_scroll_open_example() {
		ScrollableResults<Person> scrollableResults = doInJPA(this::entityManagerFactory, entityManager -> {
			QueryProducer session = entityManager.unwrap(Session.class);
			return session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name",
				Person.class)
			.setParameter("name", "J%")
			.scroll();
		});
		try {
			scrollableResults.next();
			fail("Should throw exception because the ResultSet must be closed by now!");
		}
		catch (Exception expected) {
		}
	}

	private void process(Person person) {
	}

	private void process(PersonNames personName) {
	}

	@Test
	public void test_hql_api_unique_result_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Session session = entityManager.unwrap(Session.class);
			//tag::hql-api-unique-result-example[]
			Person person = session.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like :name",
				Person.class)
			.setParameter("name", "J%")
			.getSingleResult();
			//end::hql-api-unique-result-example[]
		});
	}

	@Test
	public void test_hql_string_literals_example_1() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-string-literals-example[]
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like 'Joe'",
				Person.class)
			.getResultList();
			//end::hql-string-literals-example[]
		});
	}

	@Test
	public void test_hql_string_literals_example_2() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-string-literals-example[]

			// Escaping quotes
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like 'Joe''s'",
				Person.class)
			.getResultList();
			//end::hql-string-literals-example[]
		});
	}

	@Test
	public void test_hql_string_literals_example_3() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-numeric-literals-example[]
			// simple integer literal
			Person person = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.id = 1",
				Person.class)
			.getSingleResult();
			//end::hql-numeric-literals-example[]
		});
	}

	@Test
	public void test_hql_string_literals_example_4() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-numeric-literals-example[]

			// simple integer literal, typed as a long
			Person person = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.id = 1L",
				Person.class)
			.getSingleResult();
			//end::hql-numeric-literals-example[]
		});
	}

	@Test
	public void test_hql_string_literals_example_5() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-numeric-literals-example[]

			// decimal notation
			List<Call> calls = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"where c.duration > 100.5",
				Call.class)
			.getResultList();
			//end::hql-numeric-literals-example[]
		});
	}

	@Test
	public void test_hql_string_literals_example_6() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-numeric-literals-example[]

			// decimal notation, typed as a float
			List<Call> calls = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"where c.duration > 100.5F",
				Call.class)
			.getResultList();
			//end::hql-numeric-literals-example[]
		});
	}

	@Test
	public void test_hql_string_literals_example_7() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-numeric-literals-example[]

			// scientific notation
			List<Call> calls = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"where c.duration > 1e+2",
				Call.class)
			.getResultList();
			//end::hql-numeric-literals-example[]
		});
	}

	@Test
	public void test_hql_string_literals_example_8() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-numeric-literals-example[]

			// scientific notation, typed as a float
			List<Call> calls = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"where c.duration > 1e+2F",
				Call.class)
			.getResultList();
			//end::hql-numeric-literals-example[]
		});

	}

	@Test
	public void test_hql_java_constant_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-java-constant-example[]
			// select clause date/time arithmetic operations
			Double pi = entityManager.createQuery(
				"select java.lang.Math.PI",
				Double.class)
			.getSingleResult();
			//end::hql-java-constant-example[]
			assertEquals(java.lang.Math.PI, pi, 1e-9);
		});
	}

	@Test
	public void test_hql_enum_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-enum-example[]
			// select clause date/time arithmetic operations
			List<Phone> phones1 = entityManager.createQuery(
				"from Phone ph " +
				"where ph.type = LAND_LINE",
				Phone.class)
				.getResultList();
			//end::hql-enum-example[]
		});
	}


	@Test
	public void test_hql_numeric_arithmetic_example_1() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-numeric-arithmetic-example[]
			// select clause date/time arithmetic operations
			Long duration = entityManager.createQuery(
				"select sum(ch.duration) * :multiplier " +
				"from Person pr " +
				"join pr.phones ph " +
				"join ph.callHistory ch " +
				"where ph.id = 1L ",
				Long.class)
			.setParameter("multiplier", 1000L)
			.getSingleResult();
			//end::hql-numeric-arithmetic-example[]
			assertTrue(duration > 0);
		});
	}

	@Test
	public void test_hql_numeric_arithmetic_example_2() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-numeric-arithmetic-example[]

			// select clause date/time arithmetic operations
			Integer years = entityManager.createQuery(
				"select year(local date) - year(p.createdOn) " +
				"from Person p " +
				"where p.id = 1L",
				Integer.class)
			.getSingleResult();
			//end::hql-numeric-arithmetic-example[]
			assertTrue(years > 0);
		});
	}

	@Test
	public void test_hql_numeric_arithmetic_example_3() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-numeric-arithmetic-example[]

			// where clause arithmetic operations
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where year(local date) - year(p.createdOn) > 1",
				Person.class)
			.getResultList();
			//end::hql-numeric-arithmetic-example[]
			assertTrue(persons.size() > 0);
		});
	}

	@Test
	public void test_hql_concatenation_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-concatenation-example[]
			String name = entityManager.createQuery(
				"select 'Customer ' || p.name " +
				"from Person p " +
				"where p.id = 1",
				String.class)
			.getSingleResult();
			//end::hql-concatenation-example[]
			assertNotNull(name);
		});
	}

	@Test
	public void test_hql_aggregate_functions_example_1() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-aggregate-functions-example[]
			Object[] callStatistics = entityManager.createQuery(
				"select " +
				"	count(c), " +
				"	sum(c.duration), " +
				"	min(c.duration), " +
				"	max(c.duration), " +
				"	avg(c.duration)  " +
				"from Call c ",
				Object[].class)
			.getSingleResult();
			//end::hql-aggregate-functions-example[]
			assertNotNull(callStatistics);
		});
	}

	@Test
	public void test_hql_aggregate_functions_example_2() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-aggregate-functions-example[]

			Long phoneCount = entityManager.createQuery(
				"select count(distinct c.phone) " +
				"from Call c ",
				Long.class)
			.getSingleResult();
			//end::hql-aggregate-functions-example[]
			assertNotNull(phoneCount);
		});
	}

	@Test
	public void test_hql_aggregate_functions_example_3() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-aggregate-functions-example[]

			List<Object[]> callCount = entityManager.createQuery(
				"select p.number, count(c) " +
				"from Call c " +
				"join c.phone p " +
				"group by p.number",
				Object[].class)
			.getResultList();
			//end::hql-aggregate-functions-example[]
			assertNotNull(callCount.get(0));
		});
	}

	@Test
	public void test_hql_aggregate_functions_simple_filter_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-aggregate-functions-simple-filter-example[]

			List<Long> callCount = entityManager.createQuery(
				"select count(c) filter (where c.duration < 30) " +
				"from Call c ",
				Long.class)
			.getResultList();
			//end::hql-aggregate-functions-simple-filter-example[]
			assertNotNull(callCount.get(0));
		});
	}

	@Test
	public void test_hql_aggregate_functions_filter_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-aggregate-functions-filter-example[]

			List<Object[]> callCount = entityManager.createQuery(
				"select p.number, count(c) filter (where c.duration < 30) " +
				"from Call c " +
				"join c.phone p " +
				"group by p.number",
				Object[].class)
			.getResultList();
			//end::hql-aggregate-functions-filter-example[]
			assertNotNull(callCount.get(0));
		});
	}

	@Test
	@SkipForDialect(dialectClass = DerbyDialect.class)
	@SkipForDialect(dialectClass = SybaseASEDialect.class)
	@SkipForDialect(dialectClass = FirebirdDialect.class, reason = "order by not supported in list")
	@SkipForDialect(dialectClass = InformixDialect.class)
	public void test_hql_aggregate_functions_within_group_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-aggregate-functions-within-group-example[]

			List<String> callCount = entityManager.createQuery(
				"select listagg(p.number, ', ') within group (order by p.type,p.number) " +
				"from Phone p " +
				"group by p.person",
				String.class)
			.getResultList();
			//end::hql-aggregate-functions-within-group-example[]
			assertNotNull(callCount.get(0));
		});
	}

	@Test
	@SkipForDialect(dialectClass = DerbyDialect.class, reason = "See https://issues.apache.org/jira/browse/DERBY-2072")
	public void test_hql_concat_function_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-concat-function-example[]
			List<String> callHistory = entityManager.createQuery(
				"select concat(p.number, ' : ' , cast(c.duration as string)) " +
				"from Call c " +
				"join c.phone p",
				String.class)
			.getResultList();
			//end::hql-concat-function-example[]
			assertEquals(2, callHistory.size());
		});
	}

	@Test
	public void test_hql_substring_function_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-substring-function-example[]

			// JPQL-style
			List<String> prefixes = entityManager.createQuery(
				"select substring(p.number, 1, 2) " +
				"from Call c " +
				"join c.phone p",
				String.class)
				.getResultList();

			// ANSI SQL-style
			List<String> prefixes2 = entityManager.createQuery(
				"select substring(p.number from 1 for 2) " +
				"from Call c " +
				"join c.phone p",
				String.class)
			.getResultList();
			//end::hql-substring-function-example[]
			assertEquals(2, prefixes.size());
			assertEquals(2, prefixes2.size());
		});
	}

	@Test
	public void test_hql_upper_function_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-upper-function-example[]
			List<String> names = entityManager.createQuery(
				"select upper(p.name) " +
				"from Person p ",
				String.class)
			.getResultList();
			//end::hql-upper-function-example[]
			assertEquals(3, names.size());
		});
	}

	@Test
	public void test_hql_lower_function_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-lower-function-example[]
			List<String> names = entityManager.createQuery(
				"select lower(p.name) " +
				"from Person p ",
				String.class)
			.getResultList();
			//end::hql-lower-function-example[]
			assertEquals(3, names.size());
		});
	}

	@Test
	public void test_hql_trim_function_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-trim-function-example[]

			// trim whitespace from both ends
			List<String> names1 = entityManager.createQuery(
				"select trim(p.name) " +
				"from Person p ",
				String.class)
			.getResultList();

			// trim leading spaces
			List<String> names2 = entityManager.createQuery(
				"select trim(leading ' ' from p.name) " +
				"from Person p ",
				String.class)
			.getResultList();
			//end::hql-trim-function-example[]
			assertEquals(3, names1.size());
			assertEquals(3, names2.size());
		});
	}

	@Test
	public void test_hql_length_function_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-length-function-example[]
			List<Integer> lengths = entityManager.createQuery(
				"select length(p.name) " +
				"from Person p ",
				Integer.class)
			.getResultList();
			//end::hql-length-function-example[]
			assertEquals(3, lengths.size());
		});
	}

	@Test
	public void test_hql_locate_function_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-locate-function-example[]
			List<Integer> sizes = entityManager.createQuery(
				"select locate('John', p.name) " +
				"from Person p ",
				Integer.class)
			.getResultList();
			//end::hql-locate-function-example[]
			assertEquals(3, sizes.size());
		});
	}

	@Test
	public void test_hql_position_function_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-position-function-example[]
			List<Integer> sizes = entityManager.createQuery(
				"select position('John' in p.name) " +
				"from Person p ",
				Integer.class)
			.getResultList();
			//end::hql-position-function-example[]
			assertEquals(3, sizes.size());
		});
	}

	@Test
	public void test_hql_abs_function_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-abs-function-example[]
			List<Integer> abs = entityManager.createQuery(
				"select abs(c.duration) " +
				"from Call c ",
				Integer.class)
			.getResultList();
			//end::hql-abs-function-example[]
			assertEquals(2, abs.size());
		});
	}

	@Test
	public void test_hql_mod_function_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-mod-function-example[]
			List<Integer> mods = entityManager.createQuery(
				"select mod(c.duration, 10) " +
				"from Call c ",
				Integer.class)
			.getResultList();
			//end::hql-mod-function-example[]
			assertEquals(2, mods.size());
		});
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "https://github.com/cockroachdb/cockroach/issues/26710")
	public void test_hql_sqrt_function_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-sqrt-function-example[]
			List<Double> sqrts = entityManager.createQuery(
				"select sqrt(c.duration) " +
				"from Call c ",
				Double.class)
			.getResultList();
			//end::hql-sqrt-function-example[]
			assertEquals(2, sqrts.size());
		});
	}

	@Test
	@SkipForDialect(dialectClass = SQLServerDialect.class)
	@SkipForDialect(dialectClass = DerbyDialect.class, reason = "Comparisons between 'DATE' and 'TIMESTAMP' are not supported")
	public void test_hql_current_date_function_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-current-date-function-example[]
			List<Call> calls = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"where c.timestamp = current_date",
				Call.class)
			.getResultList();
			//end::hql-current-date-function-example[]
			assertEquals(0, calls.size());
		});
	}

	@Test
	public void test_hql_current_date_function_example_sql_server() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-extract-function-example[]
			List<Call> calls = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"where extract(date from c.timestamp) = local date",
				Call.class)
			.getResultList();

			//end::hql-extract-function-example[]
			assertEquals(0, calls.size());
		});
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	public void test_hql_current_time_function_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-current-time-function-example[]
			List<Call> calls = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"where c.timestamp = current_time",
				Call.class)
			.getResultList();
			//end::hql-current-time-function-example[]
			assertEquals(0, calls.size());
		});
	}

	@Test
	public void test_hql_current_timestamp_function_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-current-timestamp-function-example[]
			List<Call> calls = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"where c.timestamp = current_timestamp",
				Call.class)
			.getResultList();
			//end::hql-current-timestamp-function-example[]
			assertEquals(0, calls.size());
		});
	}

	@Test
//	@RequiresDialect({MySQLDialect.class, PostgreSQLDialect.class, H2Dialect.class, DerbyDialect.class, OracleDialect.class})
	public void test_var_function_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-native-function-example[]
			// careful: these functions are not supported on all databases!

			List<Tuple> variances = entityManager.createQuery(
				"select var_samp(c.duration) as sampvar, var_pop(c.duration) as popvar " +
				"from Call c ",
				Tuple.class)
			.getResultList();
			//end::hql-native-function-example[]
		});
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	@RequiresDialect(MySQLDialect.class)
	@RequiresDialect(PostgreSQLDialect.class)
	@RequiresDialect(OracleDialect.class)
	public void test_hql_bit_length_function_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-native-function-example[]

			List<Number> bits = entityManager.createQuery(
				"select bit_length(c.phone.number) " +
				"from Call c ",
				Number.class)
			.getResultList();
			//end::hql-native-function-example[]
			assertEquals(2, bits.size());
		});
	}

	@Test
	public void test_hql_cast_function_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-cast-function-example[]
			List<String> durations = entityManager.createQuery(
				"select cast(c.duration as String) " +
				"from Call c ",
				String.class)
			.getResultList();
			//end::hql-cast-function-example[]
			assertEquals(2, durations.size());
		});
	}

	@Test
	public void test_hql_extract_function_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-extract-function-example[]
			List<Integer> years = entityManager.createQuery(
				"select extract(year from c.timestamp) " +
				"from Call c ",
				Integer.class)
			.getResultList();
			//end::hql-extract-function-example[]
			assertEquals(2, years.size());
		});
	}

	@Test
	public void test_hql_year_function_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-year-function-example[]
			List<Integer> years = entityManager.createQuery(
				"select year(c.timestamp) " +
				"from Call c ",
				Integer.class)
			.getResultList();
			//end::hql-year-function-example[]
			assertEquals(2, years.size());
		});
	}

	@Test
	@SkipForDialect(dialectClass = SQLServerDialect.class)
	public void test_hql_str_function_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-str-function-example[]
			List<String> timestamps = entityManager.createQuery(
				"select str(c.timestamp) " +
				"from Call c ",
				String.class)
			.getResultList();
			//end::hql-str-function-example[]
			assertEquals(2, timestamps.size());
		});
	}

	@Test
	@RequiresDialect(SQLServerDialect.class)
	public void test_hql_str_function_example_sql_server() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-str-function-example[]
			// Special SQL Server function "str" that converts floats
			List<String> timestamps = entityManager.createQuery(
				"select str(cast(duration as float) / 60, 4, 2) " +
				"from Call c ",
				String.class)
			.getResultList();
			//end::hql-str-function-example[]
			assertEquals(2, timestamps.size());
		});
	}

	@Test
	public void test_hql_collection_expressions_example_1() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Call call = entityManager.createQuery("select c from Call c", Call.class).getResultList().get(1);
			//tag::hql-collection-expressions-example[]
			List<Phone> phones = entityManager.createQuery(
				"select p " +
				"from Phone p " +
				"where max(elements(p.calls)) = :call",
				Phone.class)
			.setParameter("call", call)
			.getResultList();
			//end::hql-collection-expressions-example[]
			assertEquals(1, phones.size());
		});
	}

	@Test
	public void test_hql_collection_expressions_example_2() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Call call = entityManager.createQuery("select c from Call c", Call.class).getResultList().get(0);
			//tag::hql-collection-expressions-example[]

			List<Phone> phones = entityManager.createQuery(
				"select p " +
				"from Phone p " +
				"where min(elements(p.calls)) = :call",
				Phone.class)
			.setParameter("call", call)
			.getResultList();
			//end::hql-collection-expressions-example[]
			assertEquals(1, phones.size());
		});
	}

	@Test
	public void test_hql_collection_expressions_example_3() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-collection-expressions-example[]

			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where max(indices(p.phones)) = 0",
				Person.class)
			.getResultList();
			//end::hql-collection-expressions-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_collection_expressions_example_5() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Call call = entityManager.createQuery("select c from Call c", Call.class).getResultList().get(0);
			Phone phone = call.getPhone();
			//tag::hql-collection-expressions-some-example[]

			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where :phone = some elements(p.phones)",
				Person.class)
			.setParameter("phone", phone)
			.getResultList();
			//end::hql-collection-expressions-some-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_collection_expressions_example_4() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			Call call = entityManager.createQuery("select c from Call c", Call.class).getResultList().get(0);
			Phone phone = call.getPhone();
			//tag::hql-collection-expressions-some-example[]

			// the above query can be re-written with member of
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where :phone member of p.phones",
				Person.class)
			.setParameter("phone", phone)
			.getResultList();
			//end::hql-collection-expressions-some-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_collection_expressions_example_6() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-collection-expressions-exists-example[]

			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where exists elements(p.phones)",
				Person.class)
			.getResultList();
			//end::hql-collection-expressions-exists-example[]
			assertEquals(2, persons.size());
		});
	}

	@Test
	@SkipForDialect(dialectClass = DerbyDialect.class, reason = "Comparisons between 'DATE' and 'TIMESTAMP' are not supported")
	public void test_hql_collection_expressions_example_8() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-collection-expressions-all-example[]

			List<Phone> phones = entityManager.createQuery(
				"select p " +
				"from Phone p " +
				"where local date > all elements(p.repairTimestamps)",
				Phone.class)
			.getResultList();
			//end::hql-collection-expressions-all-example[]
			assertEquals(3, phones.size());
		});
	}

	@Test
	public void test_hql_collection_expressions_example_9() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-collection-expressions-in-example[]

			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where 1 in indices(p.phones)",
				Person.class)
			.getResultList();
			//end::hql-collection-expressions-in-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_collection_expressions_example_10() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-size-example[]

			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where size(p.phones) >= 2",
				Person.class)
			.getResultList();
			//end::hql-size-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_collection_index_operator_example_1() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-collection-index-operator-example[]
			// indexed lists
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.phones[0].type = LAND_LINE",
				Person.class)
			.getResultList();
			//end::hql-collection-index-operator-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_collection_index_operator_example_2() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			String address = "Home address";
			//tag::hql-collection-index-operator-example[]

			// maps
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.addresses['HOME'] = :address",
				Person.class)
			.setParameter("address", address)
			.getResultList();
			//end::hql-collection-index-operator-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	@RequiresDialectFeature(DialectChecks.SupportsSubqueryInOnClause.class)
	public void test_hql_collection_index_operator_example_3() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-collection-index-operator-example[]

			//max index in list
			List<Person> persons = entityManager.createQuery(
				"select pr " +
				"from Person pr " +
				"where pr.phones[max(indices(pr.phones))].type = 'LAND_LINE'",
				Person.class)
			.getResultList();
			//end::hql-collection-index-operator-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_polymorphism_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-polymorphism-example[]
			List<Payment> payments = entityManager.createQuery(
				"select p " +
				"from Payment p ",
				Payment.class)
			.getResultList();
			//end::hql-polymorphism-example[]
			assertEquals(2, payments.size());
		});
	}

	@Test
	public void test_hql_treat_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-treat-example[]
			List<Payment> payments = entityManager.createQuery(
				"select p " +
				"from Payment p " +
				"where length(treat(p as CreditCardPayment).cardNumber) between 16 and 20",
				Payment.class)
			.getResultList();
			//end::hql-treat-example[]
			assertEquals(1, payments.size());
		});
	}

	@Test
	public void test_hql_join_many_treat_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-join-treat-example[]
			// a to-many association
			List<Object[]> payments = entityManager.createQuery(
				"select a, ccp " +
				"from Account a " +
				"join treat(a.payments as CreditCardPayment) ccp " +
				"where length(ccp.cardNumber) between 16 and 20",
				Object[].class)
			.getResultList();
			//end::hql-join-treat-example[]
			assertEquals(1, payments.size());
		});
	}

	@Test
	public void test_hql_join_one_treat_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-join-treat-example[]

			// a to-one association
			List<Object[]> payments = entityManager.createQuery(
				"select c, ccp " +
				"from Call c " +
				"join treat(c.payment as CreditCardPayment) ccp " +
				"where length(ccp.cardNumber) between 16 and 20",
				Object[].class)
			.getResultList();
			//end::hql-join-treat-example[]
			assertEquals(1, payments.size());
		});
	}

	@Test
	public void test_hql_entity_type_exp_example_1() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-entity-type-exp-example[]
			List<Payment> payments = entityManager.createQuery(
				"select p " +
				"from Payment p " +
				"where type(p) = CreditCardPayment",
				Payment.class)
			.getResultList();
			//end::hql-entity-type-exp-example[]
			assertEquals(1, payments.size());
		});
	}

	@Test
	public void test_hql_entity_type_exp_example_2() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-entity-type-exp-example[]

			// using a parameter instead of a literal entity type
			List<Payment> payments = entityManager.createQuery(
				"select p " +
				"from Payment p " +
				"where type(p) = :type",
				Payment.class)
			.setParameter("type", WireTransferPayment.class)
			.getResultList();
			//end::hql-entity-type-exp-example[]
			assertEquals(1, payments.size());
		});
	}

	@Test
	public void test_simple_case_expressions_example_1() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-simple-case-expressions-example[]
			List<String> nickNames = entityManager.createQuery(
				"select " +
				"	case p.nickName " +
				"	when 'NA' " +
				"	then '<no nick name>' " +
				"	else p.nickName " +
				"	end " +
				"from Person p",
				String.class)
			.getResultList();
			//end::hql-simple-case-expressions-example[]
			assertEquals(3, nickNames.size());
		});
	}

	@Test
	public void test_simple_case_expressions_example_2() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-coalesce-example[]
			List<String> nickNames = entityManager.createQuery(
				"select coalesce(p.nickName, '<no nick name>') " +
				"from Person p",
				String.class)
			.getResultList();
			//end::hql-coalesce-example[]
			assertEquals(3, nickNames.size());
		});
	}

	@Test
	public void test_searched_case_expressions_example_1() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-searched-case-expressions-example[]
			List<String> nickNames = entityManager.createQuery(
				"select " +
				"	case " +
				"	when p.nickName is null " +
				"	then " +
				"		case " +
				"		when p.name is null " +
				"		then '<no nick name>' " +
				"		else p.name " +
				"		end" +
				"	else p.nickName " +
				"	end " +
				"from Person p",
				String.class)
			.getResultList();
			//end::hql-searched-case-expressions-example[]
			assertEquals(3, nickNames.size());
		});
	}

	@Test
	public void test_searched_case_expressions_example_2() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-coalesce-example[]

			List<String> nickNames = entityManager.createQuery(
				"select coalesce(p.nickName, p.name, '<no nick name>') " +
				"from Person p",
				String.class)
			.getResultList();
			//end::hql-coalesce-example[]
			assertEquals(3, nickNames.size());
		});
	}

	@Test
	public void test_case_arithmetic_expressions_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-case-arithmetic-expressions-example[]
			List<Long> values = entityManager.createQuery(
				"select " +
				"	case when p.nickName is null " +
				"		 then p.id * 1000 " +
				"		 else p.id " +
				"	end " +
				"from Person p " +
				"order by p.id",
				Long.class)
			.getResultList();

			assertEquals(3, values.size());
			assertEquals(1L, (long) values.get(0));
			assertEquals(2000, (long) values.get(1));
			assertEquals(3000, (long) values.get(2));
			//end::hql-case-arithmetic-expressions-example[]
		});
	}

	@Test
	public void test_hql_null_if_example_1() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-nullif-example[]
			List<String> nickNames = entityManager.createQuery(
				"select nullif(p.nickName, p.name) " +
				"from Person p",
				String.class)
			.getResultList();
			//end::hql-nullif-example[]
			assertEquals(3, nickNames.size());
		});
	}

	@Test
	public void test_hql_null_if_example_2() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-nullif-example[]

			// equivalent CASE expression
			List<String> nickNames = entityManager.createQuery(
				"select " +
				"	case" +
				"	when p.nickName = p.name" +
				"	then null" +
				"	else p.nickName" +
				"	end " +
				"from Person p",
				String.class)
			.getResultList();
			//end::hql-nullif-example[]
			assertEquals(3, nickNames.size());
		});
	}

	@Test
	public void test_hql_select_clause_dynamic_instantiation_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-select-clause-dynamic-instantiation-example[]
			CallStatistics callStatistics = entityManager.createQuery(
				"select new org.hibernate.orm.test.hql.CallStatistics(" +
				"	count(c), " +
				"	sum(c.duration), " +
				"	min(c.duration), " +
				"	max(c.duration), " +
				"	avg(c.duration)" +
				")  " +
				"from Call c ",
				CallStatistics.class)
			.getSingleResult();
			//end::hql-select-clause-dynamic-instantiation-example[]
			assertNotNull(callStatistics);
		});
	}

	@Test
	public void test_hql_select_clause_dynamic_list_instantiation_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-select-clause-dynamic-list-instantiation-example[]
			List<List> phoneCallDurations = entityManager.createQuery(
				"select new list(" +
				"	p.number, " +
				"	c.duration " +
				")  " +
				"from Call c " +
				"join c.phone p ",
				List.class)
			.getResultList();
			//end::hql-select-clause-dynamic-list-instantiation-example[]
			assertNotNull(phoneCallDurations);
		});
	}

	@Test
	public void test_hql_select_clause_dynamic_map_instantiation_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-select-clause-dynamic-map-instantiation-example[]
			List<Map> phoneCallTotalDurations = entityManager.createQuery(
				"select new map(" +
				"	p.number as phoneNumber , " +
				"	sum(c.duration) as totalDuration, " +
				"	avg(c.duration) as averageDuration " +
				")  " +
				"from Call c " +
				"join c.phone p " +
				"group by p.number ",
				Map.class)
			.getResultList();
			//end::hql-select-clause-dynamic-map-instantiation-example[]
			assertNotNull(phoneCallTotalDurations);
		});
	}

	@Test
	public void test_hql_relational_comparisons_example_1() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-relational-comparisons-example[]
			// numeric comparison
			List<Call> calls = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"where c.duration < 30 ",
				Call.class)
			.getResultList();
			//end::hql-relational-comparisons-example[]
			assertEquals(1, calls.size());
		});
	}

	@Test
	public void test_hql_relational_comparisons_example_2() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-relational-comparisons-example[]

			// string comparison
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like 'John%' ",
				Person.class)
			.getResultList();
			//end::hql-relational-comparisons-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	@RequiresDialect(PostgreSQLDialect.class)
	@RequiresDialect(MySQLDialect.class)
	public void test_hql_relational_comparisons_example_3() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-relational-comparisons-example[]

			// datetime comparison
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.createdOn > date 1950-01-01 ",
				Person.class)
			.getResultList();
			//end::hql-relational-comparisons-example[]
			assertEquals(2, persons.size());
		});
	}

	@Test
	public void test_hql_relational_comparisons_example_4() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-relational-comparisons-example[]

			// enum comparison
			List<Phone> phones = entityManager.createQuery(
				"select p " +
				"from Phone p " +
				"where p.type = 'MOBILE' ",
				Phone.class)
			.getResultList();
			//end::hql-relational-comparisons-example[]
			assertEquals(1, phones.size());
		});
	}

	@Test
	public void test_hql_relational_comparisons_example_5() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-relational-comparisons-example[]

			// boolean comparison
			List<Payment> payments = entityManager.createQuery(
				"select p " +
				"from Payment p " +
				"where p.completed = true ",
				Payment.class)
			.getResultList();
			//end::hql-relational-comparisons-example[]
			assertEquals(2, payments.size());
		});
	}

	@Test
	public void test_hql_relational_comparisons_example_6() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-relational-comparisons-example[]

			// boolean comparison
			List<Payment> payments = entityManager.createQuery(
				"select p " +
				"from Payment p " +
				"where type(p) = WireTransferPayment ",
				Payment.class)
			.getResultList();
			//end::hql-relational-comparisons-example[]
			assertEquals(1, payments.size());
		});
	}

	@Test
	public void test_hql_relational_comparisons_example_7() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-relational-comparisons-example[]

			// entity value comparison
			List<Object[]> phonePayments = entityManager.createQuery(
				"select p " +
				"from Payment p, Phone ph " +
				"where p.person = ph.person ",
				Object[].class)
			.getResultList();
			//end::hql-relational-comparisons-example[]
			assertEquals(3, phonePayments.size());
		});
	}

	@Test
	public void test_hql_all_subquery_comparison_qualifier_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-all-subquery-comparison-qualifier-example[]
			// select all persons with all calls shorter than 50 seconds
			List<Person> persons = entityManager.createQuery(
				"select distinct p.person " +
				"from Phone p " +
				"join p.calls c " +
				"where 50 > all (" +
				"	select duration" +
				"	from Call" +
				"	where phone = p " +
				") ", Person.class)
			.getResultList();
			//end::hql-all-subquery-comparison-qualifier-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_null_predicate_example_1() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-null-predicate-example[]
			// select all persons with a nickname
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.nickName is not null",
				Person.class)
			.getResultList();
			//end::hql-null-predicate-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_null_predicate_example_2() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-null-predicate-example[]

			// select all persons without a nickname
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.nickName is null",
				Person.class)
			.getResultList();
			//end::hql-null-predicate-example[]
			assertEquals(2, persons.size());
		});
	}

	@Test
	public void test_hql_like_predicate_example_1() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-like-predicate-example[]
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like 'Jo%'",
				Person.class)
			.getResultList();
			//end::hql-like-predicate-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_like_predicate_example_2() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-like-predicate-example[]

			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name not like 'Jo%'",
				Person.class)
			.getResultList();
			//end::hql-like-predicate-example[]
			assertEquals(2, persons.size());
		});
	}

	@Test
	public void test_hql_like_predicate_escape_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-like-predicate-escape-example[]
			// find any person with a name starting with "Dr_"
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name like 'Dr|_%' escape '|'",
				Person.class)
			.getResultList();
			//end::hql-like-predicate-escape-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_between_predicate_example_1() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-between-predicate-example[]
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"join p.phones ph " +
				"where p.id = 1L and index(ph) between 0 and 3",
				Person.class)
			.getResultList();
			//end::hql-between-predicate-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	@RequiresDialect(PostgreSQLDialect.class)
	@RequiresDialect(MySQLDialect.class)
	public void test_hql_between_predicate_example_2() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-between-predicate-example[]

			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.createdOn between date 1999-01-01 and date 2001-01-02",
				Person.class)
			.getResultList();
			//end::hql-between-predicate-example[]
			assertEquals(2, persons.size());
		});
	}

	@Test
	public void test_hql_between_predicate_example_3() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-between-predicate-example[]

			List<Call> calls = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"where c.duration between 5 and 20",
				Call.class)
			.getResultList();
			//end::hql-between-predicate-example[]
			assertEquals(1, calls.size());
		});
	}

	@Test
	public void test_hql_between_predicate_example_4() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-between-predicate-example[]

			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.name between 'H' and 'M'",
				Person.class)
			.getResultList();
			//end::hql-between-predicate-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_in_predicate_example_1() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-in-predicate-example[]
			List<Payment> payments = entityManager.createQuery(
				"select p " +
				"from Payment p " +
				"where type(p) in (CreditCardPayment, WireTransferPayment)",
				Payment.class)
			.getResultList();
			//end::hql-in-predicate-example[]
			assertEquals(2, payments.size());
		});
	}

	@Test
	public void test_hql_in_predicate_example_2() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-in-predicate-example[]

			List<Phone> phones = entityManager.createQuery(
				"select p " +
				"from Phone p " +
				"where type in ('MOBILE', 'LAND_LINE')",
				Phone.class)
			.getResultList();
			//end::hql-in-predicate-example[]
			assertEquals(3, phones.size());
		});
	}

	@Test
	public void test_hql_in_predicate_example_3() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-in-predicate-example[]

			List<Phone> phones = entityManager.createQuery(
				"select p " +
				"from Phone p " +
				"where type in :types",
				Phone.class)
			.setParameter("types", Arrays.asList(PhoneType.MOBILE, PhoneType.LAND_LINE))
			.getResultList();
			//end::hql-in-predicate-example[]
			assertEquals(3, phones.size());
		});
	}

	@Test
	public void test_hql_in_predicate_example_4() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-in-predicate-example[]

			List<Phone> phones = entityManager.createQuery(
				"select distinct p " +
				"from Phone p " +
				"where p.person.id in (" +
				"	select py.person.id " +
				"	from Payment py" +
				"	where py.completed = true and py.amount > 50 " +
				")",
				Phone.class)
			.getResultList();
			//end::hql-in-predicate-example[]
			assertEquals(2, phones.size());
		});
	}

	@Test
	public void test_hql_in_predicate_example_5() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-in-predicate-example[]

			// Not JPQL compliant!
			List<Phone> phones = entityManager.createQuery(
				"select distinct p " +
				"from Phone p " +
				"where p.person in (" +
				"	select py.person " +
				"	from Payment py" +
				"	where py.completed = true and py.amount > 50 " +
				")",
				Phone.class)
			.getResultList();
			//end::hql-in-predicate-example[]
			assertEquals(2, phones.size());
		});
	}


	@Test
	public void test_hql_in_predicate_example_6() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-in-predicate-example[]

			// Not JPQL compliant!
			List<Payment> payments = entityManager.createQuery(
				"select distinct p " +
				"from Payment p " +
				"where (p.amount, p.completed) in (" +
				"	(50, true)," +
				"	(100, true)," +
				"	(5, false)" +
				")", Payment.class)
			.getResultList();
			//end::hql-in-predicate-example[]
			assertEquals(1, payments.size());
		});
	}

	@Test
	public void test_hql_empty_collection_predicate_example_1() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-empty-collection-predicate-example[]
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.phones is empty",
				Person.class)
			.getResultList();
			//end::hql-empty-collection-predicate-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_empty_collection_predicate_example_2() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-empty-collection-predicate-example[]

			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.phones is not empty",
				Person.class)
			.getResultList();
			//end::hql-empty-collection-predicate-example[]
			assertEquals(2, persons.size());
		});
	}

	@Test
	public void test_hql_member_of_collection_predicate_example_1() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-member-of-collection-predicate-example[]
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where 'Home address' member of p.addresses",
				Person.class)
			.getResultList();
			//end::hql-member-of-collection-predicate-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_hql_member_of_collection_predicate_example_2() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-member-of-collection-predicate-example[]

			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where 'Home address' not member of p.addresses",
				Person.class)
			.getResultList();
			//end::hql-member-of-collection-predicate-example[]
			assertEquals(2, persons.size());
		});
	}

	@Test
	public void test_hql_group_by_example_1() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-group-by-example[]
			Long totalDuration = entityManager.createQuery(
				"select sum(c.duration) " +
				"from Call c ",
				Long.class)
			.getSingleResult();
			//end::hql-group-by-example[]
			assertEquals(Long.valueOf(45), totalDuration);
		});
	}

	@Test
	public void test_hql_group_by_example_2() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-group-by-example[]

			List<Object[]> personTotalCallDurations = entityManager.createQuery(
				"select p.name, sum(c.duration) " +
				"from Call c " +
				"join c.phone ph " +
				"join ph.person p " +
				"group by p.name",
				Object[].class)
			.getResultList();
			//end::hql-group-by-example[]
			assertEquals(1, personTotalCallDurations.size());
		});
	}

	@Test
	public void test_hql_group_by_example_3() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-group-by-example[]

			//It's even possible to group by entities!
			List<Object[]> personTotalCallDurations = entityManager.createQuery(
				"select p, sum(c.duration) " +
				"from Call c " +
				"join c.phone ph " +
				"join ph.person p " +
				"group by p",
				Object[].class)
			.getResultList();
			//end::hql-group-by-example[]
			assertEquals(1, personTotalCallDurations.size());
		});
	}

	@Test
	public void test_hql_group_by_example_4() {

		doInJPA(this::entityManagerFactory, entityManager -> {

			Call call11 = new Call();
			call11.setDuration(10);
			call11.setTimestamp(LocalDateTime.of(2000, 1, 1, 0, 0, 0));

			Phone phone = entityManager.createQuery("select p from Phone p where p.calls is empty ", Phone.class).getResultList().get(0);

			phone.addCall(call11);
			entityManager.flush();
			entityManager.clear();

			List<Object[]> personTotalCallDurations = entityManager.createQuery(
				"select p, sum(c.duration) " +
				"from Call c " +
				"join c.phone p " +
				"group by p", Object[].class)
			.getResultList();
			assertEquals(2, personTotalCallDurations.size());
		});
	}

	@Test
	public void test_hql_group_by_having_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-group-by-having-example[]

			List<Object[]> personTotalCallDurations = entityManager.createQuery(
				"select p.name, sum(c.duration) " +
				"from Call c " +
				"join c.phone ph " +
				"join ph.person p " +
				"group by p.name " +
				"having sum(c.duration) > 1000",
				Object[].class)
			.getResultList();
			//end::hql-group-by-having-example[]
			assertEquals(0, personTotalCallDurations.size());
		});
	}

	@Test
	public void test_hql_order_by_example_1() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-order-by-example[]
			List<Person> persons = entityManager.createQuery(
				"select p " +
				"from Person p " +
				"order by p.name",
				Person.class)
			.getResultList();
			//end::hql-order-by-example[]
			assertEquals(3, persons.size());
		});
	}

	@Test
	public void test_hql_order_by_example_2() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-order-by-example[]

			List<Object[]> personTotalCallDurations = entityManager.createQuery(
				"select p.name, sum(c.duration) as total " +
				"from Call c " +
				"join c.phone ph " +
				"join ph.person p " +
				"group by p.name " +
				"order by total",
				Object[].class)
			.getResultList();
			//end::hql-order-by-example[]
			assertEquals(1, personTotalCallDurations.size());
		});
	}

	@Test
	public void test_hql_limit_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-limit-example[]
			List<Call> calls1 = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"join c.phone p " +
				"order by p.number " +
				"limit 50",
				Call.class)
			.getResultList();

			// same thing
			List<Call> calls2 = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"join c.phone p " +
				"order by p.number " +
				"fetch first 50 rows only",
				Call.class)
			.getResultList();
			//end::hql-limit-example[]
		});
	}

	@Test
	public void test_hql_bad_limit_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-bad-limit-example[]
			// don't do this! join fetch should not be used with limit
			List<Phone> wrongCalls = entityManager.createQuery(
				"select p " +
				"from Phone p " +
				"join fetch p.calls " +
				"order by p " +
				"limit 50",
				Phone.class)
			.getResultList();
			//end::hql-bad-limit-example[]
		});
	}

	@Test
	public void test_hql_bad_fetch_first_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			List<Phone> wrongCalls = entityManager.createQuery(
							"select p " +
									"from Phone p " +
									"join fetch p.calls " +
									"order by p " +
									"fetch first 50 percent rows only",
							Phone.class)
					.getResultList();
		});
	}

	@Test
	public void test_hql_read_only_entities_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-read-only-entities-example[]
			List<Call> calls = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"join c.phone p " +
				"where p.number = :phoneNumber ",
				Call.class)
			.setParameter("phoneNumber", "123-456-7890")
			.setHint("org.hibernate.readOnly", true)
			.getResultList();

			calls.forEach(c -> c.setDuration(0));
			//end::hql-read-only-entities-example[]
		});
	}

	@Test
	public void test_hql_read_only_entities_native_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-read-only-entities-native-example[]
			List<Call> calls = entityManager.createQuery(
				"select c " +
				"from Call c " +
				"join c.phone p " +
				"where p.number = :phoneNumber ",
				Call.class)
			.setParameter("phoneNumber", "123-456-7890")
			.unwrap( org.hibernate.query.Query.class)
			.setReadOnly(true)
			.getResultList();
			//end::hql-read-only-entities-native-example[]
		});
	}

	@Test
	public void test_hql_derived_root_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-derived-root-example[]
			List<Tuple> calls = entityManager.createQuery(
				"select d.owner, d.payed " +
				"from (" +
				"  select p.person as owner, c.payment is not null as payed " +
				"  from Call c " +
				"  join c.phone p " +
				"  where p.number = :phoneNumber) d",
				Tuple.class)
			.setParameter("phoneNumber", "123-456-7890")
			.getResultList();
			//end::hql-derived-root-example[]
		});
	}

	@Test
	public void test_hql_cte_materialized_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-cte-materialized-example[]
			List<Tuple> calls = entityManager.createQuery(
							"with data as materialized(" +
									"  select p.person as owner, c.payment is not null as payed " +
									"  from Call c " +
									"  join c.phone p " +
									"  where p.number = :phoneNumber" +
									")" +
									"select d.owner, d.payed " +
									"from data d",
							Tuple.class)
					.setParameter("phoneNumber", "123-456-7890")
					.getResultList();
			//end::hql-cte-materialized-example[]
		});
	}

	@Test
	@RequiresDialectFeature( DialectChecks.SupportsRecursiveCtes.class )
	public void test_hql_cte_recursive_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-cte-recursive-example[]
			List<Tuple> calls = entityManager.createQuery(
							"with paymentConnectedPersons as(" +
									"  select a.owner owner " +
									"  from Account a where a.id = :startId " +
									"  union all" +
									"  select a2.owner owner " +
									"  from paymentConnectedPersons d " +
									"  join Account a on a.owner = d.owner " +
									"  join a.payments p " +
									"  join Account a2 on a2.owner = p.person" +
									")" +
									"select d.owner " +
									"from paymentConnectedPersons d",
							Tuple.class)
					.setParameter("startId", 123L)
					.getResultList();
			//end::hql-cte-recursive-example[]
		});
	}

	@Test
	@RequiresDialectFeature( DialectChecks.SupportsRecursiveCtes.class )
	public void test_hql_cte_recursive_search_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-cte-recursive-search-example[]
			List<Tuple> calls = entityManager.createQuery(
							"with paymentConnectedPersons as(" +
									"  select a.owner owner " +
									"  from Account a where a.id = :startId " +
									"  union all" +
									"  select a2.owner owner " +
									"  from paymentConnectedPersons d " +
									"  join Account a on a.owner = d.owner " +
									"  join a.payments p " +
									"  join Account a2 on a2.owner = p.person" +
									") search breadth first by owner set orderAttr " +
									"select d.owner " +
									"from paymentConnectedPersons d",
							Tuple.class)
					.setParameter("startId", 123L)
					.getResultList();
			//end::hql-cte-recursive-search-example[]
		});
	}

	@Test
	@RequiresDialectFeature( DialectChecks.SupportsRecursiveCtes.class )
	public void test_hql_cte_recursive_cycle_example() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-cte-recursive-cycle-example[]
			List<Tuple> calls = entityManager.createQuery(
							"with paymentConnectedPersons as(" +
									"  select a.owner owner " +
									"  from Account a where a.id = :startId " +
									"  union all" +
									"  select a2.owner owner " +
									"  from paymentConnectedPersons d " +
									"  join Account a on a.owner = d.owner " +
									"  join a.payments p " +
									"  join Account a2 on a2.owner = p.person" +
									") cycle owner set cycleMark " +
									"select d.owner, d.cycleMark " +
									"from paymentConnectedPersons d",
							Tuple.class)
					.setParameter("startId", 123L)
					.getResultList();
			//end::hql-cte-recursive-cycle-example[]
		});
	}

	@Test
	@RequiresDialectFeature({
			DialectChecks.SupportsSubqueryInOnClause.class,
			DialectChecks.SupportsOrderByInCorrelatedSubquery.class
	})
	@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 11, reason = "The lateral emulation for Oracle 11 would be very complex because nested correlation is unsupported")
	public void test_hql_derived_join_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::hql-derived-join-example[]
			List<Tuple> calls1 = entityManager.createQuery(
					"from Phone p " +
					"left join (" +
					"  select c.duration as duration, c.phone.id as cid" +
					"  from Call c" +
					"  order by c.duration desc" +
					"  limit 1" +
					"  ) as longest on cid = p.id " +
					"where p.number = :phoneNumber " +
					"select longest.duration",
					Tuple.class)
			.setParameter("phoneNumber", "123-456-7890")
			.getResultList();

			//same, but using 'join lateral' instead of 'on'
			List<Tuple> calls2 = entityManager.createQuery(
				"from Phone p " +
				"left join lateral (" +
				"  select c.duration as duration" +
				"  from p.calls c" +
				"  order by c.duration desc" +
				"  limit 1" +
				"  ) as longest " +
				"where p.number = :phoneNumber " +
				"select longest.duration",
				Tuple.class)
			.setParameter("phoneNumber", "123-456-7890")
			.getResultList();
			//end::hql-derived-join-example[]
		});
	}
}
