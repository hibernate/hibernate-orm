/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.querycache;

import org.junit.Test;

import org.hibernate.CacheMode;
import org.hibernate.orm.test.querycache.AbstractHqlQueryCacheResultTransformerTest;

import org.hibernate.testing.FailureExpected;

/**
 * @author Gail Badner
 */
public class HqlQueryCacheIgnoreResultTransformerTest extends AbstractHqlQueryCacheResultTransformerTest {
	@Override
	protected CacheMode getQueryCacheMode() {
		return CacheMode.IGNORE;
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
	public void testMultiSelectNewMapUsingAliasesWithFetchJoinList() throws Exception {
		super.testMultiSelectNewMapUsingAliasesWithFetchJoinList();
	}

}
