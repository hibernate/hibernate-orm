/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.ide.completion;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CompletionHelperTest {

	@Test
	public void testGetCanonicalPathSingleEntityWithAlias() {
		List<EntityNameReference> refs = List.of(new EntityNameReference("Product", "p"));
		assertEquals("Product", CompletionHelper.getCanonicalPath(refs, "p"));
	}

	@Test
	public void testGetCanonicalPathSingleEntityWithProperty() {
		List<EntityNameReference> refs = List.of(new EntityNameReference("Product", "Product"));
		assertEquals("Product/name", CompletionHelper.getCanonicalPath(refs, "name"));
	}

	@Test
	public void testGetCanonicalPathMultipleEntities() {
		List<EntityNameReference> refs = List.of(
				new EntityNameReference("Product", "p"),
				new EntityNameReference("Category", "c")
		);
		assertEquals("Product", CompletionHelper.getCanonicalPath(refs, "p"));
		assertEquals("Category", CompletionHelper.getCanonicalPath(refs, "c"));
	}

	@Test
	public void testGetCanonicalPathDotSeparated() {
		List<EntityNameReference> refs = List.of(new EntityNameReference("Product", "p"));
		String result = CompletionHelper.getCanonicalPath(refs, "p.name");
		assertEquals("Product/name", result);
	}

	@Test
	public void testGetCanonicalPathUnknownAlias() {
		List<EntityNameReference> refs = List.of(new EntityNameReference("Product", "p"));
		assertEquals("unknown", CompletionHelper.getCanonicalPath(refs, "unknown"));
	}

	@Test
	public void testGetCanonicalPathEmptyList() {
		assertEquals("p", CompletionHelper.getCanonicalPath(new ArrayList<>(), "p"));
	}
}
