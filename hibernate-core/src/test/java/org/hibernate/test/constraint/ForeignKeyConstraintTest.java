/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.constraint;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.AssociationOverride;
import javax.persistence.CollectionTable;
import javax.persistence.ConstraintMode;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.MapKeyJoinColumns;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.PrimaryKeyJoinColumns;
import javax.persistence.SecondaryTable;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.mapping.Column;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Christian Beikov
 */
@TestForIssue( jiraKey = "HHH-11180" )
public class ForeignKeyConstraintTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				CreditCard.class,
				Person.class,
				Student.class,
				Professor.class,
				Vehicle.class,
				VehicleBuyInfo.class,
				Car.class,
				Truck.class
		};
	}

	@Test
	public void testJoinColumn() {
		assertForeignKey( "FK_CAR_OWNER", "OWNER_PERSON_ID" );
		assertForeignKey( "FK_CAR_OWNER3", "OWNER_PERSON_ID3" );
		assertForeignKey( "FK_PERSON_CC", "PERSON_CC_ID" );
		assertNoForeignKey( "FK_CAR_OWNER2", "OWNER_PERSON_ID2" );
		assertNoForeignKey( "FK_CAR_OWNER4", "OWNER_PERSON_ID4" );
		assertNoForeignKey( "FK_PERSON_CC2", "PERSON_CC_ID2" );
	}

	@Test
	public void testJoinColumns() {
		assertForeignKey( "FK_STUDENT_CAR", "CAR_NR", "CAR_VENDOR_NR" );
		assertForeignKey( "FK_STUDENT_CAR3", "CAR_NR3", "CAR_VENDOR_NR3" );
		assertNoForeignKey( "FK_STUDENT_CAR2", "CAR_NR2", "CAR_VENDOR_NR2" );
		assertNoForeignKey( "FK_STUDENT_CAR4", "CAR_NR4", "CAR_VENDOR_NR4" );
	}

	@Test
	public void testJoinTable() {
		assertForeignKey( "FK_VEHICLE_BUY_INFOS_STUDENT", "STUDENT_ID" );
	}

	@Test
	public void testJoinTableInverse() {
		assertForeignKey( "FK_VEHICLE_BUY_INFOS_VEHICLE_BUY_INFO", "VEHICLE_BUY_INFO_ID" );
	}

	@Test
	public void testPrimaryKeyJoinColumn() {
		assertForeignKey( "FK_STUDENT_PERSON", "PERSON_ID" );
		assertNoForeignKey( "FK_PROFESSOR_PERSON", "PERSON_ID" );
	}

	@Test
	public void testPrimaryKeyJoinColumns() {
		assertForeignKey( "FK_CAR_VEHICLE", "CAR_NR", "VENDOR_NR" );
		assertNoForeignKey( "FK_TRUCK_VEHICLE", "CAR_NR", "VENDOR_NR" );
	}

	@Test
	public void testCollectionTable() {
		assertForeignKey( "FK_OWNER_INFO_CAR", "CAR_NR", "VENDOR_NR" );
	}

	@Test
	public void testMapKeyJoinColumn() {
		assertForeignKey( "FK_OWNER_INFO_PERSON", "PERSON_ID" );
	}

	@Test
	public void testMapKeyJoinColumns() {
		assertForeignKey( "FK_VEHICLE_BUY_INFOS_VEHICLE", "VEHICLE_NR", "VEHICLE_VENDOR_NR" );
	}

	@Test
	public void testAssociationOverride() {
		assertForeignKey( "FK_CUSTOMER_COMPANY_OWNER", "OWNER_PERSON_ID" );
	}

	@Test
	public void testSecondaryTable() {
		assertForeignKey( "FK_CAR_DETAILS_CAR", "CAR_NR", "CAR_VENDOR_NR" );
	}

	private void assertForeignKey(String foreignKeyName, String... columns) {
		Set<String> columnSet = new LinkedHashSet<>( Arrays.asList( columns ) );
		for ( Namespace namespace : metadata().getDatabase().getNamespaces() ) {
			for ( org.hibernate.mapping.Table table : namespace.getTables() ) {
				Iterator<org.hibernate.mapping.ForeignKey> fkItr = table.getForeignKeyIterator();
				while ( fkItr.hasNext() ) {
					org.hibernate.mapping.ForeignKey fk = fkItr.next();

					if ( foreignKeyName.equals( fk.getName() ) ) {
						assertEquals( "ForeignKey column count not like expected", columnSet.size(), fk.getColumnSpan() );
						List<String> columnNames = fk.getColumns().stream().map(Column::getName).collect(Collectors.toList());
						assertTrue(
								"ForeignKey columns [" + columnNames + "] do not match expected columns [" + columnSet + "]",
								columnSet.containsAll( columnNames )
						);
						return;
					}
				}
			}
		}
		fail( "ForeignKey '" + foreignKeyName + "' could not be found!" );
	}

	private void assertNoForeignKey(String foreignKeyName, String... columns) {
		Set<String> columnSet = new LinkedHashSet<>( Arrays.asList( columns ) );
		for ( Namespace namespace : metadata().getDatabase().getNamespaces() ) {
			for ( org.hibernate.mapping.Table table : namespace.getTables() ) {
				Iterator<org.hibernate.mapping.ForeignKey> fkItr = table.getForeignKeyIterator();
				while ( fkItr.hasNext() ) {
					org.hibernate.mapping.ForeignKey fk = fkItr.next();
					assertFalse(
							"ForeignKey [" + foreignKeyName + "] defined and shouldn't have been.",
							foreignKeyName.equals( fk.getName() )
					);
				}
			}
		}
	}

	@Entity(name = "CreditCard")
	public static class CreditCard {
		@Id
		public String number;
	}
	
	@Entity(name = "Person")
	@Inheritance( strategy = InheritanceType.JOINED )
	public static class Person {
		@Id
		@GeneratedValue
		@javax.persistence.Column( nullable = false, unique = true)
		public long id;

		@OneToMany
		@JoinColumn(name = "PERSON_CC_ID", foreignKey = @ForeignKey( name = "FK_PERSON_CC" ) )
		public List<CreditCard> creditCards;

		@OneToMany
		@JoinColumn(name = "PERSON_CC_ID2", foreignKey = @ForeignKey( name = "FK_PERSON_CC2", value = ConstraintMode.NO_CONSTRAINT ) )
		public List<CreditCard> creditCards2;
	}

	@Entity(name = "Professor")
	@PrimaryKeyJoinColumn(
			name = "PERSON_ID",
			foreignKey = @ForeignKey( name = "FK_PROFESSOR_PERSON", value = ConstraintMode.NO_CONSTRAINT )
	)
	public static class Professor extends Person {

	}

	@Entity(name = "Student")
	@PrimaryKeyJoinColumn( name = "PERSON_ID", foreignKey = @ForeignKey( name = "FK_STUDENT_PERSON" ) )
	public static class Student extends Person {

		@javax.persistence.Column( name = "MATRICULATION_NUMBER" )
		public String matriculationNumber;

		@ManyToOne
		@JoinColumns(
				value = {
						@JoinColumn( name = "CAR_NR", referencedColumnName = "CAR_NR" ),
						@JoinColumn( name = "CAR_VENDOR_NR", referencedColumnName = "VENDOR_NR" )
				},
				foreignKey = @ForeignKey( name = "FK_STUDENT_CAR" )
		)
		public Car car;

		@ManyToOne
		@JoinColumns(
				value = {
						@JoinColumn( name = "CAR_NR2", referencedColumnName = "CAR_NR" ),
						@JoinColumn( name = "CAR_VENDOR_NR2", referencedColumnName = "VENDOR_NR" )
				},
				foreignKey = @ForeignKey( name = "FK_STUDENT_CAR2", value = ConstraintMode.NO_CONSTRAINT )
		)
		public Car car2;

		@OneToOne
		@JoinColumns(
				value = {
						@JoinColumn( name = "CAR_NR3", referencedColumnName = "CAR_NR" ),
						@JoinColumn( name = "CAR_VENDOR_NR3", referencedColumnName = "VENDOR_NR" )
				},
				foreignKey = @ForeignKey( name = "FK_STUDENT_CAR3" )
		)
		public Car car3;

		@OneToOne
		@JoinColumns(
				value = {
						@JoinColumn( name = "CAR_NR4", referencedColumnName = "CAR_NR" ),
						@JoinColumn( name = "CAR_VENDOR_NR4", referencedColumnName = "VENDOR_NR" )
				},
				foreignKey = @ForeignKey( name = "FK_STUDENT_CAR4", value = ConstraintMode.NO_CONSTRAINT )
		)
		public Car car4;

		@ManyToMany
		@JoinTable(
				name = "VEHICLE_BUY_INFOS",
				foreignKey = @ForeignKey( name = "FK_VEHICLE_BUY_INFOS_STUDENT" ),
				inverseForeignKey = @ForeignKey( name = "FK_VEHICLE_BUY_INFOS_VEHICLE_BUY_INFO" ),
				joinColumns = @JoinColumn( name = "STUDENT_ID"),
				inverseJoinColumns = @JoinColumn( name = "VEHICLE_BUY_INFO_ID" )
		)
		@MapKeyJoinColumns(
				value = {
						@MapKeyJoinColumn( name = "VEHICLE_NR", referencedColumnName = "VEHICLE_NR" ),
						@MapKeyJoinColumn( name = "VEHICLE_VENDOR_NR", referencedColumnName = "VEHICLE_VENDOR_NR" )
				},
				foreignKey = @ForeignKey( name = "FK_VEHICLE_BUY_INFOS_VEHICLE" )
		)
		public Map<Vehicle, VehicleBuyInfo> vehicleBuyInfos = new HashMap<>();
	}

	@Entity(name = "VehicleBuyInfo")
	public static class VehicleBuyInfo {
		@Id
		@GeneratedValue
		public long id;
		public String info;
	}

	@Entity(name = "Vehicle")
	@Inheritance( strategy = InheritanceType.JOINED )
	public static class Vehicle {
		@EmbeddedId
		public VehicleId id;
	}

	@Embeddable
	public static class VehicleId implements Serializable {
		@javax.persistence.Column( name = "VEHICLE_VENDOR_NR" )
		public long vehicleVendorNumber;
		@javax.persistence.Column( name = "VEHICLE_NR" )
		public long vehicleNumber;

		@Override
		public boolean equals(Object o) {
			if ( this == o ) return true;
			if ( !( o instanceof VehicleId ) ) return false;

			VehicleId vehicleId = (VehicleId) o;

			if ( vehicleVendorNumber != vehicleId.vehicleVendorNumber ) return false;
			return vehicleNumber == vehicleId.vehicleNumber;
		}

		@Override
		public int hashCode() {
			int result = (int) ( vehicleVendorNumber ^ ( vehicleVendorNumber >>> 32 ) );
			result = 31 * result + (int) ( vehicleNumber ^ ( vehicleNumber >>> 32 ) );
			return result;
		}
	}

	@Entity(name = "Car")
	@SecondaryTable(
			name = "CAR_DETAILS",
			pkJoinColumns = {
					@PrimaryKeyJoinColumn( name = "CAR_NR", referencedColumnName = "CAR_NR" ),
					@PrimaryKeyJoinColumn( name = "CAR_VENDOR_NR", referencedColumnName = "VENDOR_NR" )
			},
			foreignKey = @ForeignKey( name = "FK_CAR_DETAILS_CAR" )
	)
	@PrimaryKeyJoinColumns(
			value = {
					@PrimaryKeyJoinColumn( name = "CAR_NR", referencedColumnName = "VEHICLE_NR" ),
					@PrimaryKeyJoinColumn( name = "VENDOR_NR", referencedColumnName = "VEHICLE_VENDOR_NR" )
			},
			foreignKey = @ForeignKey( name = "FK_CAR_VEHICLE" )
	)
	public static class Car extends Vehicle {

		public String color;

		@ManyToOne
		@JoinColumn( name = "OWNER_PERSON_ID", foreignKey = @ForeignKey( name = "FK_CAR_OWNER") )
		public Person owner;

		@ManyToOne
		@JoinColumn( name = "OWNER_PERSON_ID2", foreignKey = @ForeignKey( name = "FK_CAR_OWNER2", value = ConstraintMode.NO_CONSTRAINT ) )
		public Person owner2;

		@OneToOne
		@JoinColumn( name = "OWNER_PERSON_ID3", foreignKey = @ForeignKey( name = "FK_CAR_OWNER3") )
		public Person owner3;

		@OneToOne
		@JoinColumn( name = "OWNER_PERSON_ID4", foreignKey = @ForeignKey( name = "FK_CAR_OWNER4", value = ConstraintMode.NO_CONSTRAINT ) )
		public Person owner4;

		@ElementCollection
		@CollectionTable(
				name = "OWNER_INFO",
				joinColumns = {
						@JoinColumn( name = "CAR_NR" ),
						@JoinColumn( name = "VENDOR_NR" )
				},
				foreignKey = @ForeignKey( name = "FK_OWNER_INFO_CAR" )
		)
		@MapKeyJoinColumn( name = "PERSON_ID", foreignKey = @ForeignKey( name = "FK_OWNER_INFO_PERSON" ) )
		public Map<Person, String> ownerInfo = new HashMap<>();

	}

	@Entity(name = "Truck")
	@PrimaryKeyJoinColumns(
			value = {
					@PrimaryKeyJoinColumn( name = "CAR_NR", referencedColumnName = "VEHICLE_NR" ),
					@PrimaryKeyJoinColumn( name = "VENDOR_NR", referencedColumnName = "VEHICLE_VENDOR_NR" )
			},
			foreignKey = @ForeignKey( name = "FK_TRUCK_VEHICLE", value = ConstraintMode.NO_CONSTRAINT )
	)
	public static class Truck extends Vehicle {
		public boolean fourWheelDrive;
	}

	@Entity(name = "Company")
	public static abstract class Company {

		@Id
		@GeneratedValue
		public long id;

		@ManyToOne
		@JoinColumn( name = "OWNER_ID" )
		public Person owner;
	}

	@Entity(name = "CustomerCompany")
	@AssociationOverride(
			name = "owner",
			joinColumns = @JoinColumn( name = "OWNER_PERSON_ID" ),
			foreignKey = @ForeignKey( name = "FK_CUSTOMER_COMPANY_OWNER" )
	)
	public static class CustomerCompany extends Company {

	}
}
