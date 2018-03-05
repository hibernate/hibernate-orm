/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.batchfetch;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.loader.BatchFetchStyle;

/**
 * @author Gail Badner
 */
public class BatchFetchNotFoundIgnoreDynamicStyleTest extends BatchFetchNotFoundIgnoreDefaultStyleTest {

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.getProperties().put( Environment.BATCH_FETCH_STYLE, BatchFetchStyle.DYNAMIC );
	}
}
