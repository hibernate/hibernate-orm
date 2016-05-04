/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.emops;

import java.util.Map;

import org.hibernate.testing.TestForIssue;

/**
 * Tests merging multiple detached representations of the same entity when
 * explicitly disallowed.
 *
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-9106")
public class MergeMultipleEntityCopiesDisallowedTest extends MergeMultipleEntityCopiesDisallowedByDefaultTest {

	@SuppressWarnings( {"unchecked"})
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put(
				"hibernate.event.merge.entity_copy_observer",
				"disallow"
		);
	}
}
