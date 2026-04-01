/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.metadata;

import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RevengMetadataDescriptorTest {

	@Test
	public void testConstructorWithNullProperties() {
		RevengMetadataDescriptor descriptor = new RevengMetadataDescriptor(null, null);
		Properties result = descriptor.getProperties();
		assertNotNull(result);
	}

	@Test
	public void testConstructorWithProperties() {
		Properties props = new Properties();
		props.put("custom.key", "custom.value");
		RevengMetadataDescriptor descriptor = new RevengMetadataDescriptor(null, props);
		Properties result = descriptor.getProperties();
		assertNotNull(result);
		assertNotNull(result.get("custom.key"));
	}

	@Test
	public void testGetPropertiesReturnsCopy() {
		RevengMetadataDescriptor descriptor = new RevengMetadataDescriptor(null, null);
		Properties result1 = descriptor.getProperties();
		Properties result2 = descriptor.getProperties();
		result1.put("extra.key", "value");
		assertFalse(result2.containsKey("extra.key"));
	}

	@Test
	public void testFactoryCreateReverseEngineeringDescriptor() {
		MetadataDescriptor descriptor = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null);
		assertNotNull(descriptor);
		assertNotNull(descriptor.getProperties());
	}

	@Test
	public void testFactoryCreateNativeDescriptor() {
		Properties props = new Properties();
		props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
		props.put("hibernate.connection.driver_class", "org.h2.Driver");
		props.put("hibernate.connection.url", "jdbc:h2:mem:factory_test");
		props.put("hibernate.connection.username", "sa");
		props.put("hibernate.connection.password", "");
		props.put("hibernate.default_schema", "");
		props.put("hibernate.default_catalog", "");
		MetadataDescriptor descriptor = MetadataDescriptorFactory
				.createNativeDescriptor(null, null, props);
		assertNotNull(descriptor);
		assertNotNull(descriptor.getProperties());
	}
}
