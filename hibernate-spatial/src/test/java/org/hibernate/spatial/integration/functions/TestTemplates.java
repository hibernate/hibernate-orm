/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.integration.functions;

import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.spatial.CommonSpatialFunction;
import org.hibernate.spatial.testing.dialects.NativeSQLTemplates;


public abstract class TestTemplates {

	static FunctionTestTemplate.Builder builder() {
		return new FunctionTestTemplate.Builder();
	}

	public static Stream<FunctionTestTemplate.Builder> all(NativeSQLTemplates sqlTemplates) {
		Map<CommonSpatialFunction, String> templates = sqlTemplates.all();
		return templates
				.keySet()
				.stream()
				.map( key -> builder()
						.key( key )
						.sql( templates.get( key ) ) );

	}
}
