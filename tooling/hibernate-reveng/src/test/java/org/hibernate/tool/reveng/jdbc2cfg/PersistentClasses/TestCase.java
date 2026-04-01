/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.jdbc2cfg.PersistentClasses;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Set;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.api.core.RevengSettings;
import org.hibernate.tool.reveng.internal.core.strategy.AbstractStrategy;
import org.hibernate.tool.reveng.internal.core.strategy.DefaultStrategy;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	private static final String PACKAGE_NAME = "org.hibernate.tool.reveng.jdbc2cfg.PersistentClasses";

	private Metadata metadata = null;

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		AbstractStrategy c = new DefaultStrategy();
		c.setSettings(new RevengSettings(c).setDefaultPackageName(PACKAGE_NAME));
		metadata = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(c, null)
				.createMetadata();
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testCreatePersistentClasses() {
		PersistentClass classMapping = metadata.getEntityBinding(PACKAGE_NAME + ".Orders");
		assertNotNull(classMapping, "class not found");
		KeyValue identifier = classMapping.getIdentifier();
		assertNotNull(identifier);
	}

	@Test
	public void testCreateManyToOne() {
		PersistentClass classMapping = metadata.getEntityBinding(PACKAGE_NAME + ".Item");
		assertNotNull(classMapping);
		KeyValue identifier = classMapping.getIdentifier();
		assertNotNull(identifier);
		assertEquals(3,classMapping.getPropertyClosureSpan() );
		Property property = classMapping.getProperty("ordersByRelatedOrderId");
		assertNotNull(property);
		property = classMapping.getProperty("ordersByOrderId");
		assertNotNull(property);
	}

	@Test
	public void testCreateOneToMany() {
		PersistentClass orders = metadata.getEntityBinding(PACKAGE_NAME + ".Orders");
		Property itemset = orders.getProperty("itemsForRelatedOrderId");
		Collection col = (Collection) itemset.getValue();
		OneToMany otm = (OneToMany) col.getElement();
		assertEquals(PACKAGE_NAME + ".Item", otm.getReferencedEntityName());
		assertEquals(PACKAGE_NAME + ".Item", otm.getAssociatedClass().getClassName());
		assertEquals("ORDERS", otm.getTable().getName());
		assertNotNull(itemset);
		assertInstanceOf(Set.class, itemset.getValue());
	}

	@Test
	public void testBinding() throws HibernateException {

		String schemaToUse = Environment
				.getProperties()
				.getProperty(AvailableSettings.DEFAULT_SCHEMA);
		PersistentClass orders = metadata.getEntityBinding(PACKAGE_NAME + ".Orders");
		orders.getTable().setSchema(schemaToUse);
		PersistentClass items = metadata.getEntityBinding(PACKAGE_NAME + ".Item");
		items.getTable().setSchema(schemaToUse);

		SessionFactory sf = metadata.buildSessionFactory();
		Session session = sf.openSession();
		Transaction t = session.beginTransaction();

		Orders order = new Orders();
		order.setId(1);
		order.setName("Mickey");

		session.merge(order);

		Item item = addItem(order, 42, "item 42");
		session.merge(item);
		session.merge(addItem(order, 43, "x") );
		session.merge(addItem(order, 44, "y") );
		session.merge(addItem(order, 45, "z") );
		session.merge(addItem(order, 46, "w") );

		t.commit();
		session.close();

		session = sf.openSession();
		t = session.beginTransaction();

		Item loadeditem = (Item) session.find(PACKAGE_NAME + ".Item", 42 );

		assertEquals(item.getName(),loadeditem.getName() );
		assertEquals(item.getChildId(),loadeditem.getChildId() );
		assertEquals(item.getOrderId().getId(),loadeditem.getOrderId().getId() );

		assertTrue(loadeditem.getOrderId().getItemsForOrderId().contains(loadeditem) );
		assertTrue(item.getOrderId().getItemsForOrderId().contains(item) );

		assertEquals(5,item.getOrderId().getItemsForOrderId().size() );
		assertEquals(5,loadeditem.getOrderId().getItemsForOrderId().size() );

		t.commit();
		session.close();

		session = sf.openSession();
		t = session.beginTransaction();

		order = session.getReference(Orders.class, 1);
		assertFalse(Hibernate.isInitialized(order) );
		assertFalse(Hibernate.isInitialized(order.getItemsForOrderId() ) );

		order = (Orders) session.createQuery("from " + PACKAGE_NAME + ".Orders", null).getSingleResult();

		assertFalse(Hibernate.isInitialized(order.getItemsForOrderId() ) );
		t.commit();
		session.close();
		sf.close();
	}

	private Item addItem(Orders m, int itemid, String name) {
		Item item = new Item();
		item.setChildId(itemid);
		item.setOrderId(m);
		item.setName(name);
		item.setOrdersByRelatedOrderId(m);
		m.getItemsForOrderId().add(item);
		return item;
	}

}
