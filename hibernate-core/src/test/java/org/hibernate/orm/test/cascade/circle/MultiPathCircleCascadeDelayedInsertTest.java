/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cascade.circle;

import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;

/**
 * @author Gail Badner
 */
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/cascade/circle/MultiPathCircleCascadeDelayedInsert.hbm.xml"
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@ServiceRegistry.Setting(name = Environment.GENERATE_STATISTICS, value = "true"),
				@ServiceRegistry.Setting(name = Environment.STATEMENT_BATCH_SIZE, value = "0"),
		}
)
public class MultiPathCircleCascadeDelayedInsertTest extends AbstractMultiPathCircleCascadeTest {
}
