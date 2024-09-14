/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.testing.dialects.oracle;

import org.hibernate.spatial.testing.dialects.PredicateRegexes;

public class OraclePredicateRegexes extends PredicateRegexes {

	public OraclePredicateRegexes() {
		super( null );
		add(
				"overlaps",
				"select .* from .* where st_geometry\\(.*\\).st_overlap.*=.*"
		);
		add(
				"crosses",
				"select .* from .* where st_geometry\\(.*\\).st_crosses.*=.*"

		);
		add(
				"contains",
				"select .* from .* where st_geometry\\(.*\\).st_contains.*=.*"
		);
		add(
				"disjoint",
				"select .* from .* where st_geometry\\(.*\\).st_disjoint.*=.*"
		);
		add(
				"touches",
				"select .* from .* where st_geometry\\(.*\\).st_touches.*=.*"
		);
		add(
				"within",
				"select .* from .* where st_geometry\\(.*\\).st_within.*=.*"

		);
		add(
				"intersects",
				"select .* from .* where st_geometry\\(.*\\).st_intersects.*=.*"
		);
		add(
				"eq",
				"select .* from .* where st_geometry\\(.*\\).st_equals.*=.*"
		);
	}
}
