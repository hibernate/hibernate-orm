/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading.multiLoad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.cache.Company;
import org.hibernate.orm.test.cache.User;
import org.hibernate.testing.orm.domain.gambit.EntityWithAggregateId;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;


@DomainModel(
		annotatedClasses = {
			MultiLoadLockingTest.Customer.class,
			EntityWithAggregateId.class,
			User.class,
			Company.class
		}
	)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true")
		}
)
@JiraKey(value = "HHH-18992")
public class MultiLoadLockingTest {

	private List<Customer> customerList = List.of(
			new Customer(1L, "Customer A"),
			new Customer(2L, "Customer B"),
			new Customer(3L, "Customer C"),
			new Customer(4L, "Customer D"),
			new Customer(5L, "Customer E")
		);

	private List<Long> customerIds = customerList
		.stream()
		.map(Customer::getId)
		.collect(Collectors.toList());

	private List<EntityWithAggregateId> entityWithAggregateIdList = List.of(
			new EntityWithAggregateId( new EntityWithAggregateId.Key( "1", "1" ), "Entity A" ),
			new EntityWithAggregateId( new EntityWithAggregateId.Key( "2", "2" ), "Entity B" ),
			new EntityWithAggregateId( new EntityWithAggregateId.Key( "3", "3" ), "Entity C" ),
			new EntityWithAggregateId( new EntityWithAggregateId.Key( "4", "4" ), "Entity D" ),
			new EntityWithAggregateId( new EntityWithAggregateId.Key( "5", "5" ), "Entity E" )
	);

	private List<EntityWithAggregateId.Key> entityWithAggregateIdKeys = entityWithAggregateIdList
		.stream()
		.map(EntityWithAggregateId::getKey)
		.collect(Collectors.toList());

	public List<User> userList = List.of(
			new User(1, null),
			new User(2, null),
			new User(3, null),
			new User(4, null),
			new User(5, null)
	);

	private List<Integer> userIds = userList
		.stream()
		.map(User::getId)
		.collect(Collectors.toList());


	@BeforeEach
	public void prepareTestDataAndClearL2C(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			customerList.forEach( session::persist );
			entityWithAggregateIdList.forEach( session::persist );
			userList.forEach( session::persist );
		});
		scope.getSessionFactory().getCache().evictAll();
	}

	// (1) simple Id entity w/ pessimistic read lock

	@Test
	void testMultiLoadSimpleIdEntityPessimisticReadLock(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Customer> customersLoaded = session.byMultipleIds(Customer.class)
				.with(new LockOptions(LockMode.PESSIMISTIC_READ))
				.multiLoad(customerIds);
			assertNotNull(customersLoaded);
			assertEquals(customerList.size(), customersLoaded.size());
			customersLoaded.forEach(customer -> {
				assertEquals(LockMode.PESSIMISTIC_READ, session.getCurrentLockMode(customer));
			});
		} );
	}

	// (2) composite Id entity w/ pessimistic read lock (one of the entities already in L1C)

	@Test
	void testMultiLoadCompositeIdEntityPessimisticReadLockAlreadyInSession(
		SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityWithAggregateId entityInL1C = session
				.find(EntityWithAggregateId.class, entityWithAggregateIdList.get(0).getKey());
			assertNotNull(entityInL1C);
			List<EntityWithAggregateId> entitiesLoaded = session.byMultipleIds(EntityWithAggregateId.class)
				.with(new LockOptions(LockMode.PESSIMISTIC_READ))
				.enableSessionCheck(true)
				.multiLoad(entityWithAggregateIdKeys);
			assertNotNull(entitiesLoaded);
			assertEquals(entityWithAggregateIdList.size(), entitiesLoaded.size());
			entitiesLoaded.forEach(entity -> {
				assertEquals(LockMode.PESSIMISTIC_READ, session.getCurrentLockMode(entity));
			});
		} );
	}

	// (3) simple Id entity w/ pessimistic write lock (one in L1C & some in L2C)

	@Test
	public void testMultiLoadSimpleIdEntityPessimisticWriteLockSomeInL1CAndSomeInL2C(
		SessionFactoryScope scope) {
		Integer userInL2CId = userIds.get(0);
		Integer userInL1CId = userIds.get(1);
		scope.inTransaction( session -> {
			User userInL2C = session.find(User.class, userInL2CId);
			assertNotNull(userInL2C);
		} );
		scope.inTransaction( session -> {
			assertTrue(session.getFactory().getCache().containsEntity(User.class, userInL2CId));
			User userInL1C = session.find(User.class, userInL1CId);
			assertNotNull(userInL1C);
			List<User> usersLoaded = session.byMultipleIds(User.class)
				.with(new LockOptions(LockMode.PESSIMISTIC_WRITE))
				.multiLoad(userIds);
			assertNotNull(usersLoaded);
			assertEquals(userList.size(), usersLoaded.size());
			usersLoaded.forEach(user -> {
				assertEquals(LockMode.PESSIMISTIC_WRITE, session.getCurrentLockMode(user));
			});
		} );
	}



	// (4) simple Id entity w/ optimistic read lock

	@Test
	void testMultiLoadSimpleIdEntityOptimisticReadLock(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Customer> customersLoaded = session.byMultipleIds(Customer.class)
				.with(new LockOptions(LockMode.OPTIMISTIC))
				.multiLoad(customerIds);
			assertNotNull(customersLoaded);
			assertEquals(customerList.size(), customersLoaded.size());
			customersLoaded.forEach(customer -> {
				assertEquals(LockMode.OPTIMISTIC, session.getCurrentLockMode(customer));
			});
		} );
	}


	// (5) simple Id entity w/ optimistic force increment lock

	@Test
	void testMultiLoadSimpleIdEntityOptimisticForceIncrementLock(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Customer> customersLoaded = session.byMultipleIds(Customer.class)
				.with(new LockOptions(LockMode.OPTIMISTIC_FORCE_INCREMENT))
				.multiLoad(customerIds);
			assertNotNull(customersLoaded);
			assertEquals(customerList.size(), customersLoaded.size());
			customersLoaded.forEach(customer -> {
				assertEquals(LockMode.OPTIMISTIC_FORCE_INCREMENT, session.getCurrentLockMode(customer));
			});
		} );
	}



	@Entity
	public static class Customer {

		@Id
		private Long id;
		@Basic
		private String name;

		protected Customer() {
		}

		public Customer(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}
}
