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
import javax.persistence.AssociationOverrides;
import javax.persistence.CollectionTable;
import javax.persistence.ConstraintMode;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
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
import javax.persistence.MappedSuperclass;
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
				Truck.class,
				Company.class,
				PlanItem.class,
				Task.class
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
	public void testMapForeignKeyJoinColumnColection() {
		assertForeignKey( "FK_PROPERTIES_TASK", "task_id" );
	}

	@Test
	public void testMapForeignKeyColection() {
		assertForeignKey( "FK_ATTRIBUTES_TASK", "task_id" );
	}

	@Test
	public void testAssociationOverride() {
		// class level association overrides
		assertForeignKey( "FK_COMPANY_OWNER", "OWNER_PERSON_ID" );
		assertForeignKey( "FK_COMPANY_CREDIT_CARD", "CREDIT_CARD_ID" );
		assertForeignKey( "FK_COMPANY_CREDIT_CARD3", "CREDIT_CARD_ID3" );
		assertNoForeignKey( "FK_COMPANY_OWNER2", "OWNER_PERSON_ID2" );
		assertNoForeignKey( "FK_COMPANY_CREDIT_CARD2", "CREDIT_CARD_ID2" );
		assertNoForeignKey( "FK_COMPANY_CREDIT_CARD4", "CREDIT_CARD_ID4" );

		// embeddable association overrides
		assertForeignKey( "FK_COMPANY_CARD", "AO_CI_CC_ID" );
		assertNoForeignKey( "FK_COMPANY_CARD2", "AO_CI_CC_ID2" );
		assertForeignKey( "FK_COMPANY_CARD3", "AO_CI_CC_ID3" );
		assertNoForeignKey( "FK_COMPANY_CARD4", "AO_CI_CC_ID4" );
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

	@Entity
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

	@Entity
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
