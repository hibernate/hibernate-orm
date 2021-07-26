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

import java.util.function.Function;

import org.hibernate.spatial.testing.BaseSpatialFunctionTestCase;
import org.hibernate.spatial.testing.HQLTemplate;
import org.hibernate.spatial.testing.NativeSQLTemplate;
import org.hibernate.spatial.testing.RequiresFunction;
import org.hibernate.spatial.testing.RowObjectMapper;

@RequiresFunction( key="st_astext" )
public class AsTextFunctionTest extends BaseSpatialFunctionTestCase  {
	@Override
	protected HQLTemplate jqlQueryTemplate() {
		return HQLTemplate.from("select g.id, st_astext(g.geom) from %s g");
	}

	@Override
	protected NativeSQLTemplate sqlTemplate() {
		return templates.createNativeAsTextTemplate();
	}

	@Override
	protected Function<Object, Object> mapper() {
		RowObjectMapper<String> rowObject = new RowObjectMapper<>() {};
		return rowObject.mapper();
	}

}
