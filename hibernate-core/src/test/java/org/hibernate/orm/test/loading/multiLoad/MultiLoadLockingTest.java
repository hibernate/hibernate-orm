/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading.multiLoad;

import jakarta.persistence.Basic;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.LockMode;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.sql.ast.PostgreSQLSqlAstTranslator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(
		annotatedClasses = {
			MultiLoadLockingTest.Customer.class,
			MultiLoadLockingTest.EntityWithAggregateId.class,
			MultiLoadLockingTest.User.class
		}
	)
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true")
		}
)
@JiraKey(value = "HHH-18992")
public class MultiLoadLockingTest {

	private SQLStatementInspector sqlStatementInspector;

	private final List<Customer> customerList = List.of(
			new Customer(1L, "Customer A"),
			new Customer(2L, "Customer B"),
			new Customer(3L, "Customer C"),
			new Customer(4L, "Customer D"),
			new Customer(5L, "Customer E")
		);

	private final List<Long> customerIdsAsLongs = customerList
		.stream()
		.map( Customer::getId )
		.toList();

	private final List<Object> customerIdsAsObjects = customerList
			.stream()
			.map( (Function<Customer, Object>) Customer::getId )
			.toList();

	private final List<Object> customerNaturalIdsAsObjects = customerList
			.stream()
			.map( (Function<Customer, Object>) Customer::getName )
			.toList();

	private final List<EntityWithAggregateId> entityWithAggregateIdList = List.of(
			new EntityWithAggregateId( new EntityWithAggregateId.Key( "1", "1" ), "Entity A" ),
			new EntityWithAggregateId( new EntityWithAggregateId.Key( "2", "2" ), "Entity B" ),
			new EntityWithAggregateId( new EntityWithAggregateId.Key( "3", "3" ), "Entity C" ),
			new EntityWithAggregateId( new EntityWithAggregateId.Key( "4", "4" ), "Entity D" ),
			new EntityWithAggregateId( new EntityWithAggregateId.Key( "5", "5" ), "Entity E" )
	);

	private final List<EntityWithAggregateId.Key> entityWithAggregateIdKeys = entityWithAggregateIdList
		.stream()
		.map(EntityWithAggregateId::getKey)
		.toList();

	private final List<Object> entityWithAggregateIdKeysAsObjects = entityWithAggregateIdList
			.stream()
			.map( (Function<EntityWithAggregateId, Object>) EntityWithAggregateId::getKey )
			.toList();

	private final List<Object> entityWithAggregateIdNaturalIdsAsObjects = entityWithAggregateIdList
			.stream()
			.map( (Function<EntityWithAggregateId, Object>) EntityWithAggregateId::getData )
			.toList();

	public final List<User> userList = List.of(
			new User(1, "User 1"),
			new User(2, "User 2"),
			new User(3, "User 3"),
			new User(4, "User 4"),
			new User(5, "User 5")
	);

	private final List<Integer> userIds = userList
		.stream()
		.map(User::getId)
		.toList();

	private final List<Object> userIdsAsObjects = userList
			.stream()
			.map( (Function<User, Object>) User::getId )
			.toList();

	private final List<Object> userNaturalIdsAsObjects = userList
			.stream()
			.map( (Function<User, Object>) User::getName )
			.toList();


	@BeforeEach
	public void prepareTestDataAndClearL2C(SessionFactoryScope scope) {
		sqlStatementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction(session -> {
			customerList.forEach( session::persist );
			entityWithAggregateIdList.forEach( session::persist );
			userList.forEach( session::persist );
		});
		scope.getSessionFactory().getCache().evictAll();
		sqlStatementInspector.clear();
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
		scope.getSessionFactory().getCache().evictAll();
	}


	// (1) simple Id entity w/ pessimistic read lock
	@Test
	void testMultiLoadSimpleIdEntityPessimisticReadLock(SessionFactoryScope scope) {
		final String lockString = scope.getSessionFactory()
				.getJdbcServices()
				.getDialect()
				.getForUpdateString( LockMode.PESSIMISTIC_READ, -1 );

		// test byMultipleIds
		scope.inTransaction( session -> {
			List<Customer> customersLoaded = session.byMultipleIds(Customer.class)
					.with( LockMode.PESSIMISTIC_READ )
					.multiLoad(customerIdsAsLongs);
			assertNotNull(customersLoaded);
			assertEquals(customerList.size(), customersLoaded.size());
			customersLoaded.forEach(customer -> assertEquals(LockMode.PESSIMISTIC_READ, session.getCurrentLockMode(customer)) );
			checkStatement( 1, lockString );
		} );
		// test findMultiple
		scope.inTransaction( session -> {
			List<Customer> customersLoaded = session.findMultiple(Customer.class, customerIdsAsObjects, LockMode.PESSIMISTIC_READ);
			assertNotNull(customersLoaded);
			assertEquals(customerList.size(), customersLoaded.size());
			customersLoaded.forEach(customer -> assertEquals(LockMode.PESSIMISTIC_READ, session.getCurrentLockMode(customer)) );
			checkStatement( 1, lockString );
		} );
		// test byMultipleNaturalId
		scope.inTransaction( session -> {
			List<Customer> customersLoaded = session.byMultipleNaturalId(Customer.class)
					.with( LockMode.PESSIMISTIC_READ )
					.multiLoad( customerNaturalIdsAsObjects );
			assertNotNull(customersLoaded);
			assertEquals(customerList.size(), customersLoaded.size());
			customersLoaded.forEach(customer -> assertEquals(LockMode.PESSIMISTIC_READ, session.getCurrentLockMode(customer)) );
			checkStatement( 1, lockString );
		} );
	}

	// (2) composite Id entity w/ pessimistic read lock (one of the entities already in L1C)
	@Test
	void testMultiLoadCompositeIdEntityPessimisticReadLockAlreadyInSession(SessionFactoryScope scope) {
		final String lockString = scope.getSessionFactory()
				.getJdbcServices()
				.getDialect()
				.getForUpdateString( LockMode.PESSIMISTIC_READ, -1 );

		scope.inTransaction( session -> {
			EntityWithAggregateId entityInL1C = session
					.find(EntityWithAggregateId.class, entityWithAggregateIdList.get(0).getKey());
			assertNotNull(entityInL1C);
			sqlStatementInspector.clear();

			// test byMultipleIds
			List<EntityWithAggregateId> entitiesLoaded = session.byMultipleIds(EntityWithAggregateId.class)
					.with( LockMode.PESSIMISTIC_READ )
					.multiLoad(entityWithAggregateIdKeys);
			assertNotNull(entitiesLoaded);
			assertEquals(entityWithAggregateIdList.size(), entitiesLoaded.size());
			entitiesLoaded.forEach(entity -> assertEquals(LockMode.PESSIMISTIC_READ, session.getCurrentLockMode(entity)) );
			checkStatement( 1, lockString );
		} );
		// test findMultiple
		scope.inTransaction( session -> {
			EntityWithAggregateId entityInL1C = session
					.find(EntityWithAggregateId.class, entityWithAggregateIdList.get(0).getKey());
			assertNotNull(entityInL1C);
			sqlStatementInspector.clear();

			List<EntityWithAggregateId> entitiesLoaded = session.findMultiple(EntityWithAggregateId.class,
							entityWithAggregateIdKeysAsObjects, LockMode.PESSIMISTIC_READ );
			assertNotNull(entitiesLoaded);
			assertEquals(entityWithAggregateIdList.size(), entitiesLoaded.size());
			entitiesLoaded.forEach(entity -> assertEquals(LockMode.PESSIMISTIC_READ, session.getCurrentLockMode(entity)) );
			checkStatement( 1, lockString );
		} );
		// test byMultipleNaturalId
		scope.inTransaction( session -> {
			EntityWithAggregateId entityInL1C = session
					.find(EntityWithAggregateId.class, entityWithAggregateIdList.get(0).getKey());
			assertNotNull(entityInL1C);
			sqlStatementInspector.clear();

			List<EntityWithAggregateId> entitiesLoaded = session
					.byMultipleNaturalId( EntityWithAggregateId.class )
					.with( LockMode.PESSIMISTIC_READ )
					.multiLoad( entityWithAggregateIdNaturalIdsAsObjects );
			assertNotNull(entitiesLoaded);
			assertEquals(entityWithAggregateIdList.size(), entitiesLoaded.size());
			entitiesLoaded.forEach(entity -> assertEquals(LockMode.PESSIMISTIC_READ, session.getCurrentLockMode(entity)) );
			// HHH-19248: multi natural-id loading checks the session (and upgrades locks) so 2 statements are expected
			checkStatement( 2, lockString );
		} );
	}

	// (3) simple Id entity w/ pessimistic write lock (one in L1C & some in L2C)
	@Test
	public void testMultiLoadSimpleIdEntityPessimisticWriteLockSomeInL1CAndSomeInL2C(SessionFactoryScope scope) {
		final Integer userInL2CId = userIds.get(0);
		final Integer userInL1CId = userIds.get(1);
		Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		String lockString;
		if ( PostgreSQLDialect.class.isAssignableFrom( dialect.getClass() ) ) {
			PgSqlAstTranslatorExt translator = new PgSqlAstTranslatorExt( scope.getSessionFactory(), null );
			lockString = translator.getForUpdate();
		}
		else  {
			lockString = dialect.getForUpdateString( LockMode.PESSIMISTIC_WRITE, -1 );
		}

		scope.inTransaction( session -> {
			User userInL2C = session.find(User.class, userInL2CId);
			assertNotNull(userInL2C);
		} );
		// test byMultipleIds
		scope.inTransaction( session -> {
			assertTrue(session.getFactory().getCache().containsEntity(User.class, userInL2CId));
			User userInL1C = session.find(User.class, userInL1CId);
			assertNotNull(userInL1C);
			sqlStatementInspector.clear();

			List<User> usersLoaded = session.byMultipleIds(User.class)
					.with( LockMode.PESSIMISTIC_WRITE )
					.multiLoad(userIds);
			assertNotNull(usersLoaded);
			assertEquals(userList.size(), usersLoaded.size());
			usersLoaded.forEach(user -> assertEquals(LockMode.PESSIMISTIC_WRITE, session.getCurrentLockMode(user)) );
			checkStatement( 1, lockString );
		} );
		// test findMultiple
		scope.inTransaction( session -> {
			User userInL1C = session.find(User.class, userInL1CId);
			assertNotNull(userInL1C);
			sqlStatementInspector.clear();

			List<User> usersLoaded = session.findMultiple(User.class, userIdsAsObjects, LockMode.PESSIMISTIC_WRITE);
			assertNotNull(usersLoaded);
			assertEquals(userList.size(), usersLoaded.size());
			usersLoaded.forEach(user -> assertEquals(LockMode.PESSIMISTIC_WRITE, session.getCurrentLockMode(user)) );
			checkStatement( 1, lockString );
		} );
		// test byMultipleNaturalId
		scope.inTransaction( session -> {
			User userInL1C = session.find(User.class, userInL1CId);
			assertNotNull(userInL1C);
			sqlStatementInspector.clear();

			List<User> usersLoaded = session.byMultipleNaturalId(User.class)
					.with( LockMode.PESSIMISTIC_WRITE )
					.multiLoad( userNaturalIdsAsObjects );
			assertNotNull(usersLoaded);
			assertEquals(userList.size(), usersLoaded.size());
			usersLoaded.forEach(user -> assertEquals(LockMode.PESSIMISTIC_WRITE, session.getCurrentLockMode(user)) );
			// HHH-19248: multi natural-id loading checks the session (and upgrades locks) so 2 statements are expected
			checkStatement( 2,lockString );
		} );
	}

	// (4) simple Id entity w/ optimistic read lock
	@Test
	void testMultiLoadSimpleIdEntityOptimisticReadLock(SessionFactoryScope scope) {
		// test byMultipleIds
		scope.inTransaction( session -> {
			List<User> usersLoaded = session.byMultipleIds(User.class)
					.with( LockMode.OPTIMISTIC )
					.multiLoad(userIds);
			assertNotNull(usersLoaded);
			assertEquals(userList.size(), usersLoaded.size());
			usersLoaded.forEach(user -> assertEquals(LockMode.OPTIMISTIC, session.getCurrentLockMode(user)) );
		} );
		// test findMultiple
		scope.inTransaction( session -> {
			List<User> usersLoaded = session.findMultiple(User.class, userIdsAsObjects, LockMode.OPTIMISTIC);
			assertNotNull(usersLoaded);
			assertEquals(userList.size(), usersLoaded.size());
			usersLoaded.forEach(user -> assertEquals(LockMode.OPTIMISTIC, session.getCurrentLockMode(user)) );
		} );
		// test byMultipleNaturalId
		scope.inTransaction( session -> {
			List<User> usersLoaded = session.byMultipleNaturalId(User.class)
					.with( LockMode.OPTIMISTIC )
					.multiLoad( userNaturalIdsAsObjects );
			assertNotNull(usersLoaded);
			assertEquals(userList.size(), usersLoaded.size());
			usersLoaded.forEach(user -> assertEquals(LockMode.OPTIMISTIC, session.getCurrentLockMode(user)) );
		} );
	}


	// (5) simple Id entity w/ optimistic force increment lock
	@Test
	void testMultiLoadSimpleIdEntityOptimisticForceIncrementLock(SessionFactoryScope scope) {
		// test byMultipleIds
		scope.inTransaction( session -> {
			List<User> usersLoaded = session.byMultipleIds(User.class)
					.with( LockMode.OPTIMISTIC_FORCE_INCREMENT )
					.multiLoad(userIds);
			assertNotNull(usersLoaded);
			assertEquals(userList.size(), usersLoaded.size());
			usersLoaded.forEach(user -> assertEquals(LockMode.OPTIMISTIC_FORCE_INCREMENT, session.getCurrentLockMode(user)) );
		} );
		// test findMultiple
		scope.inTransaction( session -> {
			List<User> usersLoaded = session.findMultiple(User.class, userIdsAsObjects, LockMode.OPTIMISTIC_FORCE_INCREMENT);
			assertNotNull(usersLoaded);
			assertEquals(userList.size(), usersLoaded.size());
			usersLoaded.forEach(user -> assertEquals(LockMode.OPTIMISTIC_FORCE_INCREMENT, session.getCurrentLockMode(user)) );
		} );
		// test byMultipleNaturalId
		scope.inTransaction( session -> {
			List<User> usersLoaded = session.byMultipleNaturalId(User.class)
					.with( LockMode.OPTIMISTIC_FORCE_INCREMENT )
					.multiLoad( userNaturalIdsAsObjects );
			assertNotNull(usersLoaded);
			assertEquals(userList.size(), usersLoaded.size());
			usersLoaded.forEach(user -> assertEquals(LockMode.OPTIMISTIC_FORCE_INCREMENT, session.getCurrentLockMode(user)) );
		} );
	}

	private void checkStatement(int stmtCount, String lockString) {
		assertEquals( stmtCount, sqlStatementInspector.getSqlQueries().size() );
		for ( String stmt : sqlStatementInspector.getSqlQueries() ) {
			assertThat( stmt, containsString( lockString ) );
		}
		sqlStatementInspector.clear();
	}

	// Ugly-ish hack to be able to access the PostgreSQLSqlAstTranslator.getForUpdate() method needed for testing the PostgreSQL dialects
	private static class PgSqlAstTranslatorExt extends PostgreSQLSqlAstTranslator {
		public PgSqlAstTranslatorExt(SessionFactoryImplementor sessionFactory, Statement statement) {
			super( sessionFactory, statement );
		}

		@Override
		protected String getForUpdate() {
			return super.getForUpdate();
		}
	}

	@Entity(name = "Customer")
	public static class Customer {
		@Id
		private Long id;
		@Basic(optional = false)
		@NaturalId
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

	@Entity(name = "EntityWithAggregateId")
	public static class EntityWithAggregateId {
		@EmbeddedId
		private EntityWithAggregateId.Key key;
		@NaturalId
		private String data;

		public EntityWithAggregateId() {
		}

		public EntityWithAggregateId(EntityWithAggregateId.Key key, String data) {
			this.key = key;
			this.data = data;
		}

		public EntityWithAggregateId.Key getKey() {
			return key;
		}

		public void setKey(EntityWithAggregateId.Key key) {
			this.key = key;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}


		@Embeddable
		public static class Key implements Serializable {
			private String value1;
			private String value2;

			public Key() {
			}

			public Key(String value1, String value2) {
				this.value1 = value1;
				this.value2 = value2;
			}

			public String getValue1() {
				return value1;
			}

			public void setValue1(String value1) {
				this.value1 = value1;
			}

			public String getValue2() {
				return value2;
			}

			public void setValue2(String value2) {
				this.value2 = value2;
			}
		}
	}

	@Entity(name = "MyUser")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class User {
		@Id
		int id;

		@Version
		private int version;

		@NaturalId
		private String name;

		public User() {
		}

		public User(int id) {
			this.id = id;
		}

		public User(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getVersion() {
			return version;
		}

		public void setVersion(Integer version) {
			this.version = version;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
