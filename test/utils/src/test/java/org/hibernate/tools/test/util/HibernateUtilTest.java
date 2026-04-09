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
package org.hibernate.tools.test.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.File;
import java.util.Collections;
import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

public class HibernateUtilTest {
	
	@TempDir
	public File outputFolder = new File("output");
	
	@Test
	public void testGetForeignKey() {
		Table table = new Table("Hibernate Tools");
		assertNull(HibernateUtil.getForeignKey(table, "foo"));
		assertNull(HibernateUtil.getForeignKey(table, "bar"));
		table.createForeignKey("foo", Collections.emptyList(), "Bar", null);
		assertNotNull(HibernateUtil.getForeignKey(table, "foo"));
		assertNull(HibernateUtil.getForeignKey(table, "bar"));
	}
	
	@Test
	public void testDialectInstantiation() {
		assertNotNull(new HibernateUtil.Dialect());
	}
	
	@Test
	public void testConnectionProviderInstantiation() {
		assertNotNull(new HibernateUtil.ConnectionProvider());
	}
	
	@Test
	public void testInitializeConfiguration() {
		Metadata metadata = HibernateUtil
				.initializeMetadataDescriptor(
						this, 
						new String[] { "HelloWorld.hbm.xml" },
						outputFolder)
				.createMetadata();
		assertSame(
				HibernateUtil.Dialect.class, 
				metadata.getDatabase().getDialect().getClass());
		assertNotNull(metadata.getEntityBinding("HelloWorld"));
	}
	
	@Test
	public void testAddAnnotatedClass() {
		Properties properties = new Properties();
		properties.setProperty(AvailableSettings.DIALECT, HibernateUtil.Dialect.class.getName());
		properties.setProperty(AvailableSettings.CONNECTION_PROVIDER, HibernateUtil.ConnectionProvider.class.getName());
		MetadataDescriptor metadataDescriptor = MetadataDescriptorFactory
				.createNativeDescriptor(null, null, properties);
		assertNull(metadataDescriptor
				.createMetadata()
				.getEntityBinding(
						"org.hibernate.tools.test.util.HibernateUtilTest$Dummy"));
		HibernateUtil.addAnnotatedClass(metadataDescriptor, Dummy.class);
		assertNotNull(metadataDescriptor
				.createMetadata()
				.getEntityBinding(
						"org.hibernate.tools.test.util.HibernateUtilTest$Dummy"));
	}
	
	@Entity
	private class Dummy {
		@Id public int id;
	}

}
