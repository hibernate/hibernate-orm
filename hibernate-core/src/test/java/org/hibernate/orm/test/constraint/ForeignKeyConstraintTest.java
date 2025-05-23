/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.constraint;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.mapping.Column;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.MapKeyJoinColumns;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;
import jakarta.persistence.SecondaryTable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Christian Beikov
 */
@JiraKey( value = "HHH-11180" )
@DomainModel(
		annotatedClasses = {
				ForeignKeyConstraintTest.CreditCard.class,
				ForeignKeyConstraintTest.Person.class,
				ForeignKeyConstraintTest.Student.class,
				ForeignKeyConstraintTest.Professor.class,
				ForeignKeyConstraintTest.Vehicle.class,
				ForeignKeyConstraintTest.VehicleBuyInfo.class,
				ForeignKeyConstraintTest.Car.class,
				ForeignKeyConstraintTest.Truck.class,
				ForeignKeyConstraintTest.Company.class,
				ForeignKeyConstraintTest.PlanItem.class,
				ForeignKeyConstraintTest.Task.class
		}
)
@SessionFactory
public class ForeignKeyConstraintTest {
	@Test
	public void testJoinColumn(DomainModelScope scope) {
		assertForeignKey( scope, "FK_CAR_OWNER", "OWNER_PERSON_ID" );
		assertForeignKey( scope, "FK_CAR_OWNER3", "OWNER_PERSON_ID3" );
		assertForeignKey( scope, "FK_PERSON_CC", "PERSON_CC_ID" );
		assertNoForeignKey( scope, "FK_CAR_OWNER2", "OWNER_PERSON_ID2" );
		assertNoForeignKey( scope, "FK_CAR_OWNER4", "OWNER_PERSON_ID4" );
		assertNoForeignKey( scope, "FK_PERSON_CC2", "PERSON_CC_ID2" );
	}

	@Test
	public void testJoinColumns(DomainModelScope scope) {
		assertForeignKey( scope, "FK_STUDENT_CAR", "CAR_NR", "CAR_VENDOR_NR" );
		assertForeignKey( scope, "FK_STUDENT_CAR3", "CAR_NR3", "CAR_VENDOR_NR3" );
		assertNoForeignKey( scope, "FK_STUDENT_CAR2", "CAR_NR2", "CAR_VENDOR_NR2" );
		assertNoForeignKey( scope, "FK_STUDENT_CAR4", "CAR_NR4", "CAR_VENDOR_NR4" );
	}

	@Test
	public void testJoinTable(DomainModelScope scope) {
		assertForeignKey( scope, "FK_VEHICLE_BUY_INFOS_STUDENT", "STUDENT_ID" );
	}

	@Test
	public void testJoinTableInverse(DomainModelScope scope) {
		assertForeignKey( scope, "FK_VEHICLE_BUY_INFOS_VEHICLE_BUY_INFO", "VEHICLE_BUY_INFO_ID" );
	}

	@Test
	public void testPrimaryKeyJoinColumn(DomainModelScope scope) {
		assertForeignKey( scope, "FK_STUDENT_PERSON", "PERSON_ID" );
		assertNoForeignKey( scope, "FK_PROFESSOR_PERSON", "PERSON_ID" );
	}

	@Test
	public void testPrimaryKeyJoinColumns(DomainModelScope scope) {
		assertForeignKey( scope, "FK_CAR_VEHICLE", "CAR_NR", "VENDOR_NR" );
		assertNoForeignKey( scope, "FK_TRUCK_VEHICLE", "CAR_NR", "VENDOR_NR" );
	}

	@Test
	public void testCollectionTable(DomainModelScope scope) {
		assertForeignKey( scope, "FK_OWNER_INFO_CAR", "CAR_NR", "VENDOR_NR" );
	}

	@Test
	public void testMapKeyJoinColumn(DomainModelScope scope) {
		assertForeignKey( scope, "FK_OWNER_INFO_PERSON", "PERSON_ID" );
	}

	@Test
	public void testMapKeyJoinColumns(DomainModelScope scope) {
		assertForeignKey( scope, "FK_VEHICLE_BUY_INFOS_VEHICLE", "VEHICLE_NR", "VEHICLE_VENDOR_NR" );
	}

	@Test
	public void testMapForeignKeyJoinColumnCollection(DomainModelScope scope) {
		assertForeignKey( scope, "FK_PROPERTIES_TASK", "task_id" );
	}

	@Test
	public void testMapForeignKeyCollection(DomainModelScope scope) {
		assertForeignKey( scope, "FK_ATTRIBUTES_TASK", "task_id" );
	}

	@Test
	public void testAssociationOverride(DomainModelScope scope) {
		// class level association overrides
		assertForeignKey( scope, "FK_COMPANY_OWNER", "OWNER_PERSON_ID" );
		assertForeignKey( scope, "FK_COMPANY_CREDIT_CARD", "CREDIT_CARD_ID" );
		assertForeignKey( scope, "FK_COMPANY_CREDIT_CARD3", "CREDIT_CARD_ID3" );
		assertNoForeignKey( scope, "FK_COMPANY_OWNER2", "OWNER_PERSON_ID2" );
		assertNoForeignKey( scope, "FK_COMPANY_CREDIT_CARD2", "CREDIT_CARD_ID2" );
		assertNoForeignKey( scope, "FK_COMPANY_CREDIT_CARD4", "CREDIT_CARD_ID4" );

		// embeddable association overrides
		assertForeignKey( scope, "FK_COMPANY_CARD", "AO_CI_CC_ID" );
		assertNoForeignKey( scope, "FK_COMPANY_CARD2", "AO_CI_CC_ID2" );
		assertForeignKey( scope, "FK_COMPANY_CARD3", "AO_CI_CC_ID3" );
		assertNoForeignKey( scope, "FK_COMPANY_CARD4", "AO_CI_CC_ID4" );
	}

	@Test
	public void testSecondaryTable(DomainModelScope scope) {
		assertForeignKey( scope, "FK_CAR_DETAILS_CAR", "CAR_NR", "CAR_VENDOR_NR" );
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.get( Student.class, 1L )
		);
	}

	private void assertForeignKey(DomainModelScope scope, String foreignKeyName, String... columns) {
		Set<String> columnSet = new LinkedHashSet<>( Arrays.asList( columns ) );
		for ( Namespace namespace : scope.getDomainModel().getDatabase().getNamespaces() ) {
			for ( org.hibernate.mapping.Table table : namespace.getTables() ) {
				Iterator<org.hibernate.mapping.ForeignKey> fkItr = table.getForeignKeyCollection().iterator();
				while ( fkItr.hasNext() ) {
					org.hibernate.mapping.ForeignKey fk = fkItr.next();

					if ( foreignKeyName.equals( fk.getName() ) ) {
						assertEquals( columnSet.size(), fk.getColumnSpan(),
								"ForeignKey column count not like expected" );
						List<String> columnNames = fk.getColumns().stream().map(Column::getName).toList();
						assertTrue( columnSet.containsAll( columnNames ),
								"ForeignKey columns [" + columnNames + "] do not match expected columns [" + columnSet + "]" );
						return;
					}
				}
			}
		}
		fail( "ForeignKey '" + foreignKeyName + "' could not be found!" );
	}

	private void assertNoForeignKey(DomainModelScope scope, String foreignKeyName, String... columns) {
		for ( Namespace namespace : scope.getDomainModel().getDatabase().getNamespaces() ) {
			for ( var table : namespace.getTables() ) {
				for ( var fk : table.getForeignKeyCollection() ) {
					assertNotEquals( foreignKeyName, fk.getName(),
							"ForeignKey [" + foreignKeyName + "] defined and shouldn't have been." );
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
		@jakarta.persistence.Column( nullable = false, unique = true)
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

		@jakarta.persistence.Column( name = "MATRICULATION_NUMBER" )
		public String matriculationNumber;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumns(
				value = {
						@JoinColumn( name = "CAR_NR", referencedColumnName = "CAR_NR" ),
						@JoinColumn( name = "CAR_VENDOR_NR", referencedColumnName = "VENDOR_NR" )
				},
				foreignKey = @ForeignKey( name = "FK_STUDENT_CAR" )
		)
		public Car car;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumns(
				value = {
						@JoinColumn( name = "CAR_NR2", referencedColumnName = "CAR_NR" ),
						@JoinColumn( name = "CAR_VENDOR_NR2", referencedColumnName = "VENDOR_NR" )
				},
				foreignKey = @ForeignKey( name = "FK_STUDENT_CAR2", value = ConstraintMode.NO_CONSTRAINT )
		)
		public Car car2;

		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumns(
				value = {
						@JoinColumn( name = "CAR_NR3", referencedColumnName = "CAR_NR" ),
						@JoinColumn( name = "CAR_VENDOR_NR3", referencedColumnName = "VENDOR_NR" )
				},
				foreignKey = @ForeignKey( name = "FK_STUDENT_CAR3" )
		)
		public Car car3;

		@OneToOne(fetch = FetchType.LAZY)
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
		public String name;
	}

	@Embeddable
	public static class VehicleId implements Serializable {
		@jakarta.persistence.Column( name = "VEHICLE_VENDOR_NR" )
		public long vehicleVendorNumber;
		@jakarta.persistence.Column( name = "VEHICLE_NR" )
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
			int result = Long.hashCode( vehicleVendorNumber );
			result = 31 * result + Long.hashCode( vehicleNumber );
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

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn( name = "OWNER_PERSON_ID", foreignKey = @ForeignKey( name = "FK_CAR_OWNER") )
		public Person owner;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn( name = "OWNER_PERSON_ID2", foreignKey = @ForeignKey( name = "FK_CAR_OWNER2", value = ConstraintMode.NO_CONSTRAINT ) )
		public Person owner2;

		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn( name = "OWNER_PERSON_ID3", foreignKey = @ForeignKey( name = "FK_CAR_OWNER3") )
		public Person owner3;

		@OneToOne(fetch = FetchType.LAZY)
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

	@MappedSuperclass
	public static abstract class AbstractCompany {

		@Id
		@GeneratedValue
		public long id;

		@ManyToOne
		@JoinColumn( name = "OWNER_ID" )
		public Person owner;

		@ManyToOne
		@JoinColumn( name = "OWNER_ID2" )
		public Person owner2;

		@OneToOne
		@JoinColumn( name = "CC_ID" )
		public CreditCard creditCard;

		@OneToOne
		@JoinColumn( name = "CC_ID2" )
		public CreditCard creditCard2;

		@OneToMany
		@JoinColumn( name = "CC_ID3" )
		public List<CreditCard> creditCards1;

		@OneToMany
		@JoinColumn( name = "CC_ID4" )
		public List<CreditCard> creditCards2;
	}

	@Embeddable
	public static class CompanyInfo {
		public String data;

		@OneToMany
		@JoinColumn( name = "CI_CC_ID", foreignKey = @ForeignKey( name = "FK_CI_CC" ) )
		public List<CreditCard> cards;

		@OneToMany
		@JoinColumn( name = "CI_CC_ID2", foreignKey = @ForeignKey( name = "FK_CI_CC2" ) )
		public List<CreditCard> cards2;

		@ManyToOne
		@JoinColumn( name = "CI_CC_ID3", foreignKey = @ForeignKey( name = "FK_CI_CC3" ) )
		public CreditCard cards3;

		@ManyToOne
		@JoinColumn( name = "CI_CC_ID4", foreignKey = @ForeignKey( name = "FK_CI_CC4" ) )
		public CreditCard cards4;
	}

	@Entity(name = "Company")
	@AssociationOverrides({
			@AssociationOverride(
					name = "owner",
					joinColumns = @JoinColumn( name = "OWNER_PERSON_ID" ),
					foreignKey = @ForeignKey( name = "FK_COMPANY_OWNER" )
			),
			@AssociationOverride(
					name = "owner2",
					joinColumns = @JoinColumn( name = "OWNER_PERSON_ID2" ),
					foreignKey = @ForeignKey( name = "FK_COMPANY_OWNER2", value = ConstraintMode.NO_CONSTRAINT )
			),
			@AssociationOverride(
					name = "creditCard",
					joinColumns = @JoinColumn( name = "CREDIT_CARD_ID" ),
					foreignKey = @ForeignKey( name = "FK_COMPANY_CREDIT_CARD" )
			),
			@AssociationOverride(
					name = "creditCard2",
					joinColumns = @JoinColumn( name = "CREDIT_CARD_ID2" ),
					foreignKey = @ForeignKey( name = "FK_COMPANY_CREDIT_CARD2", value = ConstraintMode.NO_CONSTRAINT )
			),
			@AssociationOverride(
					name = "creditCards1",
					joinColumns = @JoinColumn( name = "CREDIT_CARD_ID3" ),
					foreignKey = @ForeignKey( name = "FK_COMPANY_CREDIT_CARD3" )
			),
			@AssociationOverride(
					name = "creditCards2",
					joinColumns = @JoinColumn( name = "CREDIT_CARD_ID4" ),
					foreignKey = @ForeignKey( name = "FK_COMPANY_CREDIT_CARD4", value = ConstraintMode.NO_CONSTRAINT )
			)

	})
	public static class Company extends AbstractCompany {
		@Embedded
		@AssociationOverrides({
				@AssociationOverride(
						name = "cards",
						joinColumns = @JoinColumn( name = "AO_CI_CC_ID" ),
						foreignKey = @ForeignKey( name = "FK_COMPANY_CARD" )
				),
				@AssociationOverride(
						name = "cards2",
						joinColumns = @JoinColumn( name = "AO_CI_CC_ID2" ),
						foreignKey = @ForeignKey( name = "FK_COMPANY_CARD2", value = ConstraintMode.NO_CONSTRAINT )
				),
				@AssociationOverride(
						name = "cards3",
						joinColumns = @JoinColumn( name = "AO_CI_CC_ID3" ),
						foreignKey = @ForeignKey( name = "FK_COMPANY_CARD3" )
				),
				@AssociationOverride(
						name = "cards4",
						joinColumns = @JoinColumn( name = "AO_CI_CC_ID4" ),
						foreignKey = @ForeignKey( name = "FK_COMPANY_CARD4", value = ConstraintMode.NO_CONSTRAINT )
				)
		})
		public CompanyInfo info;
	}

	@Entity(name = "PlanItem")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public abstract class PlanItem {

		@Id
		@GeneratedValue(strategy= GenerationType.AUTO)
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Task")
	@SecondaryTable( name = "Task" )
	public class Task extends PlanItem {
		@Id
		@GeneratedValue(strategy=GenerationType.AUTO)
		private Integer id;

		@ElementCollection
		@CollectionTable(
				name = "task_properties",
				joinColumns = {
						@JoinColumn(
								name = "task_id",
								foreignKey = @ForeignKey(
										name = "FK_PROPERTIES_TASK",
										foreignKeyDefinition = "FOREIGN KEY (task_id) REFERENCES Task"
								)
						)
				}
		)
		private Set<String> properties;

		@ElementCollection
		@CollectionTable(
				name = "task_attributes",
				joinColumns = {@JoinColumn(name = "task_id")},
				foreignKey = @ForeignKey(
						name = "FK_ATTRIBUTES_TASK",
						foreignKeyDefinition = "FOREIGN KEY (task_id) REFERENCES Task"
				)
		)
		private Set<String> attributes;

		@Override
		public Integer getId() {
			return id;
		}

		@Override
		public void setId(Integer id) {
			this.id = id;
		}

		public Set<String> getProperties() {
			return properties;
		}

		public void setProperties(Set<String> properties) {
			this.properties = properties;
		}

		public Set<String> getAttributes() {
			return attributes;
		}

		public void setAttributes(Set<String> attributes) {
			this.attributes = attributes;
		}
	}
}
