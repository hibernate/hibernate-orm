/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ops;

import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;


/**
 * Tests merging multiple detached representations of the same entity when explicitly disallowed.
 *
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-9106")
public class MergeMultipleEntityCopiesDisallowedTest extends MergeMultipleEntityCopiesDisallowedByDefaultTest {

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty(
				"hibernate.event.merge.entity_copy_observer",
				"disallow"
		);
	}
}
