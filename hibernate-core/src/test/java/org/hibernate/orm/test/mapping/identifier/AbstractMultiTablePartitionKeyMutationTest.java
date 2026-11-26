package org.hibernate.orm.test.mapping.identifier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import org.hibernate.annotations.PartitionKey;
import org.hibernate.annotations.SecondaryRow;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.mapping.identifier.AbstractPartitionKeyMutationTest.assertRestrictedByPartitionKey;

/**
 * Only the table which has the partition key column is restricted by the
 * partition key, when the other tables of the entity have a foreign key to it.
 * Such a foreign key cannot reference a primary key which includes the
 * partition key.
 */
public abstract class AbstractMultiTablePartitionKeyMutationTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new DetailedBox( 1, "eu" ) );
			session.persist( new Dog( 1, "eu" ) );
		} );
		scope.getCollectingStatementObserver().clear();
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void updateAndRemoveWithSecondaryTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var box = session.find( DetailedBox.class, 1 );
			box.label = "updated";
			box.note = "updated";
		} );
		assertRestrictedByPartitionKey( scope, "partition_detailed_box", "partition_box_detail" );
		scope.inTransaction( session -> {
			final var box = session.find( DetailedBox.class, 1 );
			assertThat( box.label ).isEqualTo( "updated" );
			assertThat( box.note ).isEqualTo( "updated" );
			session.remove( box );
		} );
		assertRestrictedByPartitionKey( scope, "partition_detailed_box", "partition_box_detail" );
		scope.inTransaction( session -> assertThat( session.find( DetailedBox.class, 1 ) ).isNull() );
	}

	@Test
	void updateAndRemoveJoinedSubclass(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var dog = session.find( Dog.class, 1 );
			dog.name = "updated";
			dog.breed = "updated";
		} );
		assertRestrictedByPartitionKey( scope, "partition_animal", "partition_dog" );
		scope.inTransaction( session -> {
			final var dog = session.find( Dog.class, 1 );
			assertThat( dog.name ).isEqualTo( "updated" );
			assertThat( dog.breed ).isEqualTo( "updated" );
			session.remove( dog );
		} );
		assertRestrictedByPartitionKey( scope, "partition_animal", "partition_dog" );
		scope.inTransaction( session -> assertThat( session.find( Dog.class, 1 ) ).isNull() );
	}

	@Test
	@JiraKey("HHH-21279")
	void updateAndDeleteWithSecondaryTableInStatelessSession(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			final var box = session.get( DetailedBox.class, 1 );
			box.label = "updated";
			box.note = "updated";
			session.update( box );
		} );
		assertRestrictedByPartitionKey( scope, "partition_detailed_box", "partition_box_detail" );
		scope.inStatelessTransaction( session -> {
			final var box = session.get( DetailedBox.class, 1 );
			assertThat( box.label ).isEqualTo( "updated" );
			assertThat( box.note ).isEqualTo( "updated" );
			session.delete( box );
		} );
		assertRestrictedByPartitionKey( scope, "partition_detailed_box", "partition_box_detail" );
		scope.inTransaction( session -> assertThat( session.find( DetailedBox.class, 1 ) ).isNull() );
	}

	@Test
	@JiraKey("HHH-21279")
	void updateAndDeleteJoinedSubclassInStatelessSession(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			final var dog = session.get( Dog.class, 1 );
			dog.name = "updated";
			dog.breed = "updated";
			session.update( dog );
		} );
		assertRestrictedByPartitionKey( scope, "partition_animal", "partition_dog" );
		scope.inStatelessTransaction( session -> {
			final var dog = session.get( Dog.class, 1 );
			assertThat( dog.name ).isEqualTo( "updated" );
			assertThat( dog.breed ).isEqualTo( "updated" );
			session.delete( dog );
		} );
		assertRestrictedByPartitionKey( scope, "partition_animal", "partition_dog" );
		scope.inTransaction( session -> assertThat( session.find( Dog.class, 1 ) ).isNull() );
	}

	@Entity(name = "DetailedBox")
	@Table(name = "partition_detailed_box")
	@SecondaryTable(name = "partition_box_detail")
	@SecondaryRow(table = "partition_box_detail", optional = false)
	public static class DetailedBox {
		@Id
		Integer id;
		@PartitionKey
		@Column(nullable = false)
		String region;
		String label;
		@Column(table = "partition_box_detail")
		String note;

		DetailedBox() {
		}

		DetailedBox(Integer id, String region) {
			this.id = id;
			this.region = region;
		}
	}

	@Entity(name = "Animal")
	@Table(name = "partition_animal")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Animal {
		@Id
		Integer id;
		@PartitionKey
		@Column(nullable = false)
		String region;
		String name;

		Animal() {
		}

		Animal(Integer id, String region) {
			this.id = id;
			this.region = region;
		}
	}

	@Entity(name = "Dog")
	@Table(name = "partition_dog")
	public static class Dog extends Animal {
		String breed;

		Dog() {
		}

		Dog(Integer id, String region) {
			super( id, region );
		}
	}
}
