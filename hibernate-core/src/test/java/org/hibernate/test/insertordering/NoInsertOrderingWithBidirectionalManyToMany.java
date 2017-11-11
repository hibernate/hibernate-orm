/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import org.hibernate.cfg.Environment;
import org.hibernate.testing.TestForIssue;

import java.util.Map;

/**
 * InsertOrderingWithBidirectionalManyToMany with order_inserts = false
 */
@TestForIssue(jiraKey = "HHH-12088")
public class NoInsertOrderingWithBidirectionalManyToMany
		extends InsertOrderingWithBidirectionalManyToMany {

	@Override
	protected void addSettings(Map settings) {
		super.addSettings(settings);
		settings.put(Environment.ORDER_INSERTS, "false");
	}
}
