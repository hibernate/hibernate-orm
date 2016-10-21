/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.constraint;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Christian Beikov
 */
@TestForIssue( jiraKey = "HHH-11180" )
public class ForeignKeyConstraintTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Person.class, Student.class, Vehicle.class, VehicleBuyInfo.class, Car.class };
	}

	@Test
	public void testJoinColumn() {
		assertForeignKey("FK_CAR_OWNER", "OWNER_PERSON_ID");
	}

	@Test
	public void testJoinColumns() {
		assertForeignKey("FK_STUDENT_CAR", "CAR_NR", "CAR_VENDOR_NR");
	}

	@Test
	public void testJoinTable() {
		assertForeignKey("FK_VEHICLE_BUY_INFOS_STUDENT", "STUDENT_ID");
	}

	@Test
	public void testJoinTableInverse() {
		assertForeignKey("FK_VEHICLE_BUY_INFOS_VEHICLE_BUY_INFO", "VEHICLE_BUY_INFO_ID");
	}

	@Test
	public void testPrimaryKeyJoinColumn() {
		assertForeignKey("FK_STUDENT_PERSON", "PERSON_ID");
	}

	@Test
	public void testPrimaryKeyJoinColumns() {
		assertForeignKey( "FK_CAR_VEHICLE", "CAR_NR", "VENDOR_NR" );
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
				Iterator<ForeignKey> fkItr = table.getForeignKeyIterator();
				while ( fkItr.hasNext() ) {
					ForeignKey fk = fkItr.next();

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
	
	@Entity
	@Inheritance( strategy = InheritanceType.JOINED )
	public static class Person {
		@Id
		@GeneratedValue
		@javax.persistence.Column( nullable = false, unique = true)
		public long id;
	}
	
	@Entity
	@PrimaryKeyJoinColumn( name = "PERSON_ID", foreignKey = @javax.persistence.ForeignKey( name = "FK_STUDENT_PERSON" ) )
	public static class Student extends Person {

		@javax.persistence.Column( name = "MATRICULATION_NUMBER" )
		public String matriculationNumber;

		@ManyToOne
		@JoinColumns(
				value = {
						@JoinColumn( name = "CAR_NR", referencedColumnName = "CAR_NR" ),
						@JoinColumn( name = "CAR_VENDOR_NR", referencedColumnName = "VENDOR_NR" )
				},
				foreignKey = @javax.persistence.ForeignKey( name = "FK_STUDENT_CAR" )
		)
		public Car car;

		@ManyToMany
		@JoinTable(
				name = "VEHICLE_BUY_INFOS",
				foreignKey = @javax.persistence.ForeignKey( name = "FK_VEHICLE_BUY_INFOS_STUDENT" ),
				inverseForeignKey = @javax.persistence.ForeignKey( name = "FK_VEHICLE_BUY_INFOS_VEHICLE_BUY_INFO" ),
				joinColumns = @JoinColumn( name = "STUDENT_ID"),
				inverseJoinColumns = @JoinColumn( name = "VEHICLE_BUY_INFO_ID" )
		)
		@MapKeyJoinColumns(
				value = {
						@MapKeyJoinColumn( name = "VEHICLE_NR", referencedColumnName = "VEHICLE_NR" ),
						@MapKeyJoinColumn( name = "VEHICLE_VENDOR_NR", referencedColumnName = "VEHICLE_VENDOR_NR" )
				},
				foreignKey = @javax.persistence.ForeignKey( name = "FK_VEHICLE_BUY_INFOS_VEHICLE" )
		)
		public Map<Vehicle, VehicleBuyInfo> vehicleBuyInfos = new HashMap<>();
	}

	@Entity
	public static class VehicleBuyInfo {
		@Id
		@GeneratedValue
		public long id;
		public String info;
	}

	@Entity
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

	@Entity
	@SecondaryTable(
			name = "CAR_DETAILS",
			pkJoinColumns = {
					@PrimaryKeyJoinColumn( name = "CAR_NR", referencedColumnName = "CAR_NR" ),
					@PrimaryKeyJoinColumn( name = "CAR_VENDOR_NR", referencedColumnName = "VENDOR_NR" )
			},
			foreignKey = @javax.persistence.ForeignKey( name = "FK_CAR_DETAILS_CAR" )
	)
	@PrimaryKeyJoinColumns(
			value = {
					@PrimaryKeyJoinColumn( name = "CAR_NR", referencedColumnName = "VEHICLE_NR" ),
					@PrimaryKeyJoinColumn( name = "VENDOR_NR", referencedColumnName = "VEHICLE_VENDOR_NR" )
			},
			foreignKey = @javax.persistence.ForeignKey( name = "FK_CAR_VEHICLE" )
	)
	public static class Car extends Vehicle {

		public String color;

		@ManyToOne
		@JoinColumn( name = "OWNER_PERSON_ID", foreignKey = @javax.persistence.ForeignKey( name = "FK_CAR_OWNER") )
		public Person owner;

		@ElementCollection
		@CollectionTable(
				name = "OWNER_INFO",
				joinColumns = {
						@JoinColumn( name = "CAR_NR" ),
						@JoinColumn( name = "VENDOR_NR" )
				},
				foreignKey = @javax.persistence.ForeignKey( name = "FK_OWNER_INFO_CAR" )
		)
		@MapKeyJoinColumn( name = "PERSON_ID", foreignKey = @javax.persistence.ForeignKey( name = "FK_OWNER_INFO_PERSON" ) )
		public Map<Person, String> ownerInfo = new HashMap<>();

	}

	@Entity
	public static abstract class Company {

		@Id
		@GeneratedValue
		public long id;

		@ManyToOne
		@JoinColumn( name = "OWNER_ID" )
		public Person owner;
	}

	@Entity
	@AssociationOverride(
			name = "owner",
			joinColumns = @JoinColumn( name = "OWNER_PERSON_ID" ),
			foreignKey = @javax.persistence.ForeignKey( name = "FK_CUSTOMER_COMPANY_OWNER" )
	)
	public static class CustomerCompany extends Company {

	}
}
