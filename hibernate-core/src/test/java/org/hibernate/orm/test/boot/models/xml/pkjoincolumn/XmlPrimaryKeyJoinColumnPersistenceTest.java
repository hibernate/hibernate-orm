package org.hibernate.orm.test.boot.models.xml.pkjoincolumn;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;

import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ModelsContext;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

/// Maps entities only through orm.xml `<primary-key-join-column/>` (on a `JOINED` subclass,
/// on a `<secondary-table/>` and on a `<one-to-one/>`), then persists and reads them back
/// through a real `SessionFactory`, checking the rows land in the columns named in the XML
/// and that the foreign key names declared in the XML are applied to the `JOINED` subclass
/// and secondary table keys. For the `<one-to-one/>`, the `<primary-key-foreign-key/>` is only
/// checked to be transformed into `@PrimaryKeyJoinColumns(foreignKey = ...)`, since the binder
/// does not read the foreign key from there.
@DomainModel(xmlMappings = XmlPrimaryKeyJoinColumnPersistenceTest.XML_MAPPING)
@SessionFactory
@JiraKey("HHH-21252")
public class XmlPrimaryKeyJoinColumnPersistenceTest {

	static final String XML_MAPPING =
			"org/hibernate/orm/test/boot/models/xml/pkjoincolumn/primary-key-join-column.orm.xml";

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void joinedSubclassRowIsKeyedByXmlPrimaryKeyJoinColumn(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Dog( 1L, "Rex", "beagle" ) ) );

		scope.inTransaction( session -> {
			final String breed = session.createNativeQuery( "select breed from dog where dog_id = :id", String.class )
					.setParameter( "id", 1L )
					.getSingleResult();
			assertThat( breed ).isEqualTo( "beagle" );

			final Dog dog = session.find( Dog.class, 1L );
			assertThat( dog.name ).isEqualTo( "Rex" );
			assertThat( dog.breed ).isEqualTo( "beagle" );

			final Animal animal = session.createQuery( "from Animal where id = :id", Animal.class )
					.setParameter( "id", 1L )
					.getSingleResult();
			assertThat( animal ).isInstanceOf( Dog.class );
		} );
	}

	@Test
	void joinedSubclassForeignKeyNameComesFromNestedForeignKey(SessionFactoryScope scope) {
		final Table dogTable = entityBinding( scope, Dog.class ).getTable();

		assertThat( foreignKeyNames( dogTable ) ).containsExactly( "fk_dog_animal" );
	}

	@Test
	void joinedSubclassWithPrimaryKeyForeignKeyIsPersisted(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Car( 2L, "Fiat", 4 ) ) );

		scope.inTransaction( session -> {
			final Integer seats = session.createNativeQuery( "select seats from car where car_id = :id", Integer.class )
					.setParameter( "id", 2L )
					.getSingleResult();
			assertThat( seats ).isEqualTo( 4 );

			final Car car = session.find( Car.class, 2L );
			assertThat( car.maker ).isEqualTo( "Fiat" );
			assertThat( car.seats ).isEqualTo( 4 );
		} );
	}

	@Test
	void joinedSubclassForeignKeyNameComesFromPrimaryKeyForeignKey(SessionFactoryScope scope) {
		final Table carTable = entityBinding( scope, Car.class ).getTable();

		assertThat( foreignKeyNames( carTable ) ).containsExactly( "fk_car_vehicle" );
	}

	@Test
	void secondaryTableRowIsKeyedByXmlPrimaryKeyJoinColumn(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Customer( 3L, "Jane", "vip" ) ) );

		scope.inTransaction( session -> {
			final String detail = session.createNativeQuery(
							"select detail from customer_detail where customer_id = :id",
							String.class
					)
					.setParameter( "id", 3L )
					.getSingleResult();
			assertThat( detail ).isEqualTo( "vip" );

			final Customer customer = session.find( Customer.class, 3L );
			assertThat( customer.name ).isEqualTo( "Jane" );
			assertThat( customer.detail ).isEqualTo( "vip" );
		} );
	}

	@Test
	void secondaryTableForeignKeyNameComesFromPrimaryKeyForeignKey(SessionFactoryScope scope) {
		final List<Join> joins = entityBinding( scope, Customer.class ).getJoins();
		assertThat( joins ).hasSize( 1 );

		assertThat( foreignKeyNames( joins.get( 0 ).getTable() ) ).containsExactly( "fk_customer_detail" );
	}

	@Test
	void oneToOneSharingPrimaryKeyIsPersistedAndLoaded(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Person person = new Person( 4L, "John" );
			session.persist( person );
			session.persist( new Passport( 4L, "AB123", person ) );
		} );

		scope.inTransaction( session -> {
			final Passport passport = session.find( Passport.class, 4L );
			assertThat( passport.passportNumber ).isEqualTo( "AB123" );
			assertThat( passport.person ).isNotNull();
			assertThat( passport.person.name ).isEqualTo( "John" );
		} );
	}

	/// The binder does not read the foreign key of a `@PrimaryKeyJoinColumns` placed on a one-to-one,
	/// so this only checks that the XML is transformed into the corresponding annotation.
	@Test
	void oneToOnePrimaryKeyForeignKeyIsMappedToPrimaryKeyJoinColumnsForeignKey(SessionFactoryScope scope) {
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( XML_MAPPING )
				.build();
		final ModelsContext modelsContext = createBuildingContext(
				managedResources,
				scope.getMetadataImplementor().getMetadataBuildingOptions().getServiceRegistry()
		);

		final PrimaryKeyJoinColumns primaryKeyJoinColumns = modelsContext.getClassDetailsRegistry()
				.getClassDetails( Passport.class.getName() )
				.findFieldByName( "person" )
				.getDirectAnnotationUsage( PrimaryKeyJoinColumns.class );

		assertThat( primaryKeyJoinColumns ).isNotNull();
		assertThat( primaryKeyJoinColumns.value() ).extracting( PrimaryKeyJoinColumn::name ).containsExactly( "id" );
		assertThat( primaryKeyJoinColumns.foreignKey().name() ).isEqualTo( "fk_passport_person" );
	}

	private static PersistentClass entityBinding(SessionFactoryScope scope, Class<?> entityClass) {
		return scope.getMetadataImplementor().getEntityBinding( entityClass.getName() );
	}

	private static List<String> foreignKeyNames(Table table) {
		final List<String> names = new ArrayList<>();
		for ( final ForeignKey foreignKey : table.getForeignKeyCollection() ) {
			names.add( foreignKey.getName() );
		}
		return names;
	}

	public static class Animal {
		Long id;
		String name;

		Animal() {
		}

		Animal(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	public static class Dog extends Animal {
		String breed;

		Dog() {
		}

		Dog(Long id, String name, String breed) {
			super( id, name );
			this.breed = breed;
		}
	}

	public static class Vehicle {
		Long id;
		String maker;

		Vehicle() {
		}

		Vehicle(Long id, String maker) {
			this.id = id;
			this.maker = maker;
		}
	}

	public static class Car extends Vehicle {
		Integer seats;

		Car() {
		}

		Car(Long id, String maker, Integer seats) {
			super( id, maker );
			this.seats = seats;
		}
	}

	public static class Customer {
		Long id;
		String name;
		String detail;

		Customer() {
		}

		Customer(Long id, String name, String detail) {
			this.id = id;
			this.name = name;
			this.detail = detail;
		}
	}

	public static class Person {
		Long id;
		String name;

		Person() {
		}

		Person(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	public static class Passport {
		Long id;
		String passportNumber;
		Person person;

		Passport() {
		}

		Passport(Long id, String passportNumber, Person person) {
			this.id = id;
			this.passportNumber = passportNumber;
			this.person = person;
		}
	}
}
