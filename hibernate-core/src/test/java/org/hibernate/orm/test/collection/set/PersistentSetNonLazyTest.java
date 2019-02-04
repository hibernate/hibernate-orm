/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.set;

import org.hibernate.testing.FailureExpected;
import org.junit.Test;

/**
 * @author Gail Badner
 */
public class PersistentSetNonLazyTest extends PersistentSetTest {
	public String[] getMappings() {
		return new String[] { "collection/set/MappingsNonLazy.hbm.xml" };
	}

	@Override
	protected String getCacheConcurrencyStrategy() {
		return "nonstrict-read-write";
	}

	@Test
	@Override
	@FailureExpected(
			jiraKey = "HHH-3799",
			message = "known to fail with non-lazy collection using query cache"
	)
	public void testLoadChildCheckParentContainsChildCache() {
		 super.testLoadChildCheckParentContainsChildCache();
	}
}
