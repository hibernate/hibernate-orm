/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.caching.mocked;

import org.junit.Test;

import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.UnknownAccessTypeException;
import org.hibernate.testing.orm.junit.JiraKey;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * @author Gail Badner
 */
public class CacheAccessTypeTest {

	@Test
	@JiraKey( value = "HHH-9844")
	public void testExplicitExternalNames() {
		assertSame( AccessType.READ_ONLY, AccessType.fromExternalName( "read-only" ) );
		assertSame( AccessType.READ_WRITE, AccessType.fromExternalName( "read-write" ) );
		assertSame( AccessType.NONSTRICT_READ_WRITE, AccessType.fromExternalName( "nonstrict-read-write" ) );
		assertSame( AccessType.TRANSACTIONAL, AccessType.fromExternalName( "transactional" ) );
	}

	@Test
	@JiraKey( value = "HHH-9844")
	public void testEnumNames() {
		assertSame( AccessType.READ_ONLY, AccessType.fromExternalName( "READ_ONLY" ) );
		assertSame( AccessType.READ_WRITE, AccessType.fromExternalName( "READ_WRITE" ) );
		assertSame( AccessType.NONSTRICT_READ_WRITE, AccessType.fromExternalName( "NONSTRICT_READ_WRITE" ) );
		assertSame( AccessType.TRANSACTIONAL, AccessType.fromExternalName( "TRANSACTIONAL" ) );
	}

	@Test
	@JiraKey( value = "HHH-9844")
	public void testLowerCaseEnumNames() {
		assertSame( AccessType.READ_ONLY, AccessType.fromExternalName( "read_only" ) );
		assertSame( AccessType.READ_WRITE, AccessType.fromExternalName( "read_write" ) );
		assertSame( AccessType.NONSTRICT_READ_WRITE, AccessType.fromExternalName( "nonstrict_read_write" ) );
		assertSame( AccessType.TRANSACTIONAL, AccessType.fromExternalName( "transactional" ) );
	}

	@Test
	@JiraKey( value = "HHH-9844")
	public void testUpperCaseWithHyphens() {
		try {
			AccessType.fromExternalName( "READ-ONLY" );
			fail( "should have failed because upper-case using hyphans is not supported." );
		}
		catch (UnknownAccessTypeException ex) {
			// expected
		}
		try {
			AccessType.fromExternalName( "READ-WRITE" );
			fail( "should have failed because upper-case using hyphans is not supported." );
		}
		catch (UnknownAccessTypeException ex) {
			// expected
		}
		try {
			AccessType.fromExternalName( "NONSTRICT-READ-WRITE" );
			fail( "should have failed because upper-case using hyphans is not supported." );
		}
		catch (UnknownAccessTypeException ex) {
			// expected
		}
	}
}
