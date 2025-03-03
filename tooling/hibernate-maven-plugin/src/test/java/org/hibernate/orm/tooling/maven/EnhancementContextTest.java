/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.net.URLClassLoader;

import org.junit.jupiter.api.Test;

public class EnhancementContextTest {

	@Test
	void testGetClassLoader() {
		ClassLoader testLoader = new URLClassLoader(new URL[0]);
		EnhancementContext context = new EnhancementContext(null, false, false, false, false);
		assertNull(context.getLoadingClassLoader());
		context = new EnhancementContext(testLoader, false, false, false, false);
		assertSame(testLoader, context.getLoadingClassLoader());
	}

	@Test
	void testDoBiDirectionalAssociationManagement() {
		EnhancementContext context = new EnhancementContext(null, false, false, false, false);
		assertFalse(context.doBiDirectionalAssociationManagement(null));
		context = new EnhancementContext(null, true, false, false, false);
		assertTrue(context.doBiDirectionalAssociationManagement(null));
	}

	@Test
	void testDoDirtyCheckingInline() {
		EnhancementContext context = new EnhancementContext(null, false, false, false, false);
		assertFalse(context.doDirtyCheckingInline(null));
		context = new EnhancementContext(null, false, true, false, false);
		assertTrue(context.doDirtyCheckingInline(null));
	}

	@Test
	void testHasLazyLoadableAttributes() {
		EnhancementContext context = new EnhancementContext(null, false, false, false, false);
		assertFalse(context.hasLazyLoadableAttributes(null));
		context = new EnhancementContext(null, false, false, true, false);
		assertTrue(context.hasLazyLoadableAttributes(null));
	}

	@Test
	void testIsLazyLoadable() {
		EnhancementContext context = new EnhancementContext(null, false, false, false, false);
		assertFalse(context.isLazyLoadable(null));
		context = new EnhancementContext(null, false, false, true, false);
		assertTrue(context.isLazyLoadable(null));
	}

	@Test
	void testDoExtendedEnhancement() {
		EnhancementContext context = new EnhancementContext(null, false, false, false, false);
		assertFalse(context.doExtendedEnhancement(null));
		context = new EnhancementContext(null, false, false, false, true);
		assertTrue(context.doExtendedEnhancement(null));
	}

}
