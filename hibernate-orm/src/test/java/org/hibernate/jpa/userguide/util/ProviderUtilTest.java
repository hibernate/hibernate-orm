/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.userguide.util;

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
