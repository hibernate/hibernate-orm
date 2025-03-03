/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.userguide.util;

import jakarta.persistence.spi.LoadState;

import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.jpa.internal.util.PersistenceUtilHelper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for HHH-5094 and HHH-5334
 *
 * @author Hardy Ferentschik
 */
public class PersistenceUtilHelperTest {
	private final PersistenceUtilHelper.MetadataCache cache = new PersistenceUtilHelper.MetadataCache();

	public static class FieldAccessBean extends FieldAccessBeanBase {
		protected String protectedAccessProperty;
		private String privateAccessProperty;
	}

	public static class FieldAccessBeanBase {
		public String publicAccessProperty;
	}

	public static class MethodAccessBean extends MethodAccessBeanBase {
		private String protectedAccessProperty;
		private String privateAccessProperty;

		protected String getProtectedAccessPropertyValue() {
			return protectedAccessProperty;
		}

		private String getPrivateAccessPropertyValue() {
			return privateAccessProperty;
		}
	}

	public static class MethodAccessBeanBase {
		private String publicAccessProperty;

		public String getPublicAccessPropertyValue() {
			return publicAccessProperty;
		}
	}

	@Test
	public void testIsLoadedWithReferencePublicField() {
		assertEquals(
				LoadState.UNKNOWN,
				PersistenceUtilHelper.isLoadedWithReference(
						new FieldAccessBean(),
						"publicAccessProperty",
						cache
				)
		);
	}

	@Test
	public void testIsLoadedWithReferencePublicMethod() {
		assertEquals(
				LoadState.UNKNOWN,
				PersistenceUtilHelper.isLoadedWithReference(
						new MethodAccessBean(),
						"publicAccessPropertyValue",
						cache
				)
		);
	}

	@Test
	public void testIsLoadedWithReferenceProtectedField() {
		assertEquals(
				LoadState.UNKNOWN,
				PersistenceUtilHelper.isLoadedWithReference(
						new FieldAccessBean(),
						"protectedAccessProperty",
						cache
				)
		);
	}

	@Test
	public void testIsLoadedWithReferenceProtectedMethod() {
		assertEquals(
				LoadState.UNKNOWN,
				PersistenceUtilHelper.isLoadedWithReference(
						new MethodAccessBean(),
						"protectedAccessPropertyValue",
						cache
				)
		);
	}

	@Test
	public void testIsLoadedWithReferencePrivateField() {
		assertEquals(
				LoadState.UNKNOWN,
				PersistenceUtilHelper.isLoadedWithReference(
						new FieldAccessBean(),
						"privateAccessProperty",
						cache
				)
		);
	}

	@Test
	public void testIsLoadedWithReferencePrivateMethod() {
		assertEquals(
				LoadState.UNKNOWN,
				PersistenceUtilHelper.isLoadedWithReference(
						new MethodAccessBean(),
						"privateAccessPropertyValue",
						cache
				)
		);
	}

	@Test
	public void testIsLoadedWithNullInterceptor() {
		assertEquals(
				LoadState.LOADED,
				PersistenceUtilHelper.getLoadState(
						new PersistentAttributeInterceptable() {

							@Override
							public PersistentAttributeInterceptor $$_hibernate_getInterceptor() {
								return null;
							}

							@Override
							public void $$_hibernate_setInterceptor(PersistentAttributeInterceptor interceptor) {

							}
						}
				)
		);
	}
}
