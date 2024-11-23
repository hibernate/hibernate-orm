/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy.batch;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.loader.BatchFetchStyle;

import org.hibernate.testing.TestForIssue;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-14108")
public class DynamicBatchingTest extends AbstractBatchingTest {

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.BATCH_FETCH_STYLE, BatchFetchStyle.DYNAMIC.toString() );
	}

}
