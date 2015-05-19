/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.querycache;

import org.hibernate.CacheMode;

/**
 * @author Gail Badner
 */
public class CriteriaQueryCacheIgnoreResultTransformerTest extends AbstractQueryCacheResultTransformerTest {
	@Override
	protected CacheMode getQueryCacheMode() {
		return CacheMode.IGNORE;
	}

	@Override
	protected void runTest(HqlExecutor hqlExecutor, CriteriaExecutor criteriaExecutor, ResultChecker checker, boolean isSingleResult)
		throws Exception {
		createData();
		try {
			if ( criteriaExecutor != null ) {
				runTest( criteriaExecutor, checker, isSingleResult );
			}
		}
		finally {
			deleteData();
		}
	}
}
