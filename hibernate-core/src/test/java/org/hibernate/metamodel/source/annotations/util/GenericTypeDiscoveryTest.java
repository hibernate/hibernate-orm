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
package org.hibernate.metamodel.source.annotations.util;

import java.util.Iterator;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.metamodel.source.annotations.entity.ConfiguredClass;
import org.hibernate.metamodel.source.annotations.entity.ConfiguredClassHierarchy;
import org.hibernate.metamodel.source.annotations.entity.MappedAttribute;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.internal.BasicServiceRegistryImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class GenericTypeDiscoveryTest extends BaseUnitTestCase {
	private BasicServiceRegistryImpl serviceRegistry;
	private ClassLoaderService service;

	@Before
	public void setUp() {
		serviceRegistry = (BasicServiceRegistryImpl) new ServiceRegistryBuilder().buildServiceRegistry();
		service = serviceRegistry.getService( ClassLoaderService.class );
	}

	@After
	public void tearDown() {
		serviceRegistry.destroy();
	}

	@Test
	public void testSingleEntity() {
		Index index = JandexHelper.indexForClass( service, Paper.class, Stuff.class, Item.class, PricedStuff.class );
		Set<ConfiguredClassHierarchy> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
				index, serviceRegistry
		);
		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );

		Iterator<ConfiguredClass> iter = hierarchies.iterator().next().iterator();
		ConfiguredClass configuredClass = iter.next();
		ClassInfo info = configuredClass.getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( Stuff.class.getName() ), info.name() );
		MappedAttribute property = configuredClass.getMappedProperty( "value" );
		assertEquals( Price.class.getName(), property.getType() );

		assertTrue( iter.hasNext() );
		configuredClass = iter.next();
		info = configuredClass.getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( PricedStuff.class.getName() ), info.name() );
		assertFalse(
				"PricedStuff should not mapped properties", configuredClass.getMappedAttributes().iterator().hasNext()
		);

		assertTrue( iter.hasNext() );
		configuredClass = iter.next();
		info = configuredClass.getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( Item.class.getName() ), info.name() );
		// properties are alphabetically ordered!
		property = configuredClass.getMappedProperty( "owner" );
		assertEquals( SomeGuy.class.getName(), property.getType() );
		property = configuredClass.getMappedProperty( "type" );
		assertEquals( PaperType.class.getName(), property.getType() );

		assertTrue( iter.hasNext() );
		configuredClass = iter.next();
		info = configuredClass.getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( Paper.class.getName() ), info.name() );
		assertFalse( "Paper should not mapped properties", configuredClass.getMappedAttributes().iterator().hasNext() );

		assertFalse( iter.hasNext() );
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
}