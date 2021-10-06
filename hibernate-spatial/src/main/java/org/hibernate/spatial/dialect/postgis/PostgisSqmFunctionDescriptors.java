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

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.spatial.CommonSpatialFunction;
import org.hibernate.spatial.FunctionKey;
import org.hibernate.spatial.KeyedSqmFunctionDescriptors;
import org.hibernate.type.BasicTypeRegistry;

import static org.hibernate.spatial.CommonSpatialFunction.values;

public class PostgisSqmFunctionDescriptors implements KeyedSqmFunctionDescriptors {

	private final Map<FunctionKey, SqmFunctionDescriptor> map = new HashMap<>();

	public PostgisSqmFunctionDescriptors(FunctionContributions functionContributions) {
		final BasicTypeRegistry basicTypeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();
		for ( CommonSpatialFunction func : values() ) {
			final FunctionReturnTypeResolver returnTypeResolver;
			if ( func.getReturnType() == null ) {
				returnTypeResolver = null;
			}
			else {
				returnTypeResolver = StandardFunctionReturnTypeResolvers.invariant(
						basicTypeRegistry.resolve( func.getReturnType() )
				);
			}
			map.put(
					func.getKey(),
					new NamedSqmFunctionDescriptor(
							func.getKey().getName(),
							true,
							StandardArgumentsValidators.exactly( func.getNumArgs() ),
							returnTypeResolver
					)
			);
		}
	}

	public Map<FunctionKey, SqmFunctionDescriptor> asMap() {
		return Collections.unmodifiableMap( map );
	}

}
