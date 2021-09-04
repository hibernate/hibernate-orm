/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.dialects;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;


public class PredicateRegexes {

	protected final Map<String, String> regexes = new HashMap<>();

	// Note that we alias the function invocation so that
	// we can map the return value to the required type
	public PredicateRegexes() {
		regexes.put(
				"overlaps",
				"select .* from .* where st_overlaps\\(.*geom\\s*,.*st_geomfromewkt\\(.*\\)\\s*=\\?.*"
		);
		regexes.put(
				"intersects",
				"select .* from .* where st_intersects\\(.*geom\\s*,.*st_geomfromewkt\\(.*\\)\\s*=\\?.*"
		);
		regexes.put(
				"crosses",
				"select .* from .* where st_crosses\\(.*geom\\s*,.*st_geomfromewkt\\(.*\\)\\s*=\\?.*"
		);
		regexes.put(
				"contains",
				"select .* from .* where st_contains\\(.*geom\\s*,.*st_geomfromewkt\\(.*\\)\\s*=\\?.*"
		);
		regexes.put(
				"disjoint",
				"select .* from .* where st_disjoint\\(.*geom\\s*,.*st_geomfromewkt\\(.*\\)\\s*=\\?.*"
		);
		regexes.put(
				"touches",
				"select .* from .* where st_touches\\(.*geom\\s*,.*st_geomfromewkt\\(.*\\)\\s*=\\?.*"
		);
		regexes.put(
				"within",
				"select .* from .* where st_within\\(.*geom\\s*,.*st_geomfromewkt\\(.*\\)\\s*=\\?.*"
		);
		regexes.put(
				"eq",
				"select .* from .* where st_equals\\(.*geom\\s*,.*st_geomfromewkt\\(.*\\)\\s*=\\?.*"
		);

	}

	public Stream<Map.Entry<String, String>> all() {
		return this.regexes.entrySet().stream();
	}

}
