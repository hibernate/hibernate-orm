/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat.internal;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertNull;

@TestForIssue(jiraKey = "HHH-13645")
public class StatsNamedContainerNullComputedValueTest {

	@Test
	public void testNullComputedValue() {
		final StatsNamedContainer statsNamedContainer = new StatsNamedContainer<Integer>();
		assertNull(
				statsNamedContainer.getOrCompute(
						"key",
						v -> {
							return null;
						}
				)
		);
	}

}