/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import org.hibernate.testing.orm.domain.userguide.Account;
import org.hibernate.testing.orm.domain.userguide.AddressType;
import org.hibernate.testing.orm.domain.userguide.Call;
import org.hibernate.testing.orm.domain.userguide.CreditCardPayment;
import org.hibernate.testing.orm.domain.userguide.Person;
import org.hibernate.testing.orm.domain.userguide.Phone;
import org.hibernate.testing.orm.domain.userguide.PhoneType;
import org.hibernate.testing.orm.domain.userguide.WireTransferPayment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for all HQL tests involving Insert or Update.
 *
 * @author Loïc Lefèvre
 */
@SuppressWarnings({"unused", "JUnitMalformedDeclaration", "removal", "deprecation"})
@DomainModel(annotatedClasses = {
		Person.class,
		Phone.class,
		Call.class,
		Account.class,
		CreditCardPayment.class,
		WireTransferPayment.class
})
@SessionFactory
public class HQLInsertAndUpdateTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (entityManager) -> {
			Person person1 = new Person("John Doe");
			person1.setNickName("JD");
			person1.setAddress("Earth");
			person1.setCreatedOn( LocalDateTime.of(2000, 1, 1, 0, 0, 0)) ;
			person1.getAddresses().put( AddressType.HOME, "Home address");
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
			phone1.setType( PhoneType.MOBILE);
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
			creditCardPayment.setAmount( BigDecimal.ZERO);
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
		} );
	}

	@Test
	public void hql_insert_example(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			//tag::hql-insert-example[]
			entityManager.createQuery(
				"insert Person (id, name) " +
				"values (100L, 'Jane Doe')")
			.executeUpdate();
			//end::hql-insert-example[]
		});
	}

	@Test
	public void hql_multi_insert_example(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
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
	public void hql_insert_with_sequence_example(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			entityManager.createQuery(
				"insert Person (name) values ('Jane Doe2')" )
			.executeUpdate();
		});
	}

	@Test
	public void hql_update_example(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			//tag::hql-update-example[]
			entityManager.createQuery(
				"update Person set nickName = 'Nacho' " +
				"where name = 'Ignacio'")
			.executeUpdate();
			//end::hql-update-example[]
		});
	}

	@Test
	public void test_hql_group_by_example_4(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {

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

}
