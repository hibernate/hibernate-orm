/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.testing.dialects.sqlserver;

import org.hibernate.spatial.testing.dialects.PredicateRegexes;

public class SqlServerPredicateRegexes extends PredicateRegexes {
	public SqlServerPredicateRegexes() {
		super( null );
		add(
				"overlaps",
				"select .* from .* where .*\\.geom\\.stoverlaps\\(.*\\)\\s*=.*"
		);
		add(
				"crosses",
				"select .* from .* where .*\\.geom\\.stcrosses\\(.*\\)\\s*=.*"

		);
		add(
				"contains",
				"select .* from .* where .*\\.geom\\.stcontains\\(.*\\)\\s*=.*"
		);
		add(
				"disjoint",
				"select .* from .* where .*\\.geom\\.stdisjoint\\(.*\\)\\s*=.*"
		);
		add(
				"touches",
				"select .* from .* where .*\\.geom\\.sttouches\\(.*\\)\\s*=.*"
		);
		add(
				"within",
				"select .* from .* where .*\\.geom\\.stwithin\\(.*\\)\\s*=.*"

		);
		add(
				"intersects",
				"select .* from .* where .*\\.geom\\.stintersects\\(.*\\)\\s*=.*"
		);
		add(
				"eq",
				"select .* from .* where .*\\.geom\\.stequals\\(.*\\)\\s*=.*"
		);
	}

}
