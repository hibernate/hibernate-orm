package org.hibernate.orm.test.softdelete;

import org.hibernate.annotations.PartitionKey;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The soft delete of an entity with a partition key is restricted by the
 * partition key of the instance which is removed.
 */
@JiraKey("HHH-21262")
public abstract class AbstractPartitionedSoftDeleteTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Shipment( 1, "eu" ) );
			session.persist( new Parcel( 1, "eu" ) );
			session.persist( new Invoice( 1, "eu" ) );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void removeInstance(SessionFactoryScope scope) {
		final var statements = scope.getCollectingStatementObserver();
		statements.clear();
		scope.inTransaction( session -> session.remove( session.find( Shipment.class, 1 ) ) );
		assertThat( statements.getSqlQueries() ).last().asString().contains( "region=?" );
		assertSoftDeleted( scope, Shipment.class, "partitioned_soft_shipment", 1 );
	}

	@Test
	void removeInstanceWithNullablePartitionKey(SessionFactoryScope scope) {
		final var statements = scope.getCollectingStatementObserver();
		statements.clear();
		scope.inTransaction( session -> session.remove( session.find( Parcel.class, 1 ) ) );
		assertThat( statements.getSqlQueries() ).last().asString().contains( "region=?" );
		assertSoftDeleted( scope, Parcel.class, "partitioned_soft_parcel", 1 );
	}

	@Test
	void removeVersionedInstance(SessionFactoryScope scope) {
		final var statements = scope.getCollectingStatementObserver();
		statements.clear();
		scope.inTransaction( session -> session.remove( session.find( Invoice.class, 1 ) ) );
		assertThat( statements.getSqlQueries() ).last().asString().contains( "region=?", "version=?" );
		assertSoftDeleted( scope, Invoice.class, "partitioned_soft_invoice", 1 );
	}

	/**
	 * An unloaded reference is loaded before it is removed, since its partition key is not known.
	 */
	@Test
	void removeUnloadedReference(SessionFactoryScope scope) {
		final var statements = scope.getCollectingStatementObserver();
		statements.clear();
		scope.inTransaction( session -> session.remove( session.getReference( Shipment.class, 1 ) ) );
		assertThat( statements.getSqlQueries() ).last().asString().contains( "region=?" );
		assertSoftDeleted( scope, Shipment.class, "partitioned_soft_shipment", 1 );
	}

	@Test
	void removeUnloadedVersionedReference(SessionFactoryScope scope) {
		final var statements = scope.getCollectingStatementObserver();
		statements.clear();
		scope.inTransaction( session -> session.remove( session.getReference( Invoice.class, 1 ) ) );
		assertThat( statements.getSqlQueries() ).last().asString().contains( "region=?", "version=?" );
		assertSoftDeleted( scope, Invoice.class, "partitioned_soft_invoice", 1 );
	}

	static void assertSoftDeleted(SessionFactoryScope scope, Class<?> entityClass, String table, int id) {
		scope.inTransaction( session -> {
			assertNull( session.find( entityClass, id ) );
			assertEquals( 1L, session.createNativeQuery( "select count(*) from " + table + " where id = :id", Long.class )
					.setParameter( "id", id )
					.getSingleResult() );
		} );
	}

	@Entity(name = "Shipment")
	@Table(name = "partitioned_soft_shipment")
	@SoftDelete
	public static class Shipment {
		@Id
		Integer id;
		@PartitionKey
		@Column(nullable = false)
		String region;

		Shipment() {
		}

		Shipment(Integer id, String region) {
			this.id = id;
			this.region = region;
		}
	}

	@Entity(name = "Parcel")
	@Table(name = "partitioned_soft_parcel")
	@SoftDelete
	public static class Parcel {
		@Id
		Integer id;
		@PartitionKey
		String region;

		Parcel() {
		}

		Parcel(Integer id, String region) {
			this.id = id;
			this.region = region;
		}
	}

	@Entity(name = "Invoice")
	@Table(name = "partitioned_soft_invoice")
	@SoftDelete
	public static class Invoice {
		@Id
		Integer id;
		@PartitionKey
		@Column(nullable = false)
		String region;
		@Version
		Integer version;

		Invoice() {
		}

		Invoice(Integer id, String region) {
			this.id = id;
			this.region = region;
		}
	}
}
