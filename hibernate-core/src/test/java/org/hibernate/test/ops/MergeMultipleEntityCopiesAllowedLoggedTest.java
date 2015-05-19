/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ops;

import org.hibernate.cfg.Configuration;

/**
 * Tests merging multiple detached representations of the same entity when explicitly allowed and logged.
 *
 * @author Gail Badner
 */
public class MergeMultipleEntityCopiesAllowedLoggedTest extends MergeMultipleEntityCopiesAllowedTest {
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty(
				"hibernate.event.merge.entity_copy_observer",
				"log"
		);
	}
}
