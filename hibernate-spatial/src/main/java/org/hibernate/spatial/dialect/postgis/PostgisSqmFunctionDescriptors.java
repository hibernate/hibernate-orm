/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.postgis;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.CommonSpatialFunction;
import org.hibernate.spatial.KeyedSqmFunctionDescriptors;
import org.hibernate.spatial.FunctionKey;

import static org.hibernate.spatial.CommonSpatialFunction.*;

public class PostgisSqmFunctionDescriptors implements KeyedSqmFunctionDescriptors {

	private final Map<FunctionKey, SqmFunctionDescriptor> map = new HashMap<>();

	public PostgisSqmFunctionDescriptors(ServiceRegistry serviceRegistry) {
		Arrays.stream( values() )
				.forEach( cf -> map.put( cf.getKey(), toPGFunction( cf ) ) );
	}

	public Map<FunctionKey, SqmFunctionDescriptor> asMap() {
		return Collections.unmodifiableMap( map );
	}

	private SqmFunctionDescriptor toPGFunction(CommonSpatialFunction func) {
		return new NamedSqmFunctionDescriptor(
				func.getKey().getName(),
				true,
				StandardArgumentsValidators.exactly( func.getNumArgs() ),
				func.getReturnType() != null ? StandardFunctionReturnTypeResolvers.invariant( func.getReturnType() ) :
						null
		);
	}

}
