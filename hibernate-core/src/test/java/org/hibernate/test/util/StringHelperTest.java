/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.util;

import org.junit.Test;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class StringHelperTest extends BaseUnitTestCase {
	private static final String BASE_PACKAGE = "org.hibernate";
	private static final String STRING_HELPER_FQN = "org.hibernate.internal.util.StringHelper";
	private static final String STRING_HELPER_NAME = StringHelper.unqualify( STRING_HELPER_FQN );

	@Test
	public void testNameCollapsing() {
		assertNull( StringHelper.collapse( null ) );
		assertEquals( STRING_HELPER_NAME, StringHelper.collapse( STRING_HELPER_NAME ) );
		assertEquals( "o.h.i.u.StringHelper", StringHelper.collapse( STRING_HELPER_FQN ) );
	}

	@Test
	public void testPartialNameUnqualification() {
		assertNull( StringHelper.partiallyUnqualify( null, BASE_PACKAGE ) );
		assertEquals( STRING_HELPER_NAME, StringHelper.partiallyUnqualify( STRING_HELPER_NAME, BASE_PACKAGE ) );
		assertEquals( "internal.util.StringHelper", StringHelper.partiallyUnqualify( STRING_HELPER_FQN, BASE_PACKAGE ) );
	}

	@Test
	public void testBasePackageCollapsing() {
		assertNull( StringHelper.collapseQualifierBase( null, BASE_PACKAGE ) );
		assertEquals( STRING_HELPER_NAME, StringHelper.collapseQualifierBase( STRING_HELPER_NAME, BASE_PACKAGE ) );
		assertEquals( "o.h.internal.util.StringHelper", StringHelper.collapseQualifierBase( STRING_HELPER_FQN, BASE_PACKAGE ) );
	}

	@Test
	public void testFindInterpolationKeys() {

		Set<String> results = StringHelper.findInterpolationKeys( "{}" );
		assertEquals( 0, results.size() );

		results = StringHelper.findInterpolationKeys( "{person}.fname and {person}.lname" );
		assertEquals( 1, results.size() );
		assertTrue( results.contains( "person" ) );

		results = StringHelper.findInterpolationKeys( "{person}.fname and {other_123}.fname" );
		assertEquals( 2, results.size() );
		assertTrue( results.contains( "person" ) );
		assertTrue( results.contains( "other_123" ) );

		// should ignore whitespace and non-word characters
		results = StringHelper.findInterpolationKeys( "{'person'}{ other }{*}{person.fname}" );
		assertEquals( 0, results.size() );
	}
}
