/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.userguide.util;

import jakarta.persistence.Persistence;
import jakarta.persistence.spi.LoadState;

import org.hibernate.jpa.internal.util.PersistenceUtilHelper;

import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {Author.class})
public class ProviderUtilTest {

	private final PersistenceUtilHelper.MetadataCache cache = new PersistenceUtilHelper.MetadataCache();

	@Test
	public void testIsLoadedOnUnknownClass() {
		final Object entity = new Object();
		assertTrue( Persistence.getPersistenceUtil().isLoaded( entity ) );
		assertEquals( LoadState.UNKNOWN, PersistenceUtilHelper.getLoadState( entity ) );
	}

	@Test
	public void testIsLoadedOnKnownClass() {
		final Author entity = new Author();
		assertTrue( Persistence.getPersistenceUtil().isLoaded( entity ) );
		assertEquals( LoadState.UNKNOWN, PersistenceUtilHelper.getLoadState( entity ) );
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

}
