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
public class CriteriaQueryCachePutResultTransformerTest extends CriteriaQueryCacheIgnoreResultTransformerTest {
	@Override
	protected CacheMode getQueryCacheMode() {
		return CacheMode.PUT;
	}

	@Override
	protected boolean areDynamicNonLazyAssociationsChecked() {
		return false;
	}
}
