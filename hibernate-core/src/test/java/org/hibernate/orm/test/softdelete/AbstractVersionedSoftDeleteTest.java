package org.hibernate.orm.test.softdelete;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The soft delete of an entity with a version checks the version of the
 * instance which is removed.
 */
@JiraKey("HHH-21259")
public abstract class AbstractVersionedSoftDeleteTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Item( 1 ) );
			session.persist( new SavingsAccount( 1 ) );
			session.persist( new Vehicle( 1 ) );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void removeLoadedInstance(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.remove( session.find( Item.class, 1 ) ) );
		assertSoftDeleted( scope, Item.class, "versioned_soft_item" );
	}

	@Test
	@JiraKey("HHH-21264")
	void removeUnloadedInstance(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.remove( session.getReference( Item.class, 1 ) ) );
		assertSoftDeleted( scope, Item.class, "versioned_soft_item" );
	}

	@Test
	@SkipForDialect(dialectClass = SpannerDialect.class, reason = "Spanner requires a WHERE clause in the setup UPDATE")
	void removeStaleInstance(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.beginTransaction();
			try {
				final Item item = session.find( Item.class, 1 );
				session.createNativeMutationQuery( "update versioned_soft_item set version = version + 1" )
						.executeUpdate();
				session.remove( item );
				assertThrows( OptimisticLockException.class, session::flush );
			}
			finally {
				session.getTransaction().rollback();
			}
		} );
		scope.inTransaction( session -> assertNotNull( session.find( Item.class, 1 ) ) );
	}

	@Test
	void removeJoinedSubclassInstance(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.remove( session.find( SavingsAccount.class, 1 ) ) );
		assertSoftDeleted( scope, Account.class, "versioned_soft_account" );
	}

	@Test
	void removeTablePerClassRootInstance(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.remove( session.find( Vehicle.class, 1 ) ) );
		assertSoftDeleted( scope, Vehicle.class, "versioned_soft_vehicle" );
	}

	static void assertSoftDeleted(SessionFactoryScope scope, Class<?> entityClass, String table) {
		scope.inTransaction( session -> {
			assertNull( session.find( entityClass, 1 ) );
			assertEquals( 1L, session.createNativeQuery( "select count(*) from " + table, Long.class )
					.getSingleResult() );
		} );
	}

	@Entity(name = "Item")
	@Table(name = "versioned_soft_item")
	@SoftDelete
	public static class Item {
		@Id
		Integer id;
		@Version
		Integer version;

		Item() {
		}

		Item(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Account")
	@Table(name = "versioned_soft_account")
	@Inheritance(strategy = InheritanceType.JOINED)
	@SoftDelete
	public static class Account {
		@Id
		Integer id;
		@Version
		Integer version;
	}

	@Entity(name = "SavingsAccount")
	@Table(name = "versioned_soft_savings")
	public static class SavingsAccount extends Account {
		String name;

		SavingsAccount() {
		}

		SavingsAccount(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Vehicle")
	@Table(name = "versioned_soft_vehicle")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@SoftDelete
	public static class Vehicle {
		@Id
		Integer id;
		@Version
		Integer version;

		Vehicle() {
		}

		Vehicle(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Car")
	@Table(name = "versioned_soft_car")
	public static class Car extends Vehicle {
	}
}
