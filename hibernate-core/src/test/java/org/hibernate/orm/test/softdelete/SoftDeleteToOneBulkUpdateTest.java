package org.hibernate.orm.test.softdelete;

import org.hibernate.annotations.SoftDelete;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A bulk update which assigns a to-one whose target entity is soft-deleted
 * writes the foreign key, without a join to the target.
 */
@DomainModel(annotatedClasses = {
		SoftDeleteToOneBulkUpdateTest.Customer.class,
		SoftDeleteToOneBulkUpdateTest.Pet.class
})
@SessionFactory(useCollectingStatementInspector = true)
@JiraKey("HHH-20202")
class SoftDeleteToOneBulkUpdateTest {

	@Entity(name = "Customer")
	@Table(name = "customers")
	@SoftDelete
	static class Customer {
		@Id
		Long id;
		@OneToOne
		@JoinColumn(name = "pet_id")
		Pet pet;
	}

	@Entity(name = "Pet")
	@Table(name = "pets")
	@SoftDelete
	static class Pet {
		@Id
		Long id;
	}

	@BeforeEach
	void createData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var pet = new Pet();
			pet.id = 1L;
			session.persist( pet );
			final var other = new Pet();
			other.id = 2L;
			session.persist( other );
			final var customer = new Customer();
			customer.id = 1L;
			customer.pet = pet;
			session.persist( customer );
		} );
	}

	@AfterEach
	void dropData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void assignParameter(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		final int count = scope.fromTransaction( session -> session
				.createMutationQuery( "update Customer c set c.pet = :pet where c.id = :id" )
				.setParameter( "pet", session.getReference( Pet.class, 2L ) )
				.setParameter( "id", 1L )
				.executeUpdate() );
		assertThat( count ).isEqualTo( 1 );
		assertThat( inspector.getSqlQueries() ).hasSize( 1 );
		assertThat( inspector.getSqlQueries().get( 0 ).toLowerCase() ).doesNotContain( " join " );
		scope.inTransaction( session -> {
			final var customer = session.find( Customer.class, 1L );
			assertThat( customer ).isNotNull();
			assertThat( customer.pet.id ).isEqualTo( 2L );
		} );
	}

	@Test
	void assignNull(SessionFactoryScope scope) {
		final int count = scope.fromTransaction( session -> session
				.createMutationQuery( "update Customer c set c.pet = null where c.id = 1" )
				.executeUpdate() );
		assertThat( count ).isEqualTo( 1 );
		scope.inTransaction( session -> {
			final var customer = session.find( Customer.class, 1L );
			assertThat( customer ).isNotNull();
			assertThat( customer.pet ).isNull();
		} );
	}
}
