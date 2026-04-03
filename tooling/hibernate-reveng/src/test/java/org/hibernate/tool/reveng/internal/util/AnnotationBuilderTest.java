/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AnnotationBuilderTest {

	@Test
	public void testCreateAnnotationNoAttributes() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("Entity");
		assertEquals("@Entity", builder.getResult());
	}

	@Test
	public void testCreateAnnotationSingleAttribute() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("Table");
		builder.addAttribute("name", "\"employees\"");
		assertEquals("@Table(name=\"employees\")", builder.getResult());
	}

	@Test
	public void testCreateAnnotationMultipleAttributes() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("Column");
		builder.addAttribute("name", "\"first_name\"");
		builder.addAttribute("nullable", "false");
		assertEquals("@Column(name=\"first_name\", nullable=false)", builder.getResult());
	}

	@Test
	public void testCreateAnnotationArrayAttribute() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("NamedQueries");
		builder.addAttribute("value", new String[] {"@NamedQuery(name=\"q1\")", "@NamedQuery(name=\"q2\")"});
		assertEquals("@NamedQueries(value={@NamedQuery(name=\"q1\"), @NamedQuery(name=\"q2\")})", builder.getResult());
	}

	@Test
	public void testAddQuotedAttribute() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("Table");
		builder.addQuotedAttribute("name", "employees");
		assertEquals("@Table(name=\"employees\")", builder.getResult());
	}

	@Test
	public void testAddAttributeNullValueIgnored() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("Entity");
		builder.addAttribute("name", (String) null);
		assertEquals("@Entity", builder.getResult());
	}

	@Test
	public void testAddAttributeNullArrayIgnored() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("Entity");
		builder.addAttribute("value", (String[]) null);
		assertEquals("@Entity", builder.getResult());
	}

	@Test
	public void testAddAttributeEmptyArrayIgnored() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("Entity");
		builder.addAttribute("value", new String[] {});
		assertEquals("@Entity", builder.getResult());
	}

	@Test
	public void testAddQuotedAttributeNullIgnored() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("Entity");
		builder.addQuotedAttribute("name", null);
		assertEquals("@Entity", builder.getResult());
	}

	@Test
	public void testResetAnnotation() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("OldName");
		builder.addAttribute("key", "value");
		builder.resetAnnotation("NewName");
		assertEquals("@NewName", builder.getResult());
	}

	@Test
	public void testGetAttributeAsString() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("Test");
		builder.addAttribute("name", "\"hello\"");
		assertEquals("\"hello\"", builder.getAttributeAsString("name"));
	}

	@Test
	public void testGetAttributeAsStringArray() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("Test");
		builder.addAttribute("values", new String[] {"a", "b", "c"});
		assertEquals("{a, b, c}", builder.getAttributeAsString("values"));
	}

	@Test
	public void testGetAttributeAsStringNotFound() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("Test");
		assertNull(builder.getAttributeAsString("nonexistent"));
	}

	@Test
	public void testToStringEqualsGetResult() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("Entity");
		assertEquals(builder.getResult(), builder.toString());
	}

	@Test
	public void testAddQuotedAttributes() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("Test");
		builder.addQuotedAttributes("names", Arrays.asList("a", "b").iterator());
		assertEquals("@Test(names={\"a\", \"b\"})", builder.getResult());
	}

	@Test
	public void testAddAttributes() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("Test");
		builder.addAttributes("values", Arrays.asList("X.class", "Y.class").iterator());
		assertEquals("@Test(values={X.class, Y.class})", builder.getResult());
	}

	@Test
	public void testAddQuotedAttributesEmpty() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("Test");
		builder.addQuotedAttributes("names", Collections.emptyIterator());
		assertEquals("@Test", builder.getResult());
	}

	@Test
	public void testFluentApi() {
		String result = AnnotationBuilder.createAnnotation("Column")
				.addQuotedAttribute("name", "id")
				.addAttribute("nullable", "false")
				.getResult();
		assertEquals("@Column(name=\"id\", nullable=false)", result);
	}
}
