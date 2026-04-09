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

package org.hibernate.tool.hbm2x.Hbm2JavaEqualsTest;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.test.utils.ConnectionProvider;
import org.hibernate.tool.test.utils.HibernateUtil;
import org.hibernate.tool.test.utils.JavaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class TestCase {
	
	private static final String TEST_ENTITY_HBM_XML = 
            "<hibernate-mapping package='org.hibernate.tool.hbm2x.Hbm2JavaEquals'>"+
            "  <class name='UnProxiedTestEntity'>                                 "+
            "    <id name='id' type='int'>                                        "+
		    "      <meta attribute='use-in-equals'>true</meta>                    "+
		    "    </id>                                                            "+
	        "  </class>                                                           "+
	        "  <class name='ProxiedTestEntity' proxy='TestEntityProxy'>           "+
		    "    <id name='id' type='int'>                                        "+
		    "      <meta attribute='use-in-equals'>true</meta>                    "+
		    "    </id>                                                            "+
	        "  </class>                                                           "+
            "</hibernate-mapping>                                                 ";	
	
	private static final String TEST_ENTITY_PROXY_JAVA = 
			"package org.hibernate.tool.hbm2x.Hbm2JavaEquals;"+ System.lineSeparator() +
	        "interface TestEntityProxy {                     "+ System.lineSeparator() +
			"  int getId();                                  "+ System.lineSeparator() +
	        "}                                               ";
	
	@TempDir
	public File outputFolder = new File("output");
	
	private File srcDir = null;

    @BeforeEach
	public void setUp() throws Exception {
		// create output folder
		srcDir = new File(outputFolder, "output");
		assertTrue(srcDir.mkdir());
        File resourcesDir = new File(outputFolder, "resources");
		assertTrue(resourcesDir.mkdir());
		// export class ProxiedTestEntity.java and UnProxiedTestEntity
		File hbmXml = new File(resourcesDir, "testEntity.hbm.xml");
		FileWriter fileWriter = new FileWriter(hbmXml);
		fileWriter.write(TEST_ENTITY_HBM_XML);
		fileWriter.close();
		Properties properties = new Properties();
		properties.put(AvailableSettings.DIALECT, HibernateUtil.Dialect.class.getName());
		properties.put(AvailableSettings.CONNECTION_PROVIDER, ConnectionProvider.class.getName());
		MetadataDescriptor metadataDescriptor = MetadataDescriptorFactory
				.createNativeDescriptor(null, new File[] { hbmXml }, properties);
		Exporter exporter = ExporterFactory.createExporter(ExporterType.JAVA);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, srcDir);
		exporter.start();
		// copy interface EntityProxy.java
		File file = new File(srcDir, "org/hibernate/tool/hbm2x/Hbm2JavaEquals/TestEntityProxy.java");
		FileWriter writer = new FileWriter(file);
		writer.write(TEST_ENTITY_PROXY_JAVA);
		writer.close();
		// compile the source files
		JavaUtil.compile(srcDir);
	}	
	
	@Test
	public void testEqualsWithoutProxy() throws Exception {
		// load the entity class and lookup the setId method
        URL[] urls = new URL[] { srcDir.toURI().toURL() };
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		URLClassLoader ucl = new URLClassLoader(urls, oldLoader );
        Class<?> entityClass = ucl.loadClass("org.hibernate.tool.hbm2x.Hbm2JavaEquals.UnProxiedTestEntity");
		Constructor<?> entityClassConstructor = entityClass.getConstructor();
        Method setId = entityClass.getMethod("setId", int.class);

        // create a first entity and check the 'normal' behavior: 
        // - 'true' when comparing against itself
        // - 'false' when comparing against null
        // - 'false' when comparing against an object of a different class
        Object firstEntity = entityClassConstructor.newInstance();
        setId.invoke(firstEntity, Integer.MAX_VALUE);
        assertEquals(firstEntity, firstEntity);
        assertNotNull(firstEntity);
        assertNotEquals(new Object(), firstEntity);

        // create a second entity and check the 'normal behavior
        // - 'true' if the id property is the same
        // - 'false' if the id property is different
        Object secondEntity = entityClassConstructor.newInstance();
        setId.invoke(secondEntity, Integer.MAX_VALUE);
        assertEquals(firstEntity, secondEntity);
        assertEquals(secondEntity, firstEntity);
        setId.invoke(secondEntity, Integer.MIN_VALUE);
        assertNotEquals(firstEntity, secondEntity);
        assertNotEquals(secondEntity, firstEntity);

        ucl.close();
	}

	@Test
	public void testEqualsWithProxy() throws Exception {

		// load the entity and proxy classes, lookup the setId method and create a proxy object
        URL[] urls = new URL[] { srcDir.toURI().toURL() };
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		URLClassLoader ucl = new URLClassLoader(urls, oldLoader );
        Class<?> entityClass = ucl.loadClass("org.hibernate.tool.hbm2x.Hbm2JavaEquals.ProxiedTestEntity");
        Class<?> entityProxyInterface = ucl.loadClass("org.hibernate.tool.hbm2x.Hbm2JavaEquals.TestEntityProxy");
		Constructor<?> entityClassConstructor = entityClass.getConstructor();
        Method setId = entityClass.getMethod("setId", int.class);
        TestEntityProxyInvocationHandler handler = new TestEntityProxyInvocationHandler();
        Object testEntityProxy = Proxy.newProxyInstance(
        		ucl, 
        		new Class[] { entityProxyInterface }, 
        		handler);
        
        // create a first proxied entity and check the 'normal' behavior: 
        // - 'true' when comparing against itself
        // - 'false' when comparing against null
        // - 'false' when comparing against an object of a different class (that is not the proxy class)
		Object firstEntity = entityClassConstructor.newInstance();
        setId.invoke(firstEntity, Integer.MAX_VALUE);
        assertEquals(firstEntity, firstEntity);
        assertNotNull(firstEntity);
        assertNotEquals(new Object(), firstEntity);

        // create a second proxied entity and check the 'normal behavior
        // - 'true' if the id property is the same
        // - 'false' if the id property is different
        Object secondEntity = entityClassConstructor.newInstance();
        setId.invoke(secondEntity, Integer.MAX_VALUE);
        assertEquals(firstEntity, secondEntity);
        assertEquals(secondEntity, firstEntity);
        setId.invoke(secondEntity, Integer.MIN_VALUE);
        assertNotEquals(firstEntity, secondEntity);
        assertNotEquals(secondEntity, firstEntity);

        // compare both proxied entities with the proxy
        handler.id = Integer.MAX_VALUE;
        assertEquals(firstEntity, testEntityProxy);
        assertNotEquals(secondEntity, testEntityProxy);
        handler.id = Integer.MIN_VALUE;
        assertNotEquals(firstEntity, testEntityProxy);
        assertEquals(secondEntity, testEntityProxy);
        
        ucl.close();
	}

	private static class TestEntityProxyInvocationHandler implements InvocationHandler {
		public int id = 0;
		@Override public Object invoke(
				Object proxy, 
				Method method, 
				Object[] args) {
			if ("getId".equals(method.getName())) {
				return id;
			}
			return null;
		}		
	}
	
}
