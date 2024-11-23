/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.hhh14407;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.persistent.PersistentTableBulkIdStrategy;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Nathan Xu
 * @author Sönke Reimer
 */
@RequiresDialect( value = H2Dialect.class )
@TestForIssue( jiraKey = "HHH14407" )
public class PersistentTableBulkIdStrategyNPETest extends BaseCoreFunctionalTestCase {

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty(
				Environment.DIALECT,
				PersistentTableBulkIdH2Dialect.class.getName()
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				ParentEntity.class,
				ChildEntity.class
		};
	}

	@Test
	public void hhh14407Test() {
		// without fix of HHH14407, the test case will trigger exception due to NPE in PersistentTableBulkIdStrategy
	}

	public static class PersistentTableBulkIdH2Dialect extends H2Dialect {

		@Override
		public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
			return new PersistentTableBulkIdStrategy();
		}

	}

}
