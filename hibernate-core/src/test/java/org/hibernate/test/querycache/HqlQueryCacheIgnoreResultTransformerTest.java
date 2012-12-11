/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.querycache;

import org.junit.Test;

import org.hibernate.CacheMode;
import org.hibernate.testing.FailureExpected;

/**
 * @author Gail Badner
 */
public class HqlQueryCacheIgnoreResultTransformerTest extends AbstractQueryCacheResultTransformerTest {
	@Override
	protected CacheMode getQueryCacheMode() {
		return CacheMode.IGNORE;
	}

	@Override
	protected void runTest(HqlExecutor hqlExecutor, CriteriaExecutor criteriaExecutor, ResultChecker checker, boolean isSingleResult)
		throws Exception {
		createData();
		if ( hqlExecutor != null ) {
			runTest( hqlExecutor, checker, isSingleResult );
		}
		deleteData();
	}

	@Test
	@Override
	@FailureExpected( jiraKey = "N/A", message = "HQL query using Transformers.ALIAS_TO_ENTITY_MAP with no projection" )
	public void testAliasToEntityMapNoProjectionList() throws Exception {
		super.testAliasToEntityMapNoProjectionList();
	}

	@Test
	@Override
	@FailureExpected( jiraKey = "N/A", message = "HQL query using Transformers.ALIAS_TO_ENTITY_MAP with no projection" )
	public void testAliasToEntityMapNoProjectionMultiAndNullList() throws Exception {
		super.testAliasToEntityMapNoProjectionMultiAndNullList();
	}

	@Test
	@Override
	@FailureExpected( jiraKey = "N/A", message = "HQL query using Transformers.ALIAS_TO_ENTITY_MAP with no projection" )
	public void testAliasToEntityMapNoProjectionNullAndNonNullAliasList() throws Exception {
		super.testAliasToEntityMapNoProjectionNullAndNonNullAliasList();
	}

	@Test
	@Override
	@FailureExpected( jiraKey = "HHH-3345", message = "HQL query using 'select new' and 'join fetch'" )
	public void testMultiSelectNewMapUsingAliasesWithFetchJoinList() throws Exception {
		super.testMultiSelectNewMapUsingAliasesWithFetchJoinList();
	}

}
