/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.jdbc2cfg.PersistentClasses;

import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.api.reveng.RevengSettings;
import org.hibernate.tool.reveng.internal.metadata.RevengMetadataDescriptor;
import org.hibernate.tool.reveng.internal.strategy.AbstractStrategy;
import org.hibernate.tool.reveng.internal.strategy.DefaultStrategy;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
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

	private static final String PACKAGE_NAME = "org.hibernate.tool.reveng.jdbc2cfg.PersistentClasses";

	private List<ClassDetails> entities = null;

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		AbstractStrategy c = new DefaultStrategy();
		c.setSettings(new RevengSettings(c).setDefaultPackageName(PACKAGE_NAME));
		entities = ((RevengMetadataDescriptor) MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(c, null))
				.getEntityClassDetails();
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

	private ClassDetails findByClassName(String className) {
		for (ClassDetails cd : entities) {
			if (className.equals(cd.getName())) {
				return cd;
			}
		}
		return null;
	}

}
