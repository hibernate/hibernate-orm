/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue;

import jakarta.persistence.*;
import org.hibernate.action.queue.spi.QueueType;
import org.hibernate.annotations.NaturalId;
import org.hibernate.cfg.FlushSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that DELETE and INSERT operations to tables with unique constraints
 * are properly ordered (DELETE before INSERT) to avoid unique constraint violations.
 *
 * This test verifies Phase 1 implementation of unique constraint ordering.
 *
 * @author Steve Ebersole
 */
@ServiceRegistry(settings = {
		@Setting( name= FlushSettings.ORDER_BY_UNIQUE_KEY, value = "true")
})
@DomainModel(
		annotatedClasses = {
				UniqueConstraintOrderingTest.UserAccount.class,
				UniqueConstraintOrderingTest.Employee.class,
				UniqueConstraintOrderingTest.Department.class,
				UniqueConstraintOrderingTest.Product.class,
				UniqueConstraintOrderingTest.CollectionOwner.class,
				UniqueConstraintOrderingTest.EmbeddedUniqueOwner.class
		}
)
@SessionFactory
public class UniqueConstraintOrderingTest {

	@Entity(name = "UserAccount")
	@Table(name = "user_account")
	public static class UserAccount {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@Column(unique = true, nullable = false)
		private String email;

		private String name;

		public UserAccount() {}

		public UserAccount(String email, String name) {
			this.email = email;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Employee")
	@Table(name = "employee")
	public static class Employee {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		private String name;

		@OneToOne
		@JoinColumn(name = "dept_id", unique = true)
		private Department department;

		public Employee() {}

		public Employee(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Department getDepartment() {
			return department;
		}

		public void setDepartment(Department department) {
			this.department = department;
		}
	}

	@Entity(name = "Department")
	@Table(name = "department")
	public static class Department {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

		public Department() {}

		public Department(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "Product")
	@Table(name = "product")
	public static class Product {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		@NaturalId(mutable = true)  // Phase 3: Allow natural ID changes
		private String sku;

		private String name;

		public Product() {}

		public Product(String sku, String name) {
			this.sku = sku;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getSku() {
			return sku;
		}

		public void setSku(String sku) {
			this.sku = sku;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "CollectionOwner")
	@Table(name = "collection_owner")
	public static class CollectionOwner {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		@ElementCollection
		@CollectionTable(
				name = "collection_owner_values",
				joinColumns = @JoinColumn(name = "owner_id"),
				uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = { "owner_id", "value_text" })
		)
		@Column(name = "value_text")
		private java.util.List<String> values = new java.util.ArrayList<>();

		public Long getId() {
			return id;
		}

		public java.util.List<String> getValues() {
			return values;
		}
	}

	@Entity(name = "EmbeddedUniqueOwner")
	@Table(
			name = "embedded_unique_owner",
			uniqueConstraints = @UniqueConstraint(columnNames = { "code_part_one", "code_part_two" })
	)
	public static class EmbeddedUniqueOwner {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		@Embedded
		private EmbeddedCode code;

		public EmbeddedUniqueOwner() {
		}

		public EmbeddedUniqueOwner(String partOne, String partTwo) {
			this.code = new EmbeddedCode( partOne, partTwo );
		}

		public Long getId() {
			return id;
		}

		public EmbeddedCode getCode() {
			return code;
		}
	}

	@Embeddable
	public static class EmbeddedCode {
		@Column(name = "code_part_one")
		private String partOne;

		@Column(name = "code_part_two")
		private String partTwo;

		public EmbeddedCode() {
		}

		public EmbeddedCode(String partOne, String partTwo) {
			this.partOne = partOne;
			this.partTwo = partTwo;
		}
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			session.createMutationQuery("delete from CollectionOwner").executeUpdate();
			session.createMutationQuery("delete from EmbeddedUniqueOwner").executeUpdate();
			session.createMutationQuery("delete from Employee").executeUpdate();
			session.createMutationQuery("delete from Department").executeUpdate();
			session.createMutationQuery("delete from UserAccount").executeUpdate();
			session.createMutationQuery("delete from Product").executeUpdate();
		});
	}

	@Test
	public void testDeleteAndInsertSameEmbeddableUniqueSlot(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		Long ownerId = scope.fromTransaction(session -> {
			EmbeddedUniqueOwner owner = new EmbeddedUniqueOwner( "north", "one" );
			session.persist( owner );
			return owner.getId();
		});

		scope.inTransaction(session -> {
			EmbeddedUniqueOwner oldOwner = session.find( EmbeddedUniqueOwner.class, ownerId );
			session.remove( oldOwner );
			session.persist( new EmbeddedUniqueOwner( "north", "one" ) );
			session.flush();
		});

		scope.inTransaction(session -> {
			EmbeddedUniqueOwner owner = session.createQuery(
					"from EmbeddedUniqueOwner",
					EmbeddedUniqueOwner.class
			).getSingleResult();
			assertEquals( "north", owner.getCode().partOne );
			assertEquals( "one", owner.getCode().partTwo );
		});
	}

	@Test
	public void testDeleteAndInsertSameCollectionUniqueSlot(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		Long ownerId = scope.fromTransaction(session -> {
			CollectionOwner owner = new CollectionOwner();
			owner.getValues().add( "alpha" );
			session.persist( owner );
			return owner.getId();
		});

		scope.inTransaction(session -> {
			CollectionOwner owner = session.find( CollectionOwner.class, ownerId );
			owner.getValues().clear();
			owner.getValues().add( "alpha" );
			session.flush();
		});

		scope.inTransaction(session -> {
			CollectionOwner owner = session.find( CollectionOwner.class, ownerId );
			assertEquals( java.util.List.of( "alpha" ), owner.getValues() );
		});
	}

	@Test
	public void testDeleteAndInsertSameTableWithPrimaryKey(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		// This test verifies that DELETE → INSERT ordering works for tables with primary keys
		// (all tables have PKs, so this should always be ordered)

		Long userId = scope.fromTransaction(session -> {
			UserAccount user = new UserAccount("john@example.com", "John Doe");
			session.persist(user);
			session.flush();
			return user.getId();
		});

		// In same transaction: delete old user and insert new user with potentially conflicting email
		scope.inTransaction(session -> {
			// Delete existing user
			UserAccount oldUser = session.find(UserAccount.class, userId);
			session.remove(oldUser);

			// Insert new user (different entity, but same table)
			UserAccount newUser = new UserAccount("jane@example.com", "Jane Smith");
			session.persist(newUser);

			// Flush should order DELETE before INSERT to avoid PK conflicts
			session.flush();
		});

		// Verify only new user exists
		scope.inTransaction(session -> {
			long count = session.createQuery("select count(u) from UserAccount u", Long.class)
					.getSingleResult();
			assertEquals(1, count, "Should have exactly one user");

			UserAccount user = session.createQuery("from UserAccount", UserAccount.class)
					.getSingleResult();
			assertEquals("jane@example.com", user.getEmail());
		});
	}

	@Test
	public void testDeleteAndInsertSameTableMultipleEntities(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		// Insert multiple users
		scope.inTransaction(session -> {
			session.persist(new UserAccount("user1@example.com", "User 1"));
			session.persist(new UserAccount("user2@example.com", "User 2"));
			session.persist(new UserAccount("user3@example.com", "User 3"));
		});

		// Delete all and insert new ones in same transaction
		scope.inTransaction(session -> {
			// Delete all existing
			session.createMutationQuery("delete from UserAccount").executeUpdate();

			// Insert new ones
			session.persist(new UserAccount("new1@example.com", "New 1"));
			session.persist(new UserAccount("new2@example.com", "New 2"));

			// Should not cause unique constraint violations
		});

		// Verify
		scope.inTransaction(session -> {
			long count = session.createQuery("select count(u) from UserAccount u", Long.class)
					.getSingleResult();
			assertEquals(2, count, "Should have 2 users");
		});
	}

	@Test
	public void testOneToOneUniqueConstraintSwapping_FuturePhase(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		// This tests swapping one-to-one relationships (unique FK constraint)
		// Currently fails because it requires runtime value tracking and UPDATE cycle breaking

		Long[] ids = scope.fromTransaction(session -> {
			Department dept1 = new Department("Sales");
			Department dept2 = new Department("Engineering");
			session.persist(dept1);
			session.persist(dept2);

			Employee emp1 = new Employee("Alice");
			Employee emp2 = new Employee("Bob");
			emp1.setDepartment(dept1);
			emp2.setDepartment(dept2);
			session.persist(emp1);
			session.persist(emp2);

			session.flush();
			return new Long[] { emp1.getId(), emp2.getId(), dept1.getId(), dept2.getId() };
		});

		// Swap departments (this creates a temporary unique constraint conflict)
		scope.inTransaction(session -> {
			Employee emp1 = session.find(Employee.class, ids[0]);
			Employee emp2 = session.find(Employee.class, ids[1]);
			Department dept1 = session.find(Department.class, ids[2]);
			Department dept2 = session.find(Department.class, ids[3]);

			// Swap: emp1 gets dept2, emp2 gets dept1
			emp1.setDepartment(dept2);
			emp2.setDepartment(dept1);

			// This should handle the unique constraint on employee.dept_id properly
			// The graph builder should order the UPDATEs appropriately
		});

		// Verify swap occurred
		scope.inTransaction(session -> {
			Employee emp1 = session.find(Employee.class, ids[0]);
			Employee emp2 = session.find(Employee.class, ids[1]);

			assertEquals("Engineering", emp1.getDepartment().getName());
			assertEquals("Sales", emp2.getDepartment().getName());
		});
	}

	@Test
	public void testMixedDeleteInsertUpdateSameTable(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		// Complex scenario: DELETE + INSERT + UPDATE all in same flush

		Long[] userIds = scope.fromTransaction(session -> {
			UserAccount user1 = new UserAccount("user1@example.com", "User 1");
			UserAccount user2 = new UserAccount("user2@example.com", "User 2");
			UserAccount user3 = new UserAccount("user3@example.com", "User 3");
			session.persist(user1);
			session.persist(user2);
			session.persist(user3);
			session.flush();
			return new Long[] { user1.getId(), user2.getId(), user3.getId() };
		});

		scope.inTransaction(session -> {
			// Delete first user
			UserAccount user1 = session.find(UserAccount.class, userIds[0]);
			session.remove(user1);

			// Update second user
			UserAccount user2 = session.find(UserAccount.class, userIds[1]);
			user2.setName("Updated User 2");

			// Insert new user
			session.persist(new UserAccount("new@example.com", "New User"));

			// All operations should complete without constraint violations
		});

		scope.inTransaction(session -> {
			long count = session.createQuery("select count(u) from UserAccount u", Long.class)
					.getSingleResult();
			assertEquals(3, count, "Should have 3 users (deleted 1, kept 2, inserted 1)");
		});
	}

	@Test
	public void testPhase2ValueBasedOrdering(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		// Phase 2 test: DELETE and INSERT with different IDs should NOT be ordered
		// Only DELETE and INSERT with SAME unique constraint values are ordered

		// Create users with IDs 1, 2, 3
		Long[] userIds = scope.fromTransaction(session -> {
			UserAccount user1 = new UserAccount("user1@example.com", "User 1");
			UserAccount user2 = new UserAccount("user2@example.com", "User 2");
			UserAccount user3 = new UserAccount("user3@example.com", "User 3");
			session.persist(user1);
			session.persist(user2);
			session.persist(user3);
			session.flush();
			return new Long[] { user1.getId(), user2.getId(), user3.getId() };
		});

		// Delete user1 (ID=userIds[0]) and insert new user (different ID)
		// Phase 2: These have DIFFERENT primary key values, so no ordering needed
		// This should work without issues
		scope.inTransaction(session -> {
			// Delete user1
			UserAccount user1 = session.find(UserAccount.class, userIds[0]);
			session.remove(user1);

			// Insert new user with different email (different unique value)
			session.persist(new UserAccount("newuser@example.com", "New User"));

			// Phase 2 allows these to execute in any order because they don't
			// conflict on unique constraint values (different PKs, different emails)
		});

		// Verify results
		scope.inTransaction(session -> {
			long count = session.createQuery("select count(u) from UserAccount u", Long.class)
					.getSingleResult();
			assertEquals(3, count, "Should have 3 users");

			// Verify new user exists
			long newUserCount = session.createQuery(
					"select count(u) from UserAccount u where u.email = :email", Long.class)
					.setParameter("email", "newuser@example.com")
					.getSingleResult();
			assertEquals(1, newUserCount, "New user should exist");
		});
	}

	@Test
	public void testPhase4NaturalIdOrdering(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		// Phase 4 test: @NaturalId constraint should be tracked
		// DELETE and INSERT with same natural ID should be ordered

		// Create products with unique SKUs
		Long[] productIds = scope.fromTransaction(session -> {
			Product product1 = new Product("SKU-001", "Product 1");
			Product product2 = new Product("SKU-002", "Product 2");
			Product product3 = new Product("SKU-003", "Product 3");
			session.persist(product1);
			session.persist(product2);
			session.persist(product3);
			session.flush();
			return new Long[] { product1.getId(), product2.getId(), product3.getId() };
		});

		// Delete product with SKU-001 and insert new product with same SKU
		// Phase 4: Should detect natural ID conflict and order DELETE before INSERT
		scope.inTransaction(session -> {
			// Delete product1
			Product product1 = session.find(Product.class, productIds[0]);
			session.remove(product1);

			// Insert new product with SAME SKU
			session.persist(new Product("SKU-001", "New Product"));

			// Phase 4 should detect natural ID conflict and order these operations
		});

		// Verify results
		scope.inTransaction(session -> {
			long count = session.createQuery("select count(p) from Product p", Long.class)
					.getSingleResult();
			assertEquals(3, count, "Should have 3 products");

			// Verify new product with SKU-001 exists
			Product newProduct = session.createQuery(
					"from Product where sku = :sku", Product.class)
					.setParameter("sku", "SKU-001")
					.getSingleResult();
			assertEquals("New Product", newProduct.getName(), "Should be the new product");
		});
	}

	@Test
	public void testPhase4NaturalIdNonConflicting(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		// Phase 4 test: DELETE and INSERT with DIFFERENT natural IDs should NOT be ordered

		// Create products
		Long[] productIds = scope.fromTransaction(session -> {
			Product product1 = new Product("SKU-001", "Product 1");
			Product product2 = new Product("SKU-002", "Product 2");
			session.persist(product1);
			session.persist(product2);
			session.flush();
			return new Long[] { product1.getId(), product2.getId() };
		});

		// Delete product with SKU-001 and insert new product with DIFFERENT SKU
		// Phase 4: Different natural IDs = no conflict = no ordering needed
		scope.inTransaction(session -> {
			// Delete product1 (SKU-001)
			Product product1 = session.find(Product.class, productIds[0]);
			session.remove(product1);

			// Insert new product with DIFFERENT SKU
			session.persist(new Product("SKU-999", "New Product"));

			// Phase 4 allows these to execute in any order because natural IDs don't conflict
		});

		// Verify results
		scope.inTransaction(session -> {
			long count = session.createQuery("select count(p) from Product p", Long.class)
					.getSingleResult();
			assertEquals(2, count, "Should have 2 products");

			// Verify new product exists
			long newProductCount = session.createQuery(
					"select count(p) from Product p where p.sku = :sku", Long.class)
					.setParameter("sku", "SKU-999")
					.getSingleResult();
			assertEquals(1, newProductCount, "New product should exist");
		});
	}

	@Test
	public void testPhase4OneToOneUniqueFK(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		// Phase 4 test: One-to-one unique FK should be tracked
		// This tests UNIQUE_FOREIGN_KEY constraint type

		Long[] ids = scope.fromTransaction(session -> {
			Department dept1 = new Department("Sales");
			Department dept2 = new Department("Engineering");
			session.persist(dept1);
			session.persist(dept2);

			Employee emp1 = new Employee("Alice");
			emp1.setDepartment(dept1);
			session.persist(emp1);

			session.flush();
			return new Long[] { emp1.getId(), dept1.getId(), dept2.getId() };
		});

		// Delete employee and create new employee with same department
		// The unique FK constraint should be tracked
		scope.inTransaction(session -> {
			Employee emp1 = session.find(Employee.class, ids[0]);
			Department dept1 = session.find(Department.class, ids[1]);

			// Delete old employee
			session.remove(emp1);

			// Create new employee with SAME department (same unique FK value)
			Employee newEmp = new Employee("Bob");
			newEmp.setDepartment(dept1);
			session.persist(newEmp);

			// Phase 4: Should detect unique FK conflict and order DELETE before INSERT
		});

		// Verify results
		scope.inTransaction(session -> {
			long count = session.createQuery("select count(e) from Employee e", Long.class)
					.getSingleResult();
			assertEquals(1, count, "Should have 1 employee");

			Employee emp = session.createQuery("from Employee", Employee.class)
					.getSingleResult();
			assertEquals("Bob", emp.getName(), "Should be the new employee");
		});
	}

	@Test
	public void testPhase3UpdateChangingNaturalId(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		// Phase 3 test: UPDATE that changes natural ID value
		// Should detect that old SKU is released and new SKU is occupied

		// Create product with SKU-001
		Long productId = scope.fromTransaction(session -> {
			Product product = new Product("SKU-001", "Product 1");
			session.persist(product);
			session.flush();
			return product.getId();
		});

		// Update product to use SKU-002 (change natural ID)
		scope.inTransaction(session -> {
			Product product = session.find(Product.class, productId);
			product.setSku("SKU-002");  // Change natural ID value
			// Phase 3: Should detect unique constraint value change
		});

		// Verify: Product should have new SKU
		scope.inTransaction(session -> {
			Product product = session.find(Product.class, productId);
			assertEquals("SKU-002", product.getSku(), "SKU should be updated");
		});
	}

	@Test
	public void testPhase3UpdateConflictWithInsert(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		// Phase 3 test: UPDATE changes natural ID to value that INSERT will use
		// This creates a conflict that should be detected

		Long[] productIds = scope.fromTransaction(session -> {
			Product product1 = new Product("SKU-001", "Product 1");
			Product product2 = new Product("SKU-002", "Product 2");
			session.persist(product1);
			session.persist(product2);
			session.flush();
			return new Long[] { product1.getId(), product2.getId() };
		});

		// UPDATE product1 to SKU-003, DELETE product2, INSERT new product with SKU-002
		scope.inTransaction(session -> {
			// Update product1 natural ID (releases SKU-001, occupies SKU-003)
			Product product1 = session.find(Product.class, productIds[0]);
			product1.setSku("SKU-003");

			// Delete product2 (releases SKU-002)
			Product product2 = session.find(Product.class, productIds[1]);
			session.remove(product2);

			// Insert new product with SKU-002 (occupies SKU-002)
			session.persist(new Product("SKU-002", "New Product"));

			// Phase 3: DELETE must happen before INSERT to avoid SKU-002 conflict
			// UPDATE can happen anytime (no conflict)
		});

		// Verify results
		scope.inTransaction(session -> {
			long count = session.createQuery("select count(p) from Product p", Long.class)
					.getSingleResult();
			assertEquals(2, count, "Should have 2 products");

			Product product1 = session.find(Product.class, productIds[0]);
			assertEquals("SKU-003", product1.getSku(), "Product 1 should have new SKU");

			Product newProduct = session.createQuery(
					"from Product where sku = :sku", Product.class)
					.setParameter("sku", "SKU-002")
					.getSingleResult();
			assertEquals("New Product", newProduct.getName());
		});
	}

	@Test
	public void testPhase3OneToOneSwap(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		// Phase 3 test: Swap one-to-one relationships (creates cycle)
		// Employee emp1: dept_id = 1
		// Employee emp2: dept_id = 2
		// Swap: emp1 -> dept2, emp2 -> dept1
		// This creates a cycle that should be handled by IGNORE_UNIQUE_EDGES_IN_CYCLES

		Long[] ids = scope.fromTransaction(session -> {
			Department dept1 = new Department("Sales");
			Department dept2 = new Department("Engineering");
			session.persist(dept1);
			session.persist(dept2);

			Employee emp1 = new Employee("Alice");
			Employee emp2 = new Employee("Bob");
			emp1.setDepartment(dept1);
			emp2.setDepartment(dept2);
			session.persist(emp1);
			session.persist(emp2);

			session.flush();
			return new Long[] { emp1.getId(), emp2.getId(), dept1.getId(), dept2.getId() };
		});

		// Swap departments (creates unique constraint cycle)
		scope.inTransaction(session -> {
			Employee emp1 = session.find(Employee.class, ids[0]);
			Employee emp2 = session.find(Employee.class, ids[1]);
			Department dept1 = session.find(Department.class, ids[2]);
			Department dept2 = session.find(Department.class, ids[3]);

			// Swap: emp1 gets dept2, emp2 gets dept1
			// This creates a cycle:
			// - UPDATE emp1.dept_id = 2 conflicts with emp2.dept_id = 2 (before swap)
			// - UPDATE emp2.dept_id = 1 conflicts with emp1.dept_id = 1 (before swap)
			emp1.setDepartment(dept2);
			emp2.setDepartment(dept1);

			// Phase 3: IGNORE_UNIQUE_EDGES_IN_CYCLES should allow this to work
			// The updates don't actually conflict at execution time
		});

		// Verify swap occurred
		scope.inTransaction(session -> {
			Employee emp1 = session.find(Employee.class, ids[0]);
			Employee emp2 = session.find(Employee.class, ids[1]);

			assertEquals("Engineering", emp1.getDepartment().getName(), "emp1 should have dept2");
			assertEquals("Sales", emp2.getDepartment().getName(), "emp2 should have dept1");
		});
	}

	@Test
	public void testUpdateReleasesUniqueSlotBeforeInsertOccupiesIt(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		Long productId = scope.fromTransaction(session -> {
			Product product = new Product("SKU-001", "Product 1");
			session.persist(product);
			session.flush();
			return product.getId();
		});

		scope.inTransaction(session -> {
			Product product = session.find(Product.class, productId);
			product.setSku("SKU-002");

			session.persist(new Product("SKU-001", "Replacement Product"));
		});

		scope.inTransaction(session -> {
			Product updatedProduct = session.find(Product.class, productId);
			assertEquals("SKU-002", updatedProduct.getSku(), "Original product should have released SKU-001");

			Product replacement = session.createQuery(
					"from Product where sku = :sku",
					Product.class
			).setParameter("sku", "SKU-001").getSingleResult();
			assertEquals("Replacement Product", replacement.getName());
		});
	}
}
