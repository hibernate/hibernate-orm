/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
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
 *
 */
package org.hibernate.test.querycache;

import junit.framework.Test;

import org.hibernate.CacheMode;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gail Badner
 */
public class HqlQueryCacheNormalResultTransformerTest extends HqlQueryCachePutResultTransformerTest {

	public HqlQueryCacheNormalResultTransformerTest(String str) {
		super( str );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( HqlQueryCacheNormalResultTransformerTest.class );
	}

	protected CacheMode getQueryCacheMode() {
		return CacheMode.NORMAL;
	}

	public void testAliasToBeanDtoMultiArgList() {
		reportSkip( "Results from queries using Transformers.aliasToBean cannot be found in the cache due to bug in hashCode",
				"Query using Transformers.aliasToBean with cache"
		);
	}
	public void testAliasToBeanDtoMultiArgListFailureExpected() throws Exception {
		super.testAliasToBeanDtoMultiArgList();
	}

	public void testAliasToBeanDtoLiteralArgList() {
		reportSkip( "Results from queries using Transformers.aliasToBean cannot be found in the cache due to bug in hashCode",
				"Query using Transformers.aliasToBean with cache" );
	}
	public void testAliasToBeanDtoLiteralArgListFailureExpected() throws Exception {
		super.testAliasToBeanDtoLiteralArgList();
	}

	public void testAliasToBeanDtoWithNullAliasList() {
		reportSkip( "Results from queries using Transformers.aliasToBean cannot be found in the cache due to bug in hashCode",
				"Query using Transformers.aliasToBean with cache" );
	}
	public void testAliasToBeanDtoWithNullAliasListFailureExpected() throws Exception {
		super.testAliasToBeanDtoWithNullAliasList();
	}
}
