/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AnnotationBuilderTest {

	@Test
	public void testSimpleAnnotation() {
		String result = AnnotationBuilder.createAnnotation("Entity").getResult();
		assertEquals("@Entity", result);
	}

	@Test
	public void testAnnotationWithSingleAttribute() {
		String result = AnnotationBuilder.createAnnotation("Table")
				.addAttribute("name", "\"employees\"")
				.getResult();
		assertEquals("@Table(name=\"employees\")", result);
	}

	@Test
	public void testAnnotationWithMultipleAttributes() {
		String result = AnnotationBuilder.createAnnotation("Column")
				.addAttribute("name", "\"emp_name\"")
				.addAttribute("nullable", "false")
				.getResult();
		assertEquals("@Column(name=\"emp_name\", nullable=false)", result);
	}

	@Test
	public void testAnnotationWithArrayAttribute() {
		String result = AnnotationBuilder.createAnnotation("NamedQueries")
				.addAttribute("value", new String[]{"@NamedQuery(name=\"q1\")", "@NamedQuery(name=\"q2\")"})
				.getResult();
		assertEquals("@NamedQueries(value={@NamedQuery(name=\"q1\"), @NamedQuery(name=\"q2\")})", result);
	}

	@Test
	public void testAddQuotedAttribute() {
		String result = AnnotationBuilder.createAnnotation("Table")
				.addQuotedAttribute("name", "employees")
				.getResult();
		assertEquals("@Table(name=\"employees\")", result);
	}

	@Test
	public void testAddQuotedAttributeNull() {
		String result = AnnotationBuilder.createAnnotation("Table")
				.addQuotedAttribute("name", null)
				.getResult();
		assertEquals("@Table", result);
	}

	@Test
	public void testAddAttributeNullValue() {
		String result = AnnotationBuilder.createAnnotation("Table")
				.addAttribute("name", (String) null)
				.getResult();
		assertEquals("@Table", result);
	}

	@Test
	public void testAddAttributeEmptyArray() {
		String result = AnnotationBuilder.createAnnotation("Table")
				.addAttribute("name", new String[]{})
				.getResult();
		assertEquals("@Table", result);
	}

	@Test
	public void testResetAnnotation() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("Entity")
				.addAttribute("name", "\"Foo\"");
		builder.resetAnnotation("Table");
		assertEquals("@Table", builder.getResult());
	}

	@Test
	public void testToString() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("Entity");
		assertEquals("@Entity", builder.toString());
	}

	@Test
	public void testGetAttributeAsString() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("Column")
				.addAttribute("name", "\"id\"");
		assertEquals("\"id\"", builder.getAttributeAsString("name"));
	}

	@Test
	public void testGetAttributeAsStringNull() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("Column");
		assertNull(builder.getAttributeAsString("name"));
	}

	@Test
	public void testGetAttributeAsStringArray() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("Test")
				.addAttribute("value", new String[]{"a", "b"});
		assertEquals("{a, b}", builder.getAttributeAsString("value"));
	}

	@Test
	public void testAddQuotedAttributes() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("UniqueConstraint");
		builder.addQuotedAttributes("columnNames", List.of("col1", "col2").iterator());
		String result = builder.getResult();
		assertEquals("@UniqueConstraint(columnNames={\"col1\", \"col2\"})", result);
	}

	@Test
	public void testAddAttributes() {
		AnnotationBuilder builder = AnnotationBuilder.createAnnotation("Test");
		builder.addAttributes("value", List.of("a", "b").iterator());
		String result = builder.getResult();
		assertEquals("@Test(value={a, b})", result);
	}
}
