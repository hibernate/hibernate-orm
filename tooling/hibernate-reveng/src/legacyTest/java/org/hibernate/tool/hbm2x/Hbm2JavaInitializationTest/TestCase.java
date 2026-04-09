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

package org.hibernate.tool.hbm2x.Hbm2JavaInitializationTest;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.tool.internal.export.java.Cfg2JavaTool;
import org.hibernate.tool.internal.export.java.ImportContextImpl;
import org.hibernate.tool.internal.export.java.POJOClass;
import org.hibernate.tool.test.utils.HibernateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	private static final String[] HBM_XML_FILES = new String[] {
			"Author.hbm.xml",
			"Article.hbm.xml",
			"Train.hbm.xml",
			"Passenger.hbm.xml"
	};
	
	@TempDir
	public File outputFolder = new File("output");
	
	private Metadata metadata = null;
	
	@BeforeEach
	public void setUp() throws Exception {
		metadata = HibernateUtil
				.initializeMetadataDescriptor(
						this, 
						HBM_XML_FILES, 
						outputFolder)
				.createMetadata();
	}
	
	@Test
	public void testFieldInitializationAndTypeNames() {
		PersistentClass classMapping = metadata.getEntityBinding("org.hibernate.tool.hbm2x.Hbm2JavaInitializationTest.Article");
		Cfg2JavaTool cfg2java = new Cfg2JavaTool();
		POJOClass clazz = cfg2java.getPOJOClass(classMapping);
		Property p = classMapping.getProperty("AMap");
		assertEquals("java.util.Map<java.lang.String,org.hibernate.tool.hbm2x.Hbm2JavaInitializationTest.Article>",cfg2java.getJavaTypeName(p, true), "all types should be fully qualified when no importcontext");
		assertEquals("Map<String,Article>",cfg2java.getJavaTypeName(p, true, clazz));		
		assertEquals("new HashMap<String,Article>(0)", clazz.getFieldInitialization(p, true));
		assertEquals("new HashMap(0)", clazz.getFieldInitialization(p, false));
		p = classMapping.getProperty("aList");
		assertEquals("List<Article>",cfg2java.getJavaTypeName(p, true, clazz), "lists should not have the index visible in the declaration");
		assertEquals("java.util.List<org.hibernate.tool.hbm2x.Hbm2JavaInitializationTest.Article>",cfg2java.getJavaTypeName(p, true), "all types should be fully qualified when no importcontext");
		assertEquals("new ArrayList<Article>(0)", clazz.getFieldInitialization(p, true));
		assertEquals("new ArrayList(0)", clazz.getFieldInitialization(p, false));
		p = classMapping.getProperty("content");
		assertEquals("\"what can I say\"",clazz.getFieldInitialization(p, false));
		p = classMapping.getProperty("bagarticles");
		assertEquals("java.util.List", cfg2java.getJavaTypeName( p, false ), "Should be a list via property-type");
		assertEquals("java.util.List<org.hibernate.tool.hbm2x.Hbm2JavaInitializationTest.Article>", cfg2java.getJavaTypeName( p, true ), "Should be a a generic'd list when generics=true");
		assertEquals("List<Article>",cfg2java.getJavaTypeName(p, true, clazz));		
		assertEquals("new ArrayList<Article>(0)", clazz.getFieldInitialization(p, true));
		assertEquals("new ArrayList(0)", clazz.getFieldInitialization(p, false));
		p = classMapping.getProperty("bagstrings");
		assertEquals("java.util.Collection", cfg2java.getJavaTypeName( p, false ), "Bag's are just a collection");
		assertEquals("java.util.Collection<java.lang.String>", cfg2java.getJavaTypeName( p, true ), "Should be a a generic'd collection when generics=true");
		assertEquals("Collection<String>",cfg2java.getJavaTypeName(p, true, clazz));		
		assertEquals("new ArrayList<String>(0)", clazz.getFieldInitialization(p, true));
		assertEquals("new ArrayList(0)", clazz.getFieldInitialization(p, false));
		p = classMapping.getProperty("bagstrings");
		assertEquals("new ArrayList(0)", clazz.getFieldInitialization(p, false));
		p = classMapping.getProperty("naturalSortedArticlesMap");
		assertEquals("java.util.SortedMap", cfg2java.getJavaTypeName( p, false));
		assertEquals("SortedMap<String,Article>", cfg2java.getJavaTypeName( p, true, new ImportContextImpl("") ));
		assertEquals("new TreeMap<String,Article>()", clazz.getFieldInitialization(p, true));		
		assertEquals("new TreeMap()", clazz.getFieldInitialization(p, false));
		p = classMapping.getProperty("sortedArticlesMap");
		assertEquals("java.util.SortedMap", cfg2java.getJavaTypeName( p, false));
		assertEquals("SortedMap<String,Article>", cfg2java.getJavaTypeName( p, true, new ImportContextImpl("") ));
		assertFalse(clazz.generateImports().contains("import comparator.NoopComparator;"));
		assertEquals("new TreeMap(new NoopComparator())", clazz.getFieldInitialization(p, false));
		assertTrue(clazz.generateImports().contains("import comparator.NoopComparator;"));
		assertEquals("new TreeMap<String,Article>(new NoopComparator())", clazz.getFieldInitialization(p, true));
		p = classMapping.getProperty("sortedArticlesSet");
		assertEquals("java.util.SortedSet", cfg2java.getJavaTypeName( p, false));
		assertEquals("SortedSet<Article>", cfg2java.getJavaTypeName( p, true, new ImportContextImpl("") ));
		assertEquals("new TreeSet<Article>(new NoopComparator())", clazz.getFieldInitialization(p, true));
	}
	
}
