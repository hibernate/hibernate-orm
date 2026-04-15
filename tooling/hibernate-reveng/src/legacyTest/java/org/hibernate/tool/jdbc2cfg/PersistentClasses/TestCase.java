/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.jdbc2cfg.PersistentClasses;

import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.*;
import org.hibernate.boot.Metadata;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.internal.metadata.RevengMetadataDescriptor;
import org.hibernate.tool.internal.reveng.strategy.AbstractStrategy;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	private static final String PACKAGE_NAME = "org.hibernate.tool.jdbc2cfg.PersistentClasses";

	private MetadataDescriptor metadataDescriptor = null;
	private List<ClassDetails> entities = null;

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
        AbstractStrategy c = new DefaultStrategy();
        c.setSettings(new RevengSettings(c).setDefaultPackageName(PACKAGE_NAME));
        metadataDescriptor = MetadataDescriptorFactory
        		.createReverseEngineeringDescriptor(c, null);
        entities = ((RevengMetadataDescriptor) metadataDescriptor).getEntityClassDetails();
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testCreatePersistentClasses() {
		ClassDetails orders = findByClassName(PACKAGE_NAME + ".Orders");
		assertNotNull(orders, "class not found");
		// Check it has an @Id field
		boolean hasId = orders.getFields().stream()
				.anyMatch(f -> f.getDirectAnnotationUsage(Id.class) != null);
		assertTrue(hasId, "Orders should have an @Id field");
	}

	@Test
	public void testCreateManyToOne() {
		ClassDetails item = findByClassName(PACKAGE_NAME + ".Item");
		assertNotNull(item);
		List<FieldDetails> manyToOneFields = item.getFields().stream()
				.filter(f -> f.getDirectAnnotationUsage(ManyToOne.class) != null)
				.toList();
		assertFalse(manyToOneFields.isEmpty(), "Item should have ManyToOne fields");
		// Check for the two ManyToOne fields referencing Orders
		boolean hasOrdersByRelatedOrderId = manyToOneFields.stream()
				.anyMatch(f -> f.getName().equals("ordersByRelatedOrderId"));
		boolean hasOrdersByOrderId = manyToOneFields.stream()
				.anyMatch(f -> f.getName().equals("ordersByOrderId"));
		assertTrue(hasOrdersByRelatedOrderId, "Should have ordersByRelatedOrderId field");
		assertTrue(hasOrdersByOrderId, "Should have ordersByOrderId field");
	}

	@Test
	public void testCreateOneToMany() {
		ClassDetails orders = findByClassName(PACKAGE_NAME + ".Orders");
		assertNotNull(orders);
		List<FieldDetails> oneToManyFields = orders.getFields().stream()
				.filter(f -> f.getDirectAnnotationUsage(OneToMany.class) != null)
				.toList();
		assertFalse(oneToManyFields.isEmpty(), "Orders should have OneToMany fields");
		boolean hasItemsForRelatedOrderId = oneToManyFields.stream()
				.anyMatch(f -> f.getName().equals("itemsForRelatedOrderId"));
		assertTrue(hasItemsForRelatedOrderId, "Should have itemsForRelatedOrderId field");
		// Check the OneToMany field has the correct annotation
		FieldDetails itemSet = oneToManyFields.stream()
				.filter(f -> f.getName().equals("itemsForRelatedOrderId"))
				.findFirst().orElseThrow();
		OneToMany oneToMany = itemSet.getDirectAnnotationUsage(OneToMany.class);
		assertNotNull(oneToMany);
	}

	@Test
	public void testBinding() throws HibernateException {

		String schemaToUse = Environment
				.getProperties()
				.getProperty(AvailableSettings.DEFAULT_SCHEMA);
		Metadata metadata = metadataDescriptor.createMetadata();
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

		Item loadeditem = (Item) session.find(PACKAGE_NAME + ".Item", 42);

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

        order = (Orders) session.createQuery("from " + PACKAGE_NAME + ".Orders", (Class<?>)null).getSingleResult();

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

	private ClassDetails findByClassName(String className) {
		for (ClassDetails cd : entities) {
			if (className.equals(cd.getName())) {
				return cd;
			}
		}
		return null;
	}

}
