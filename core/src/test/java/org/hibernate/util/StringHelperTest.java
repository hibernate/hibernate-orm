/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.util;

import junit.framework.TestCase;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class StringHelperTest extends TestCase {
	private static final String BASE_PACKAGE = "org.hibernate";
	private static final String STRING_HELPER_FQN = "org.hibernate.util.StringHelper";
	private static final String STRING_HELPER_NAME = StringHelper.unqualify( STRING_HELPER_FQN );

	public void testNameCollapsing() {
		assertNull( StringHelper.collapse( null ) );
		assertEquals( STRING_HELPER_NAME, StringHelper.collapse( STRING_HELPER_NAME ) );
		assertEquals( "o.h.u.StringHelper", StringHelper.collapse( STRING_HELPER_FQN ) );
	}

	public void testPartialNameUnqualification() {
		assertNull( StringHelper.partiallyUnqualify( null, BASE_PACKAGE ) );
		assertEquals( STRING_HELPER_NAME, StringHelper.partiallyUnqualify( STRING_HELPER_NAME, BASE_PACKAGE ) );
		assertEquals( "util.StringHelper", StringHelper.partiallyUnqualify( STRING_HELPER_FQN, BASE_PACKAGE ) );
	}

	public void testBasePackageCollapsing() {
		assertNull( StringHelper.collapseQualifierBase( null, BASE_PACKAGE ) );
		assertEquals( STRING_HELPER_NAME, StringHelper.collapseQualifierBase( STRING_HELPER_NAME, BASE_PACKAGE ) );
		assertEquals( "o.h.util.StringHelper", StringHelper.collapseQualifierBase( STRING_HELPER_FQN, BASE_PACKAGE ) );
	}
}
