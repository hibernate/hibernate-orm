/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.postgis;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.spatial.KeyedSqmFunctionDescriptors;
import org.hibernate.spatial.FunctionKey;

import static org.hibernate.spatial.CommonSpatialFunction.*;

public class PostgisSqmFunctionDescriptors implements KeyedSqmFunctionDescriptors {

	private final Map<FunctionKey, SqmFunctionDescriptor> map = new HashMap<>();

	PostgisSqmFunctionDescriptors() {
		map.put(
				ST_GEOMETRYTYPE.getKey(), new NamedSqmFunctionDescriptor(
						ST_GEOMETRYTYPE.getKey().getName(),
						true,
						StandardArgumentsValidators.exactly( 1 ),
						StandardFunctionReturnTypeResolvers.invariant( ST_GEOMETRYTYPE.getReturnType() )
				)
		);


		map.put(
				ST_ASTEXT.getKey(), new NamedSqmFunctionDescriptor(
						ST_ASTEXT.getKey().getName(),
						true,
						StandardArgumentsValidators.exactly( 1 ),
						StandardFunctionReturnTypeResolvers.invariant( ST_ASTEXT.getReturnType() )
				)
		);

		map.put(
				ST_DIMENSION.getKey(), new NamedSqmFunctionDescriptor(
						ST_DIMENSION.getKey().getName(),
						true,
						StandardArgumentsValidators.exactly( 1 ),
						StandardFunctionReturnTypeResolvers.invariant( ST_DIMENSION.getReturnType() )
				)
		);

	}

	public Map<FunctionKey, SqmFunctionDescriptor> asMap() {
		return Collections.unmodifiableMap( map );
	}
}
