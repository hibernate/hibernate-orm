/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.annotations.entity;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.junit.Test;

import org.hibernate.annotations.Parent;
import org.hibernate.annotations.Target;
import org.hibernate.metamodel.binding.BasicAttributeBinding;
import org.hibernate.metamodel.binding.ComponentAttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Tests for {@code javax.persistence.Embeddable}.
 *
 * @author Hardy Ferentschik
 */
public class EmbeddableBindingTest extends BaseAnnotationBindingTestCase {
	@Entity
	class User {
		@Id
		private int id;

		@Embedded
		private Phone phone;
	}

	@Embeddable
	class Phone {
		String countryCode;
		String areaCode;
		String number;
	}

	@Test
	@Resources(annotatedClasses = { User.class, Phone.class })
	public void testEmbeddable() {
		EntityBinding binding = getEntityBinding( User.class );

		final String componentName = "phone";
		assertNotNull( binding.locateAttributeBinding( componentName ) );
		assertTrue( binding.locateAttributeBinding( componentName ) instanceof ComponentAttributeBinding );
		ComponentAttributeBinding componentBinding = (ComponentAttributeBinding) binding.locateAttributeBinding(
				componentName
		);

		// todo - is this really correct? Does the path start w/ the class name
		assertEquals(
				"Wrong path",
				"org.hibernate.metamodel.source.annotations.entity.EmbeddableBindingTest$User.phone",
				componentBinding.getPathBase()
		);

		assertNotNull( componentBinding.locateAttributeBinding( "countryCode" ) );
		assertNotNull( componentBinding.locateAttributeBinding( "areaCode" ) );
		assertNotNull( componentBinding.locateAttributeBinding( "number" ) );
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Entity
	@AttributeOverride(name = "embedded.name", column = @Column(name = "FUBAR", length = 42))
	class BaseEntity {
		@Id
		private int id;

		@Embedded
		private EmbeddedEntity embedded;
	}

	@Embeddable
	class EmbeddedEntity {
		String name;
	}

	@Test
	@Resources(annotatedClasses = { BaseEntity.class, EmbeddedEntity.class })
	public void testEmbeddableWithAttributeOverride() {
		EntityBinding binding = getEntityBinding( BaseEntity.class );

		final String componentName = "embedded";
		assertNotNull( binding.locateAttributeBinding( componentName ) );
		assertTrue( binding.locateAttributeBinding( componentName ) instanceof ComponentAttributeBinding );
		ComponentAttributeBinding componentBinding = (ComponentAttributeBinding) binding.locateAttributeBinding(
				componentName
		);

		assertNotNull( componentBinding.locateAttributeBinding( "name" ) );
		BasicAttributeBinding nameAttribute = (BasicAttributeBinding) componentBinding.locateAttributeBinding( "name" );
		org.hibernate.metamodel.relational.Column column = (org.hibernate.metamodel.relational.Column) nameAttribute.getValue();
		assertEquals( "Attribute override specifies a custom column name", "FUBAR", column.getColumnName().getName() );
		assertEquals( "Attribute override specifies a custom size", 42, column.getSize().getLength() );
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Embeddable
	public class Address {
		protected String street;
		protected String city;
		protected String state;
		@Embedded
		protected Zipcode zipcode;
	}

	@Embeddable
	public class Zipcode {
		protected String zip;
		protected String plusFour;
	}

	@Entity
	public class Customer {
		@Id
		protected Integer id;
		protected String name;
		@AttributeOverrides( {
				@AttributeOverride(name = "state",
						column = @Column(name = "ADDR_STATE")),
				@AttributeOverride(name = "zipcode.zip",
						column = @Column(name = "ADDR_ZIP"))
		})
		@Embedded
		protected Address address;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test
	@Resources(annotatedClasses = { Zipcode.class, Address.class, Customer.class })
	public void testNestedEmbeddable() {
		EntityBinding binding = getEntityBinding( Customer.class );

		final String addressComponentName = "address";
		assertNotNull( binding.locateAttributeBinding( addressComponentName ) );
		assertTrue( binding.locateAttributeBinding( addressComponentName ) instanceof ComponentAttributeBinding );
		ComponentAttributeBinding attributeComponentBinding = (ComponentAttributeBinding) binding.locateAttributeBinding(
				addressComponentName
		);

		assertNotNull( attributeComponentBinding.locateAttributeBinding( "street" ) );
		assertNotNull( attributeComponentBinding.locateAttributeBinding( "city" ) );
		assertNotNull( attributeComponentBinding.locateAttributeBinding( "state" ) );

		BasicAttributeBinding stateAttribute = (BasicAttributeBinding) attributeComponentBinding.locateAttributeBinding(
				"state"
		);
		org.hibernate.metamodel.relational.Column column = (org.hibernate.metamodel.relational.Column) stateAttribute.getValue();
		assertEquals(
				"Attribute override specifies a custom column name",
				"ADDR_STATE",
				column.getColumnName().getName()
		);


		final String zipComponentName = "zipcode";
		assertNotNull( attributeComponentBinding.locateAttributeBinding( zipComponentName ) );
		assertTrue( attributeComponentBinding.locateAttributeBinding( zipComponentName ) instanceof ComponentAttributeBinding );
		ComponentAttributeBinding zipComponentBinding = (ComponentAttributeBinding) attributeComponentBinding.locateAttributeBinding(
				zipComponentName
		);

		BasicAttributeBinding nameAttribute = (BasicAttributeBinding) zipComponentBinding.locateAttributeBinding( "zip" );
		column = (org.hibernate.metamodel.relational.Column) nameAttribute.getValue();
		assertEquals(
				"Attribute override specifies a custom column name",
				"ADDR_ZIP",
				column.getColumnName().getName()
		);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Embeddable
	public class A {
		@Embedded
		@AttributeOverrides( {
				@AttributeOverride(name = "foo", column = @Column(name = "BAR")),
				@AttributeOverride(name = "fubar", column = @Column(name = "A_WINS"))
		})
		private B b;

		public B getB() {
			return b;
		}
	}

	@Embeddable
	public class B {
		private String foo;
		private String fubar;

		public String getFoo() {
			return foo;
		}

		public String getFubar() {
			return fubar;
		}
	}

	@Entity
	public class C {
		@Id
		int id;

		@Embedded
		@AttributeOverride(name = "b.fubar", column = @Column(name = "C_WINS"))
		protected A a;

		public int getId() {
			return id;
		}

		public A getA() {
			return a;
		}
	}

	@Test
	@Resources(annotatedClasses = { A.class, B.class, C.class })
	public void testAttributeOverrideInEmbeddable() {
		EntityBinding binding = getEntityBinding( C.class );

		final String aComponentName = "a";
		assertNotNull( binding.locateAttributeBinding( aComponentName ) );
		assertTrue( binding.locateAttributeBinding( aComponentName ) instanceof ComponentAttributeBinding );
		ComponentAttributeBinding aComponentBinding = (ComponentAttributeBinding) binding.locateAttributeBinding(
				aComponentName
		);

		final String bComponentName = "b";
		assertNotNull( aComponentBinding.locateAttributeBinding( bComponentName ) );
		assertTrue( aComponentBinding.locateAttributeBinding( bComponentName ) instanceof ComponentAttributeBinding );
		ComponentAttributeBinding bComponentBinding = (ComponentAttributeBinding) aComponentBinding.locateAttributeBinding(
				bComponentName
		);

		BasicAttributeBinding attribute = (BasicAttributeBinding) bComponentBinding.locateAttributeBinding( "foo" );
		org.hibernate.metamodel.relational.Column column = (org.hibernate.metamodel.relational.Column) attribute.getValue();
		assertEquals(
				"Attribute override specifies a custom column name",
				"BAR",
				column.getColumnName().getName()
		);

		attribute = (BasicAttributeBinding) bComponentBinding.locateAttributeBinding( "fubar" );
		column = (org.hibernate.metamodel.relational.Column) attribute.getValue();
		assertEquals(
				"Attribute override specifies a custom column name",
				"C_WINS",
				column.getColumnName().getName()
		);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Embeddable
	public class EmbeddableEntity {
		private String test;
		@Parent
		private MainEntity parent;
	}

	@Entity
	public class MainEntity {
		@Id
		private int id;

		@Embedded
		private EmbeddableEntity embedded;
	}

	@Test
	@Resources(annotatedClasses = { MainEntity.class, EmbeddableEntity.class })
	public void testParentReferencingAttributeName() {
		EntityBinding binding = getEntityBinding( MainEntity.class );

		final String componentName = "embedded";
		assertNotNull( binding.locateAttributeBinding( componentName ) );
		assertTrue( binding.locateAttributeBinding( componentName ) instanceof ComponentAttributeBinding );
		ComponentAttributeBinding componentBinding = (ComponentAttributeBinding) binding.locateAttributeBinding(
				componentName
		);

		assertEquals( "Wrong parent reference name", "parent", componentBinding.getParentReference().getName() );
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public interface Car {
		int getHorsePower();
	}

	@Embeddable
	public class CarImpl implements Car {
		@Override
		public int getHorsePower() {
			return 0;
		}
	}

	@Entity
	public class Owner {
		private int id;
		private Car car;

		@Id
		public int getId() {
			return id;
		}

		@Embedded
		@Target(CarImpl.class)
		public Car getCar() {
			return car;
		}
	}

	@Test
	@Resources(annotatedClasses = { Owner.class, CarImpl.class, Car.class })
	public void testTargetAnnotationWithEmbeddable() {
		EntityBinding binding = getEntityBinding( Owner.class );

		final String componentName = "car";
		assertNotNull( binding.locateAttributeBinding( componentName ) );
		assertTrue( binding.locateAttributeBinding( componentName ) instanceof ComponentAttributeBinding );
		ComponentAttributeBinding componentBinding = (ComponentAttributeBinding) binding.locateAttributeBinding(
				componentName
		);

		BasicAttributeBinding attribute = (BasicAttributeBinding) componentBinding.locateAttributeBinding( "horsePower" );
		assertTrue( attribute.getAttribute().isTypeResolved() );
		assertEquals(
				"Wrong resolved type",
				"int",
				attribute.getAttribute().getSingularAttributeType().getClassName()
		);
	}
}


