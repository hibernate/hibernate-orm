/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.orm.domain.userguide.Account;
import org.hibernate.testing.orm.domain.userguide.AddressType;
import org.hibernate.testing.orm.domain.userguide.Call;
import org.hibernate.testing.orm.domain.userguide.CreditCardPayment;
import org.hibernate.testing.orm.domain.userguide.Partner;
import org.hibernate.testing.orm.domain.userguide.Partner_;
import org.hibernate.testing.orm.domain.userguide.Person;
import org.hibernate.testing.orm.domain.userguide.Person_;
import org.hibernate.testing.orm.domain.userguide.Phone;
import org.hibernate.testing.orm.domain.userguide.PhoneType;
import org.hibernate.testing.orm.domain.userguide.Phone_;
import org.hibernate.testing.orm.domain.userguide.WireTransferPayment;

import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class CriteriaTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
			Partner.class,
			Phone.class,
			Call.class,
			CreditCardPayment.class,
			WireTransferPayment.class,
			Account.class,
			Event.class
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

			Phone phone2 = new Phone("098_765-4321");
			phone2.setId(2L);
			phone2.setType(PhoneType.LAND_LINE);

			Phone phone3 = new Phone("098-765-4320");
			phone3.setId(3L);
			phone3.setType(PhoneType.LAND_LINE);

			person2.addPhone(phone2);
			person2.addPhone(phone3);

			CreditCardPayment creditCardPayment = new CreditCardPayment();
			creditCardPayment.setCompleted(true);
			creditCardPayment.setAmount(BigDecimal.ZERO);
			creditCardPayment.setPerson(person1);

			WireTransferPayment wireTransferPayment = new WireTransferPayment();
			wireTransferPayment.setCompleted(true);
			wireTransferPayment.setAmount(BigDecimal.valueOf(100));
			wireTransferPayment.setPerson(person2);

			entityManager.persist(creditCardPayment);
			entityManager.persist(wireTransferPayment);

			Partner partner = new Partner("John Doe");
			entityManager.persist(partner);
		});
	}

	@Test
	public void test_criteria_typedquery_entity_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::criteria-typedquery-entity-example[]
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<Person> criteria = builder.createQuery(Person.class);
			Root<Person> root = criteria.from(Person.class);
			criteria.select(root);
			criteria.where(builder.equal(root.get(Person_.name), "John Doe"));

			List<Person> persons = entityManager.createQuery(criteria).getResultList();
			//end::criteria-typedquery-entity-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_criteria_typedquery_expression_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::criteria-typedquery-expression-example[]
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<String> criteria = builder.createQuery(String.class);
			Root<Person> root = criteria.from(Person.class);
			criteria.select(root.get(Person_.nickName));
			criteria.where(builder.equal(root.get(Person_.name), "John Doe"));

			List<String> nickNames = entityManager.createQuery(criteria).getResultList();
			//end::criteria-typedquery-expression-example[]
			assertEquals(1, nickNames.size());
		});
	}

	@Test
	public void test_criteria_typedquery_multiselect_explicit_array_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::criteria-typedquery-multiselect-array-explicit-example[]
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<Object[]> criteria = builder.createQuery(Object[].class);
			Root<Person> root = criteria.from(Person.class);

			Path<Long> idPath = root.get(Person_.id);
			Path<String> nickNamePath = root.get(Person_.nickName);

			criteria.select(builder.array(idPath, nickNamePath));
			criteria.where(builder.equal(root.get(Person_.name), "John Doe"));

			List<Object[]> idAndNickNames = entityManager.createQuery(criteria).getResultList();
			//end::criteria-typedquery-multiselect-array-explicit-example[]
			assertEquals(1, idAndNickNames.size());
		});
	}

	@Test
	public void test_criteria_typedquery_multiselect_implicit_array_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::criteria-typedquery-multiselect-array-implicit-example[]
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<Object[]> criteria = builder.createQuery(Object[].class);
			Root<Person> root = criteria.from(Person.class);

			Path<Long> idPath = root.get(Person_.id);
			Path<String> nickNamePath = root.get(Person_.nickName);

			criteria.multiselect(idPath, nickNamePath);
			criteria.where(builder.equal(root.get(Person_.name), "John Doe"));

			List<Object[]> idAndNickNames = entityManager.createQuery(criteria).getResultList();
			//end::criteria-typedquery-multiselect-array-implicit-example[]
			assertEquals(1, idAndNickNames.size());
		});
	}

	@Test
	public void test_criteria_typedquery_wrapper_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::criteria-typedquery-wrapper-example[]

			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<PersonWrapper> criteria = builder.createQuery(PersonWrapper.class);
			Root<Person> root = criteria.from(Person.class);

			Path<Long> idPath = root.get(Person_.id);
			Path<String> nickNamePath = root.get(Person_.nickName);

			criteria.select(builder.construct(PersonWrapper.class, idPath, nickNamePath));
			criteria.where(builder.equal(root.get(Person_.name), "John Doe"));

			List<PersonWrapper> wrappers = entityManager.createQuery(criteria).getResultList();
			//end::criteria-typedquery-wrapper-example[]
			assertEquals(1, wrappers.size());
		});
	}

	@Test
	public void test_criteria_tuple_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::criteria-tuple-example[]
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<Tuple> criteria = builder.createQuery(Tuple.class);
			Root<Person> root = criteria.from(Person.class);

			Path<Long> idPath = root.get(Person_.id);
			Path<String> nickNamePath = root.get(Person_.nickName);

			criteria.multiselect(idPath, nickNamePath);
			criteria.where(builder.equal(root.get(Person_.name), "John Doe"));

			List<Tuple> tuples = entityManager.createQuery(criteria).getResultList();

			for (Tuple tuple : tuples) {
				Long id = tuple.get(idPath);
				String nickName = tuple.get(nickNamePath);
			}

			//or using indices
			for (Tuple tuple : tuples) {
				Long id = (Long) tuple.get(0);
				String nickName = (String) tuple.get(1);
			}
			//end::criteria-tuple-example[]
			assertEquals(1, tuples.size());
		});
	}

	@Test
	public void test_criteria_from_root_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::criteria-from-root-example[]
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<Person> criteria = builder.createQuery(Person.class);
			Root<Person> root = criteria.from(Person.class);
			//end::criteria-from-root-example[]
		});
	}

	@Test
	public void test_criteria_from_multiple_root_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			String address = "Earth";
			String prefix = "J%";
			//tag::criteria-from-multiple-root-example[]
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<Tuple> criteria = builder.createQuery(Tuple.class);

			Root<Person> personRoot = criteria.from(Person.class);
			Root<Partner> partnerRoot = criteria.from(Partner.class);
			criteria.multiselect(personRoot, partnerRoot);

			Predicate personRestriction = builder.and(
				builder.equal(personRoot.get(Person_.address), address),
				builder.isNotEmpty(personRoot.get(Person_.phones))
			);
			Predicate partnerRestriction = builder.and(
				builder.like(partnerRoot.get(Partner_.name), prefix),
				builder.equal(partnerRoot.get(Partner_.version), 0)
			);
			criteria.where(builder.and(personRestriction, partnerRestriction));

			List<Tuple> tuples = entityManager.createQuery(criteria).getResultList();
			//end::criteria-from-multiple-root-example[]
			assertEquals(2, tuples.size());
		});
	}

	@Test
	public void test_criteria_from_join_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::criteria-from-join-example[]
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<Phone> criteria = builder.createQuery(Phone.class);
			Root<Phone> root = criteria.from(Phone.class);

			// Phone.person is a @ManyToOne
			Join<Phone, Person> personJoin = root.join(Phone_.person);
			// Person.addresses is an @ElementCollection
			Join<Person, String> addressesJoin = personJoin.join(Person_.addresses);

			criteria.where(builder.isNotEmpty(root.get(Phone_.calls)));

			List<Phone> phones = entityManager.createQuery(criteria).getResultList();
			//end::criteria-from-join-example[]
			assertEquals(1, phones.size());
		});
	}

	@Test
	public void test_criteria_from_fetch_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::criteria-from-fetch-example[]
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<Phone> criteria = builder.createQuery(Phone.class);
			Root<Phone> root = criteria.from(Phone.class);

			// Phone.person is a @ManyToOne
			Fetch<Phone, Person> personFetch = root.fetch(Phone_.person);
			// Person.addresses is an @ElementCollection
			Fetch<Person, String> addressesJoin = personFetch.fetch(Person_.addresses);

			criteria.where(builder.isNotEmpty(root.get(Phone_.calls)));

			List<Phone> phones = entityManager.createQuery(criteria).getResultList();
			//end::criteria-from-fetch-example[]
			assertEquals(1, phones.size());
		});
	}

	@Test
	public void test_criteria_param_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::criteria-param-example[]
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<Person> criteria = builder.createQuery(Person.class);
			Root<Person> root = criteria.from(Person.class);

			ParameterExpression<String> nickNameParameter = builder.parameter(String.class);
			criteria.where(builder.equal(root.get(Person_.nickName), nickNameParameter));

			TypedQuery<Person> query = entityManager.createQuery(criteria);
			query.setParameter(nickNameParameter, "JD");
			List<Person> persons = query.getResultList();
			//end::criteria-param-example[]
			assertEquals(1, persons.size());
		});
	}

	@Test
	public void test_criteria_group_by_example() {

		doInJPA(this::entityManagerFactory, entityManager -> {
			//tag::criteria-group-by-example[]
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			CriteriaQuery<Tuple> criteria = builder.createQuery(Tuple.class);
			Root<Person> root = criteria.from(Person.class);

			criteria.groupBy(root.get("address"));
			criteria.multiselect(root.get("address"), builder.count(root));

			List<Tuple> tuples = entityManager.createQuery(criteria).getResultList();

			for (Tuple tuple : tuples) {
				String name = (String) tuple.get(0);
				Long count = (Long) tuple.get(1);
			}
			//end::criteria-group-by-example[]
			assertEquals(2, tuples.size());
		});
	}


	@Entity(name = "ApplicationEvent")
	public static class Event {

		@Id
		private Long id;

		private String name;
	}
}
