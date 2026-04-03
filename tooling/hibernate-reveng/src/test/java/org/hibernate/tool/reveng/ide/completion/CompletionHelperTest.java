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
	public void testSingleEntityMatchAlias() {
		List<EntityNameReference> refs = new ArrayList<>();
		refs.add(new EntityNameReference("Person", "p"));
		assertEquals("Person", CompletionHelper.getCanonicalPath(refs, "p"));
	}

	@Test
	public void testSingleEntityPropertyPath() {
		List<EntityNameReference> refs = new ArrayList<>();
		refs.add(new EntityNameReference("Person", "p"));
		// alias "p" is not null/empty/same as entity name, so falls through to general resolution
		// "name" is not a known alias, no dots, so returns as-is
		assertEquals("name", CompletionHelper.getCanonicalPath(refs, "name"));
	}

	@Test
	public void testSingleEntityNoAlias() {
		List<EntityNameReference> refs = new ArrayList<>();
		refs.add(new EntityNameReference("Person", null));
		assertEquals("Person/name", CompletionHelper.getCanonicalPath(refs, "name"));
	}

	@Test
	public void testSingleEntitySameAlias() {
		List<EntityNameReference> refs = new ArrayList<>();
		refs.add(new EntityNameReference("Person", "Person"));
		assertEquals("Person/name", CompletionHelper.getCanonicalPath(refs, "name"));
	}

	@Test
	public void testMultipleEntities() {
		List<EntityNameReference> refs = new ArrayList<>();
		refs.add(new EntityNameReference("Person", "p"));
		refs.add(new EntityNameReference("Address", "a"));
		// With multiple entities, falls through to general resolution
		assertEquals("Person", CompletionHelper.getCanonicalPath(refs, "p"));
	}

	@Test
	public void testDottedProperty() {
		List<EntityNameReference> refs = new ArrayList<>();
		refs.add(new EntityNameReference("Person", "p"));
		refs.add(new EntityNameReference("Address", "a"));
		assertEquals("Address/city", CompletionHelper.getCanonicalPath(refs, "a.city"));
	}

	@Test
	public void testUnknownName() {
		List<EntityNameReference> refs = new ArrayList<>();
		refs.add(new EntityNameReference("Person", "p"));
		assertEquals("unknown", CompletionHelper.getCanonicalPath(refs, "unknown"));
	}
}
