/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class DerbyDialectDefaultMultiTableBulkIdStrategyTest extends BaseUnitTestCase {
	@Test
	@TestForIssue(jiraKey = "HHH-10238")
	public void testDefaultMultiTableBulkIdStrategyIsLocal() {
		MultiTableBulkIdStrategy actual = new DerbyDialectTestCase.LocalDerbyDialect().getFallbackSqmMutationStrategy(
				runtimeRootEntityDescriptor );
		assertThat(actual, is(instanceOf(LocalTemporaryTableBulkIdStrategy.class)));
	}
}
