/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.java;

import org.hibernate.mapping.MetaAttributable;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.Property;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BasicPOJOClassTest {

	/**
	 * Simple MetaAttributable implementation for testing.
	 */
	private static class SimpleMetaAttributable implements MetaAttributable {
		private Map<String, MetaAttribute> metaAttributes = new HashMap<>();

		@Override
		public MetaAttribute getMetaAttribute(String name) {
			return metaAttributes.get(name);
		}

		@Override
		public Map<String, MetaAttribute> getMetaAttributes() {
			return metaAttributes;
		}

		@Override
		public void setMetaAttributes(Map<String, MetaAttribute> metas) {
			this.metaAttributes = metas;
		}
	}

	/**
	 * Minimal concrete subclass for testing BasicPOJOClass's non-abstract methods.
	 */
	private static class TestablePOJOClass extends BasicPOJOClass {

		private final String mappedClassName;

		TestablePOJOClass(String mappedClassName, Cfg2JavaTool c2j) {
			super(new SimpleMetaAttributable(), c2j);
			this.mappedClassName = mappedClassName;
			init();
		}

		TestablePOJOClass(MetaAttributable ma, String mappedClassName, Cfg2JavaTool c2j) {
			super(ma, c2j);
			this.mappedClassName = mappedClassName;
			init();
		}

		@Override
		protected String getMappedClassName() {
			return mappedClassName;
		}

		@Override
		public Iterator<Property> getAllPropertiesIterator() {
			return Collections.emptyIterator();
		}

		@Override public String getExtends() { return null; }
		@Override public String getImplements() { return null; }
		@Override public boolean isComponent() { return false; }
		@Override public boolean hasIdentifierProperty() { return false; }
		@Override public String generateAnnIdGenerator() { return ""; }
		@Override public String generateAnnTableUniqueConstraint() { return ""; }
		@Override public Object getDecoratedObject() { return null; }
		@Override public boolean isSubclass() { return false; }
		@Override public List<Property> getPropertiesForFullConstructor() { return List.of(); }
		@Override public List<Property> getPropertyClosureForFullConstructor() { return List.of(); }
		@Override public List<Property> getPropertyClosureForSuperclassFullConstructor() { return List.of(); }
		@Override public List<Property> getPropertiesForMinimalConstructor() { return List.of(); }
		@Override public List<Property> getPropertyClosureForMinimalConstructor() { return List.of(); }
		@Override public List<Property> getPropertyClosureForSuperclassMinimalConstructor() { return List.of(); }
		@Override public POJOClass getSuperClass() { return null; }
		@Override public Property getIdentifierProperty() { return null; }
		@Override public boolean hasVersionProperty() { return false; }
		@Override public Property getVersionProperty() { return null; }
	}

	// --- beanCapitalize tests ---

	@Test
	public void testBeanCapitalizeNormal() {
		assertEquals("Name", BasicPOJOClass.beanCapitalize("name"));
	}

	@Test
	public void testBeanCapitalizeAlreadyCapitalized() {
		assertEquals("Name", BasicPOJOClass.beanCapitalize("Name"));
	}

	@Test
	public void testBeanCapitalizeSecondCharUpper() {
		// JavaBeans spec: if second char is uppercase, return unchanged
		assertEquals("nAme", BasicPOJOClass.beanCapitalize("nAme"));
	}

	@Test
	public void testBeanCapitalizeNull() {
		assertEquals(null, BasicPOJOClass.beanCapitalize(null));
	}

	@Test
	public void testBeanCapitalizeEmpty() {
		assertEquals("", BasicPOJOClass.beanCapitalize(""));
	}

	@Test
	public void testBeanCapitalizeSingleChar() {
		assertEquals("A", BasicPOJOClass.beanCapitalize("a"));
	}

	// --- Package and class name tests ---

	@Test
	public void testGetPackageName() {
		TestablePOJOClass pojo = new TestablePOJOClass("com.example.Person", new Cfg2JavaTool());
		assertEquals("com.example", pojo.getPackageName());
	}

	@Test
	public void testGetPackageNameNoPackage() {
		TestablePOJOClass pojo = new TestablePOJOClass("Person", new Cfg2JavaTool());
		assertEquals("", pojo.getPackageName());
	}

	@Test
	public void testGetPackageDeclaration() {
		TestablePOJOClass pojo = new TestablePOJOClass("com.example.Person", new Cfg2JavaTool());
		assertEquals("package com.example;", pojo.getPackageDeclaration());
	}

	@Test
	public void testGetPackageDeclarationNoPackage() {
		TestablePOJOClass pojo = new TestablePOJOClass("Person", new Cfg2JavaTool());
		assertEquals("// default package", pojo.getPackageDeclaration());
	}

	@Test
	public void testGetDeclarationName() {
		TestablePOJOClass pojo = new TestablePOJOClass("com.example.Person", new Cfg2JavaTool());
		assertEquals("Person", pojo.getDeclarationName());
	}

	@Test
	public void testGetShortName() {
		TestablePOJOClass pojo = new TestablePOJOClass("com.example.Person", new Cfg2JavaTool());
		assertEquals("Person", pojo.getShortName());
	}

	@Test
	public void testGetQualifiedDeclarationName() {
		TestablePOJOClass pojo = new TestablePOJOClass("com.example.Person", new Cfg2JavaTool());
		// qualifier of mappedClassName (com.example) + generatedClassName (com.example.Person)
		assertEquals("com.example.com.example.Person", pojo.getQualifiedDeclarationName());
	}

	@Test
	public void testGetQualifiedDeclarationNameSimple() {
		TestablePOJOClass pojo = new TestablePOJOClass("Person", new Cfg2JavaTool());
		assertEquals("Person", pojo.getQualifiedDeclarationName());
	}

	// --- Inner class handling ---

	@Test
	public void testInnerClassDollarReplacedWithDot() {
		TestablePOJOClass pojo = new TestablePOJOClass("com.example.Outer$Inner", new Cfg2JavaTool());
		assertEquals("Outer.Inner", pojo.getDeclarationName());
	}

	// --- Extends / Implements declarations ---

	@Test
	public void testGetExtendsDeclarationNull() {
		TestablePOJOClass pojo = new TestablePOJOClass("Person", new Cfg2JavaTool());
		assertEquals("", pojo.getExtendsDeclaration());
	}

	@Test
	public void testGetImplementsDeclarationNull() {
		TestablePOJOClass pojo = new TestablePOJOClass("Person", new Cfg2JavaTool());
		assertEquals("", pojo.getImplementsDeclaration());
	}

	// --- Declaration type and modifiers ---

	@Test
	public void testGetDeclarationType() {
		TestablePOJOClass pojo = new TestablePOJOClass("Person", new Cfg2JavaTool());
		assertEquals("class", pojo.getDeclarationType());
	}

	@Test
	public void testGetClassModifiersDefault() {
		TestablePOJOClass pojo = new TestablePOJOClass("Person", new Cfg2JavaTool());
		assertEquals("public", pojo.getClassModifiers());
	}

	// --- Needs constructors ---

	@Test
	public void testNeedsFullConstructorEmpty() {
		TestablePOJOClass pojo = new TestablePOJOClass("Person", new Cfg2JavaTool());
		assertFalse(pojo.needsFullConstructor());
	}

	@Test
	public void testNeedsMinimalConstructorEmpty() {
		TestablePOJOClass pojo = new TestablePOJOClass("Person", new Cfg2JavaTool());
		assertFalse(pojo.needsMinimalConstructor());
	}

	// --- Equals / HashCode / ToString ---

	@Test
	public void testNeedsEqualsHashCodeEmpty() {
		TestablePOJOClass pojo = new TestablePOJOClass("Person", new Cfg2JavaTool());
		assertFalse(pojo.needsEqualsHashCode());
	}

	@Test
	public void testNeedsToStringEmpty() {
		TestablePOJOClass pojo = new TestablePOJOClass("Person", new Cfg2JavaTool());
		assertFalse(pojo.needsToString());
	}

	@Test
	public void testGenerateEqualsNoProperties() {
		TestablePOJOClass pojo = new TestablePOJOClass("Person", new Cfg2JavaTool());
		assertEquals("false", pojo.generateEquals("this", "other", false));
	}

	// --- Import context ---

	@Test
	public void testImportType() {
		TestablePOJOClass pojo = new TestablePOJOClass("com.example.Person", new Cfg2JavaTool());
		String result = pojo.importType("java.util.List");
		assertEquals("List", result);
	}

	@Test
	public void testGenerateImports() {
		TestablePOJOClass pojo = new TestablePOJOClass("com.example.Person", new Cfg2JavaTool());
		pojo.importType("java.util.List");
		String imports = pojo.generateImports();
		assertTrue(imports.contains("import java.util.List"));
	}

	// --- Meta attribute methods ---

	@Test
	public void testHasMetaAttributeFalse() {
		TestablePOJOClass pojo = new TestablePOJOClass("Person", new Cfg2JavaTool());
		assertFalse(pojo.hasMetaAttribute("nonexistent"));
	}

	@Test
	public void testGetExtraClassCodeNull() {
		TestablePOJOClass pojo = new TestablePOJOClass("Person", new Cfg2JavaTool());
		// No "class-code" meta attribute set, so getMetaAsString returns null
		String code = pojo.getExtraClassCode();
		// MetaAttributeHelper.getMetaAsString returns null when MetaAttribute is null
		assertTrue(code == null || code.isEmpty());
	}

	// --- Constructor validation ---

	@Test
	public void testConstructorNullMeta() {
		assertThrows(IllegalArgumentException.class,
				() -> new TestablePOJOClass(null, "Person", new Cfg2JavaTool()));
	}

	// --- Field modifiers with Property ---

	@Test
	public void testGetFieldModifiers() {
		TestablePOJOClass pojo = new TestablePOJOClass("Person", new Cfg2JavaTool());
		Property p = new Property();
		p.setName("name");
		assertEquals("private", pojo.getFieldModifiers(p));
	}

	@Test
	public void testGetPropertyGetModifiers() {
		TestablePOJOClass pojo = new TestablePOJOClass("Person", new Cfg2JavaTool());
		Property p = new Property();
		p.setName("name");
		assertEquals("public", pojo.getPropertyGetModifiers(p));
	}

	@Test
	public void testGetPropertySetModifiers() {
		TestablePOJOClass pojo = new TestablePOJOClass("Person", new Cfg2JavaTool());
		Property p = new Property();
		p.setName("name");
		assertEquals("public", pojo.getPropertySetModifiers(p));
	}

	@Test
	public void testGetFieldModifiersWithOverride() {
		TestablePOJOClass pojo = new TestablePOJOClass("Person", new Cfg2JavaTool());
		Property p = new Property();
		p.setName("name");
		MetaAttribute scopeField = new MetaAttribute("scope-field");
		scopeField.addValue("protected");
		p.setMetaAttributes(java.util.Map.of("scope-field", scopeField));
		assertEquals("protected", pojo.getFieldModifiers(p));
	}

	@Test
	public void testHasFieldJavaDoc() {
		TestablePOJOClass pojo = new TestablePOJOClass("Person", new Cfg2JavaTool());
		Property p = new Property();
		p.setName("name");
		assertFalse(pojo.hasFieldJavaDoc(p));
	}

	@Test
	public void testGetFieldDescription() {
		TestablePOJOClass pojo = new TestablePOJOClass("Person", new Cfg2JavaTool());
		Property p = new Property();
		p.setName("name");
		assertEquals("", pojo.getFieldDescription(p));
	}

	@Test
	public void testGetPropertyName() {
		TestablePOJOClass pojo = new TestablePOJOClass("Person", new Cfg2JavaTool());
		Property p = new Property();
		p.setName("firstName");
		assertEquals("FirstName", pojo.getPropertyName(p));
	}
}
