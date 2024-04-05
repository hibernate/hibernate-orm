/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.batch;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

/**
 * @author Andrea Boriero
 */
@JiraKey("HHH-14108")
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.BATCH_FETCH_STYLE, value = "PADDED"),
				@Setting( name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "100" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
		}
)
public class PaddedBatchingTest extends AbstractBatchingTest {
}
