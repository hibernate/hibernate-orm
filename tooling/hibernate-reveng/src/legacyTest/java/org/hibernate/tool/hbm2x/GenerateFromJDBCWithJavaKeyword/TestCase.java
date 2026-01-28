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
package org.hibernate.tool.hbm2x.GenerateFromJDBCWithJavaKeyword;

import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.internal.reveng.strategy.AbstractStrategy;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.internal.reveng.strategy.OverrideRepository;
import org.hibernate.tool.test.utils.JavaUtil;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author koen@hibernate.org
 */
public class TestCase {

    @TempDir
	public File outputDir = new File("output");
	
	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
	}
	
	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}
	
	@Test
	public void testGenerateJava() throws Exception {	
		Exporter exporter = ExporterFactory.createExporter(ExporterType.JAVA);	
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, createMetadataDescriptor());
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
 		exporter.start();
		File myReturn = new File(outputDir, "org/reveng/MyReturn.java");
		assertTrue(myReturn.exists());
		File myReturnHistory = new File(outputDir, "org/reveng/MyReturnHistory.java");
		assertTrue(myReturnHistory.exists());
		JavaUtil.compile(outputDir);
		URLClassLoader loader = new URLClassLoader(new URL[] { outputDir.toURI().toURL() } );
		Class<?> returnClass = loader.loadClass("org.reveng.MyReturn");
		assertNotNull(returnClass);
		Class<?> returnHistoryClass = loader.loadClass("org.reveng.MyReturnHistory");
		assertNotNull(returnHistoryClass);
		Field returnField = returnHistoryClass.getDeclaredField("return_");
		assertNotNull(returnField);
		Method returnSetter = returnHistoryClass.getMethod("setReturn", returnClass);
		assertNotNull(returnSetter);
		loader.close();
	}
	
	private MetadataDescriptor createMetadataDescriptor() {
		AbstractStrategy configurableNamingStrategy = new DefaultStrategy();
		configurableNamingStrategy.setSettings(new RevengSettings(configurableNamingStrategy).setDefaultPackageName("org.reveng").setCreateCollectionForForeignKey(false));
		OverrideRepository overrideRepository = new OverrideRepository();
        String REVENG_XML = """
                <hibernate-reverse-engineering>                                                   \s
                   <table name='MY_RETURN_HISTORY'>                                               \s
                      <foreign-key                                                                \s
                            constraint-name='FK_MY_RETURN_HISTORY_RETURN_ID'                      \s
                            foreign-table='MY_RETURN'>                                            \s
                          <column-ref local-column='MY_RETURN_REF' foreign-column='RETURN_ID'/>   \s
                          <many-to-one property='return'/>                                        \s
                      </foreign-key>                                                              \s
                   </table>                                                                       \s
                </hibernate-reverse-engineering>                                                  \s""";
        InputStream inputStream = new ByteArrayInputStream(REVENG_XML.getBytes());
		overrideRepository.addInputStream(inputStream);
		RevengStrategy res = overrideRepository
				.getReverseEngineeringStrategy(configurableNamingStrategy);
		return MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(res, null);
	}
	
}
