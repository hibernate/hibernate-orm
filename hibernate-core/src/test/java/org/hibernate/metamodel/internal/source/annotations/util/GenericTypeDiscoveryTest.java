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
package org.hibernate.metamodel.internal.source.annotations.util;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import org.junit.Test;

/**
 * @author Hardy Ferentschik
 */
public class GenericTypeDiscoveryTest extends BaseAnnotationIndexTestCase {

	@Test
	public void testGenericClassHierarchy() {
//		Set<ConfiguredClassHierarchy<EntityClass>> hierarchies = createEntityHierarchies(
//				Paper.class,
//				Stuff.class,
//				Item.class,
//				PricedStuff.class
//		);
//		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );
//
//		Iterator<EntityClass> iter = hierarchies.iterator().next().iterator();
//		ManagedTypeMetadata configuredClass = iter.next();
//		ClassInfo info = configuredClass.getClassInfo();
//		assertEquals( "wrong class", DotName.createSimple( Stuff.class.getName() ), info.name() );
//		PersistentAttribute property = configuredClass.getMappedAttribute( "value" );
//		assertEquals( Price.class, property.getJavaType() );
//
//		assertTrue( iter.hasNext() );
//		configuredClass = iter.next();
//		info = configuredClass.getClassInfo();
//		assertEquals( "wrong class", DotName.createSimple( PricedStuff.class.getName() ), info.name() );
//		assertFalse(
//				"PricedStuff should not mapped properties", configuredClass.getSimpleAttributes().iterator().hasNext()
//		);
//
//		assertTrue( iter.hasNext() );
//		configuredClass = iter.next();
//		info = configuredClass.getClassInfo();
//		assertEquals( "wrong class", DotName.createSimple( Item.class.getName() ), info.name() );
//		// properties are alphabetically ordered!
//		property = configuredClass.getMappedAttribute( "owner" );
//		assertEquals( SomeGuy.class, property.getJavaType() );
//		property = configuredClass.getMappedAttribute( "type" );
//		assertEquals( PaperType.class, property.getJavaType() );
//
//		assertTrue( iter.hasNext() );
//		configuredClass = iter.next();
//		info = configuredClass.getClassInfo();
//		assertEquals( "wrong class", DotName.createSimple( Paper.class.getName() ), info.name() );
//		assertFalse( "Paper should not mapped properties", configuredClass.getSimpleAttributes().iterator().hasNext() );
//
//		assertFalse( iter.hasNext() );
	}

	@Test
	public void testUnresolvedType() {
//		Set<ConfiguredClassHierarchy<EntityClass>> hierarchies = createEntityHierarchies( UnresolvedType.class );
//		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );
	}

	@MappedSuperclass
	public class Stuff<Value> {
		private Value value;

		@ManyToOne
		public Value getValue() {
			return value;
		}

		public void setValue(Value value) {
			this.value = value;
		}
	}

	@MappedSuperclass
	public class PricedStuff extends Stuff<Price> {
	}

	@MappedSuperclass
	public class Item<Type, Owner> extends PricedStuff {
		private Integer id;
		private String name;
		private Type type;
		private Owner owner;

		@Id
		@GeneratedValue
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@ManyToOne
		public Type getType() {
			return type;
		}

		public void setType(Type type) {
			this.type = type;
		}

		@ManyToOne
		public Owner getOwner() {
			return owner;
		}

		public void setOwner(Owner owner) {
			this.owner = owner;
		}
	}

	@Entity
	public class Paper extends Item<PaperType, SomeGuy> {
	}

	@Entity
	public class PaperType {
		private Integer id;
		private String name;

		@Id
		@GeneratedValue
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	@Entity
	public class Price {
		private Integer id;
		private Double amount;
		private String currency;

		@Id
		@GeneratedValue
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Double getAmount() {
			return amount;
		}

		public void setAmount(Double amount) {
			this.amount = amount;
		}

		public String getCurrency() {
			return currency;
		}

		public void setCurrency(String currency) {
			this.currency = currency;
		}
	}

	@Entity
	public class SomeGuy {
		private Integer id;

		@Id
		@GeneratedValue
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity
	public class UnresolvedType<T> {

		private Integer id;
		private T state;

		@Id
		@GeneratedValue
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		//@Type(type = "org.hibernate.test.annotations.generics.StateType")
		public T getState() {
			return state;
		}

		public void setState(T state) {
			this.state = state;
		}
	}
}