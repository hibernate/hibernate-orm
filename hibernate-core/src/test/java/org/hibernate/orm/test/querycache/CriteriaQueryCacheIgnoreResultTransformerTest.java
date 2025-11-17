/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.querycache;

import org.hibernate.CacheMode;

import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Gail Badner
 */
public class CriteriaQueryCacheIgnoreResultTransformerTest extends AbstractQueryCacheResultTransformerTest {
	@Override
	protected CacheMode getQueryCacheMode() {
		return CacheMode.IGNORE;
	}

	@Override
	protected void runTest(
			HqlExecutor hqlExecutor,
			CriteriaExecutor criteriaExecutor,
			ResultChecker checker,
			boolean isSingleResult,
			SessionFactoryScope scope)
			throws Exception {
		createData( scope );
		try {
			if ( criteriaExecutor != null ) {
				runTest( criteriaExecutor, checker, isSingleResult, scope );
			}
		}
		finally {
			deleteData( scope );
		}
	}

	@Test
	@Override
	@FailureExpected(jiraKey = "N/A", reason = "Using Transformers.ALIAS_TO_ENTITY_MAP with no projection")
	public void testAliasToEntityMapNoProjectionList(SessionFactoryScope scope) throws Exception {
		super.testAliasToEntityMapNoProjectionList( scope );
	}

	@Test
	@Override
	@FailureExpected(jiraKey = "N/A", reason = "Using Transformers.ALIAS_TO_ENTITY_MAP with no projection")
	public void testAliasToEntityMapNoProjectionMultiAndNullList(SessionFactoryScope scope) throws Exception {
		super.testAliasToEntityMapNoProjectionMultiAndNullList( scope );
	}

	@Test
	@Override
	@FailureExpected(jiraKey = "N/A", reason = "Using Transformers.ALIAS_TO_ENTITY_MAP with no projection")
	public void testAliasToEntityMapNoProjectionNullAndNonNullAliasList(SessionFactoryScope scope) throws Exception {
		super.testAliasToEntityMapNoProjectionNullAndNonNullAliasList( scope );
	}

}
