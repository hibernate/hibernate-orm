/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.ops;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

/**
 * Tests merging multiple detached representations of the same entity when explicitly allowed and logged.
 *
 * @author Gail Badner
 */
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.MERGE_ENTITY_COPY_OBSERVER, value = "log")
)
public class MergeMultipleEntityCopiesAllowedLoggedTest extends MergeMultipleEntityCopiesAllowedTest {
}
