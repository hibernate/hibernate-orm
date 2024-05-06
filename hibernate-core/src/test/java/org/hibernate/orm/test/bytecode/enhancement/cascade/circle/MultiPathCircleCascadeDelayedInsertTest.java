/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.cascade.circle;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.orm.test.bytecode.enhancement.lazy.NoDirtyCheckingContext;
import org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking.DirtyCheckEnhancementContext;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.junit.runner.RunWith;

/**
 * @author Gail Badner
 */
@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({ NoDirtyCheckingContext.class, DirtyCheckEnhancementContext.class })
public class MultiPathCircleCascadeDelayedInsertTest extends AbstractMultiPathCircleCascadeTest {
	@Override
	protected String[] getOrmXmlFiles() {
		return new String[] {
				"org/hibernate/orm/test/cascade/circle/MultiPathCircleCascadeDelayedInsert.hbm.xml"
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( Environment.GENERATE_STATISTICS, "true" );
		configuration.setProperty( Environment.STATEMENT_BATCH_SIZE, "0" );
	}
}
