/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.ide.completion.CompletionHelperTest;

import org.hibernate.tool.reveng.ide.completion.CompletionHelper;
import org.hibernate.tool.reveng.ide.completion.EntityNameReference;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author leon
 * @author koen
 */
public class TestCase {

	@Test
	public void testGetCanonicalPath() {
		List<EntityNameReference> qts = new ArrayList<>();
		qts.add(new EntityNameReference("Article", "art"));
		qts.add(new EntityNameReference("art.descriptions", "descr"));
		qts.add(new EntityNameReference("descr.name", "n"));
		assertEquals("Article/descriptions/name/locale", CompletionHelper.getCanonicalPath(qts, "n.locale"), "Invalid path");
		assertEquals("Article/descriptions", CompletionHelper.getCanonicalPath(qts, "descr"), "Invalid path");
		//
		qts.clear();
		qts.add(new EntityNameReference("com.company.Clazz", "clz"));
		qts.add(new EntityNameReference("clz.attr", "a"));
		assertEquals("com.company.Clazz/attr", CompletionHelper.getCanonicalPath(qts, "a"), "Invalid path");
		//
		qts.clear();
		qts.add(new EntityNameReference("Agga", "a"));
		assertEquals("Agga", CompletionHelper.getCanonicalPath(qts, "a"), "Invalid path");
	}

	@Test
	public void testStackOverflowInGetCanonicalPath() {
		List<EntityNameReference> qts = new ArrayList<>();
		qts.add(new EntityNameReference("Article", "art"));
		qts.add(new EntityNameReference("art.stores", "store"));
		qts.add(new EntityNameReference("store.articles", "art"));
		// This should not result in a stack overflow
		CompletionHelper.getCanonicalPath(qts, "art");
	}


}
