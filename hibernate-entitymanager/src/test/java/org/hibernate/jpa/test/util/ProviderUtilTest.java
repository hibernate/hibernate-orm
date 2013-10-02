/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.jpa.test.util;

import javax.persistence.Persistence;
import javax.persistence.spi.LoadState;

import org.hibernate.jpa.internal.util.PersistenceUtilHelper;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class ProviderUtilTest extends BaseEntityManagerFunctionalTestCase {

	private final PersistenceUtilHelper.MetadataCache cache = new PersistenceUtilHelper.MetadataCache();

	@Test
	public void testIsLoadedOnUnknownClass() {
		final Object entity = new Object();
		assertTrue( Persistence.getPersistenceUtil().isLoaded( entity ) );
		assertEquals( LoadState.UNKNOWN, PersistenceUtilHelper.isLoaded( entity ) );
	}

	@Test
	public void testIsLoadedOnKnownClass() {
		final Author entity = new Author();
		assertTrue( Persistence.getPersistenceUtil().isLoaded( entity ) );
		assertEquals( LoadState.UNKNOWN, PersistenceUtilHelper.isLoaded( entity ) );
	}

	@Test
	public void testIsLoadedWithoutReferenceOnUnknownClass() {
		final Object entity = new Object();
		assertEquals( LoadState.UNKNOWN, PersistenceUtilHelper.isLoadedWithoutReference( entity, "attribute", cache ) );
	}

	@Test
	public void testIsLoadedWithoutReferenceOnKnownClass() {
		final Author entity = new Author();
		assertEquals( LoadState.UNKNOWN, PersistenceUtilHelper.isLoadedWithoutReference( entity, "attribute", cache ) );
	}

	@Test
	public void testIsLoadedWithReferenceOnUnknownClass() {
		final Object entity = new Object();
		assertEquals( LoadState.UNKNOWN, PersistenceUtilHelper.isLoadedWithReference( entity, "attribute", cache ) );
	}

	@Test
	public void testIsLoadedWithReferenceOnKnownClass() {
		final Author entity = new Author();
		assertEquals( LoadState.UNKNOWN, PersistenceUtilHelper.isLoadedWithReference( entity, "attribute", cache ) );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Author.class,
				Book.class,
				CopyrightableContent.class
		};
	}
}
