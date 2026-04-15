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
package org.hibernate.tool.jdbc2cfg.CompositeId;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.*;
import org.hibernate.tool.internal.metadata.NativeMetadataDescriptor;
import org.hibernate.tool.internal.reveng.models.exporter.entity.EntityExporter;
import org.hibernate.tool.test.utils.HibernateUtil;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.test.utils.HibernateUtil;
import org.hibernate.tool.test.utils.JavaUtil;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	private MetadataDescriptor metadataDescriptor = null;
	private RevengStrategy reverseEngineeringStrategy = null;
	
	@TempDir
	public File outputDir = new File("output");
	
	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		reverseEngineeringStrategy = new DefaultStrategy();
		metadataDescriptor = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(reverseEngineeringStrategy, null);
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
    public void testMultiColumnForeignKeys() {
		Metadata metadata = metadataDescriptor.createMetadata();
        Table table = HibernateUtil.getTable(
        		metadata, 
        		JdbcUtil.toIdentifier(this, "LINE_ITEM") );
        assertNotNull(table);
        ForeignKey foreignKey = HibernateUtil.getForeignKey(
        		table, 
        		JdbcUtil.toIdentifier(this, "TO_CUSTOMER_ORDER") );     
        assertNotNull(foreignKey);                
        assertEquals(
        		reverseEngineeringStrategy.tableToClassName(
        				TableIdentifier.create(
        						null, 
        						null, 
        						JdbcUtil.toIdentifier(this, "CUSTOMER_ORDER"))),
        		foreignKey.getReferencedEntityName() );
        assertEquals(
        		JdbcUtil.toIdentifier(this, "LINE_ITEM"), 
        		foreignKey.getTable().getName() );       
        assertEquals(2,foreignKey.getColumnSpan() );
        assertEquals("CUSTOMER_ID_REF", foreignKey.getColumn(0).getName());
        assertEquals("ORDER_NUMBER", foreignKey.getColumn(1).getName());
        Table tab = HibernateUtil.getTable(
        		metadata, 
        		JdbcUtil.toIdentifier(this, "CUSTOMER_ORDER"));
        assertEquals("CUSTOMER_ID", tab.getPrimaryKey().getColumn(0).getName());
        assertEquals("ORDER_NUMBER", tab.getPrimaryKey().getColumn(1).getName());
        PersistentClass lineMapping = metadata.getEntityBinding(
        		reverseEngineeringStrategy.tableToClassName(
        				TableIdentifier.create(
        						null, 
        						null, 
        						JdbcUtil.toIdentifier(this, "LINE_ITEM"))));       
        assertEquals(4,lineMapping.getIdentifier().getColumnSpan() );
        Iterator<Column> columnIterator = lineMapping.getIdentifier().getColumns().iterator();
        assertEquals("CUSTOMER_ID_REF", columnIterator.next().getName());
        assertEquals("EXTRA_PROD_ID", columnIterator.next().getName());
        assertEquals("ORDER_NUMBER", columnIterator.next().getName());
     }
     
	@Test
    public void testPossibleKeyManyToOne() {
         PersistentClass product = metadataDescriptor.createMetadata().getEntityBinding( 
         		reverseEngineeringStrategy.tableToClassName(
        				TableIdentifier.create(
        						null, 
        						null, 
        						JdbcUtil.toIdentifier(this, "CUSTOMER_ORDER"))));         
         Property identifierProperty = product.getIdentifierProperty();
        assertInstanceOf(Component.class, identifierProperty.getValue());
         Component cmpid = (Component) identifierProperty.getValue();        
         assertEquals(2, cmpid.getPropertySpan() );         
         Iterator<?> iter = cmpid.getProperties().iterator();
         Property id = (Property) iter.next();
         Property extraId = (Property) iter.next();         
 		 assertEquals(
				reverseEngineeringStrategy.columnToPropertyName(
						null, 
						"CUSTOMER_ID"), 
				id.getName() );
         assertEquals(
 				reverseEngineeringStrategy.columnToPropertyName(
						null, 
						"ORDER_NUMBER"), 
        		 extraId.getName() );         
         assertFalse(id.getValue() instanceof ManyToOne);
         assertFalse(extraId.getValue() instanceof ManyToOne);
     }
     
	@Test
    public void testKeyProperty() {
        PersistentClass product = metadataDescriptor.createMetadata().getEntityBinding( 
         		reverseEngineeringStrategy.tableToClassName(
        				TableIdentifier.create(
        						null, 
        						null, 
        						JdbcUtil.toIdentifier(this, "PRODUCT"))));                 
        Property identifierProperty = product.getIdentifierProperty();
        assertInstanceOf(Component.class, identifierProperty.getValue());
        Component cmpid = (Component) identifierProperty.getValue();        
        assertEquals(2, cmpid.getPropertySpan() );       
        Iterator<?> iter = cmpid.getProperties().iterator();
		Property id = (Property)iter.next();
		Property extraId = (Property)iter.next();
		if ("extraId".equals(id.getName())) {
			Property temp = id;
			id = extraId;
			extraId = temp;
		}
        assertEquals(
				reverseEngineeringStrategy.columnToPropertyName(
						null, 
						"PRODUCT_ID"), 
        		id.getName() );
        assertEquals(
				reverseEngineeringStrategy.columnToPropertyName(
						null, 
						"EXTRA_ID"), 
        		extraId.getName() );        
        assertFalse(id.getValue() instanceof ManyToOne);
        assertFalse(extraId.getValue() instanceof ManyToOne);
    }
     
    @Test
    public void testGeneration() throws Exception {
        EntityExporter.create(metadataDescriptor, true).exportAll(outputDir);
        List<String> paths = new java.util.ArrayList<>();
        paths.add(JavaUtil.resolvePathToJarFileFor(jakarta.persistence.Persistence.class));
        paths.add(JavaUtil.resolvePathToJarFileFor(org.hibernate.Version.class));
        JavaUtil.compile(outputDir, paths);
        URL[] urls = new URL[] { outputDir.toURI().toURL() };
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
        URLClassLoader ucl = new URLClassLoader(urls, oldLoader);
        try {
            Thread.currentThread().setContextClassLoader(ucl);
            org.hibernate.boot.registry.StandardServiceRegistryBuilder builder =
                    new org.hibernate.boot.registry.StandardServiceRegistryBuilder();
            org.hibernate.service.ServiceRegistry serviceRegistry = builder.build();
            NativeMetadataDescriptor mds = new NativeMetadataDescriptor(null, null, null);
            for (File f : outputDir.listFiles()) {
                if (f.getName().endsWith(".class")) {
                    String className = f.getName().replace(".class", "");
                    HibernateUtil.addAnnotatedClass(mds, ucl.loadClass(className));
                }
            }
            new org.hibernate.tool.hbm2ddl.SchemaValidator()
                    .validate(mds.createMetadata(), serviceRegistry);
            Class<?> productIdClass = ucl.loadClass("ProductId");
            Constructor<?> productIdClassConstructor = productIdClass.getConstructor();
            Object object = productIdClassConstructor.newInstance();
            int hash = -1;
            try {
                hash = object.hashCode();
            } catch (Throwable t) {
                fail("Hashcode on new instance should not fail " + t);
            }
            assertNotEquals(hash, System.identityHashCode(object), "hashcode should be different from system");
        } finally {
            Thread.currentThread().setContextClassLoader(oldLoader);
        }
    }
	 
}
     

