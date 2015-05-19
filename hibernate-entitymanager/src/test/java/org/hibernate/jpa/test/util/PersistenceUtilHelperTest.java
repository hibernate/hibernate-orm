/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.util;

import javax.persistence.spi.LoadState;

import org.junit.Test;

import org.hibernate.jpa.internal.util.PersistenceUtilHelper;

import static org.junit.Assert.assertEquals;
/**
 * Tests for HHH-5094 and HHH-5334
 *
 * @author Hardy Ferentschik
 */
public class PersistenceUtilHelperTest{
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
				PersistenceUtilHelper.isLoadedWithReference( new FieldAccessBean(), "publicAccessProperty", cache )
		);
	}
    @Test
	public void testIsLoadedWithReferencePublicMethod() {
		assertEquals(
				LoadState.UNKNOWN,
				PersistenceUtilHelper.isLoadedWithReference(
						new MethodAccessBean(), "publicAccessPropertyValue", cache
				)
		);
	}
   @Test
	public void testIsLoadedWithReferenceProtectedField() {
		assertEquals(
				LoadState.UNKNOWN,
				PersistenceUtilHelper.isLoadedWithReference( new FieldAccessBean(), "protectedAccessProperty", cache )
		);
	}
    @Test
	public void testIsLoadedWithReferenceProtectedMethod() {
		assertEquals(
				LoadState.UNKNOWN,
				PersistenceUtilHelper.isLoadedWithReference(
						new MethodAccessBean(), "protectedAccessPropertyValue", cache
				)
		);
	}
    @Test
	public void testIsLoadedWithReferencePrivateField() {
		assertEquals(
				LoadState.UNKNOWN,
				PersistenceUtilHelper.isLoadedWithReference( new FieldAccessBean(), "privateAccessProperty", cache )
		);
	}
    @Test
	public void testIsLoadedWithReferencePrivateMethod() {
		assertEquals(
				LoadState.UNKNOWN,
				PersistenceUtilHelper.isLoadedWithReference(
						new MethodAccessBean(), "privateAccessPropertyValue", cache
				)
		);
	}
}
