/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.test.util;

import javax.persistence.spi.LoadState;

import org.hibernate.ejb.util.PersistenceUtilHelper;

/**
 * Tests for HHH-5094
 *
 * @author Hardy Ferentschik
 */
public class PersistenceUtilHelperTest extends junit.framework.TestCase {
	private final PersistenceUtilHelper.MetadataCache cache = new PersistenceUtilHelper.MetadataCache();

	public static class FieldAccessBean {
		public String publicAccessProperty;
		protected String protectedAccessProperty;
		private String privateAccessProperty;
	}

	public static class MethodAccessBean {
		private String publicAccessProperty;
		private String protectedAccessProperty;
		private String privateAccessProperty;

		public String getPublicAccessPropertyValue() {
			return publicAccessProperty;
		}

		protected String getProtectedAccessPropertyValue() {
			return protectedAccessProperty;
		}

		private String getPrivateAccessPropertyValue() {
			return privateAccessProperty;
		}
	}

	public void testIsLoadedWithReferencePublicField() {
		assertEquals(
				LoadState.UNKNOWN,
				PersistenceUtilHelper.isLoadedWithReference( new FieldAccessBean(), "publicAccessProperty", cache )
		);
	}

	public void testIsLoadedWithReferencePublicMethod() {
		assertEquals(
				LoadState.UNKNOWN,
				PersistenceUtilHelper.isLoadedWithReference(
						new MethodAccessBean(), "publicAccessPropertyValue", cache
				)
		);
	}

	public void testIsLoadedWithReferenceProtectedField() {
		assertEquals(
				LoadState.UNKNOWN,
				PersistenceUtilHelper.isLoadedWithReference( new FieldAccessBean(), "protectedAccessProperty", cache )
		);
	}

	public void testIsLoadedWithReferenceProtectedMethod() {
		assertEquals(
				LoadState.UNKNOWN,
				PersistenceUtilHelper.isLoadedWithReference(
						new MethodAccessBean(), "protectedAccessPropertyValue", cache
				)
		);
	}

	public void testIsLoadedWithReferencePrivateField() {
		assertEquals(
				LoadState.UNKNOWN,
				PersistenceUtilHelper.isLoadedWithReference( new FieldAccessBean(), "privateAccessProperty", cache )
		);
	}

	public void testIsLoadedWithReferencePrivateMethod() {
		assertEquals(
				LoadState.UNKNOWN,
				PersistenceUtilHelper.isLoadedWithReference(
						new MethodAccessBean(), "privateAccessPropertyValue", cache
				)
		);
	}
}
