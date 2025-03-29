/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.oracle;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.spatial.CommonSpatialFunction;
import org.hibernate.spatial.FunctionKey;
import org.hibernate.spatial.KeyedSqmFunctionDescriptors;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;

public class OracleSDOFunctionDescriptors implements KeyedSqmFunctionDescriptors {

	private final Map<FunctionKey, SqmFunctionDescriptor> map = new HashMap<>();
	private final BasicTypeRegistry typeRegistry;

	public OracleSDOFunctionDescriptors(FunctionContributions functionContributions) {
		typeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();
		registerSDOFunctions();
	}

	@Override
	public Map<FunctionKey, SqmFunctionDescriptor> asMap() {
		return Collections.unmodifiableMap( map );
	}

	private void registerSDOFunctions() {
		map.put( CommonSpatialFunction.ST_ASTEXT.getKey(), new NamedSqmFunctionDescriptor(
				"SDO_UTIL.TO_WKTGEOMETRY",
				false,
				StandardArgumentsValidators.exactly(
						1 ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeRegistry.resolve(
								StandardBasicTypes.STRING ) )
		) );

		map.put( CommonSpatialFunction.ST_GEOMETRYTYPE.getKey(), new SDOGetGeometryType( typeRegistry ) );
		map.put( CommonSpatialFunction.ST_DIMENSION.getKey(), new SDOMethodDescriptor(
				"Get_Dims",
				StandardArgumentsValidators.exactly(
						1 ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeRegistry.resolve(
								StandardBasicTypes.INTEGER ) )
		) );

		map.put(
				CommonSpatialFunction.ST_ENVELOPE.getKey(),
				new NamedSqmFunctionDescriptor(
						"SDO_GEOM.SDO_MBR",
						true,
						StandardArgumentsValidators.exactly( 1 ),
						StandardFunctionReturnTypeResolvers.useFirstNonNull()
				)
		);

		map.put(
				CommonSpatialFunction.ST_SRID.getKey(),
				new SDOMethodDescriptor(
						"SDO_SRID",
						false,
						StandardArgumentsValidators.exactly( 1 ),
						StandardFunctionReturnTypeResolvers.invariant(
								typeRegistry.resolve(
										StandardBasicTypes.INTEGER
								)
						)
				)
		);

		map.put(
				CommonSpatialFunction.ST_ASBINARY.getKey(),
				new SDOMethodDescriptor(
						"Get_WKB",
						true,
						StandardArgumentsValidators.exactly( 1 ),
						StandardFunctionReturnTypeResolvers.invariant( typeRegistry.resolve( StandardBasicTypes.BINARY ) )
				)
		);

		map.put(
				CommonSpatialFunction.ST_ISSIMPLE.getKey(),
				new OracleSpatialSQLMMFunction(
						"ST_ISSIMPLE",
						"ST_ISSIMPLE",
						1,
						StandardFunctionReturnTypeResolvers.invariant(
								typeRegistry.resolve( StandardBasicTypes.BOOLEAN )
						),
						false
				)
		);

		map.put(
				CommonSpatialFunction.ST_ISEMPTY.getKey(),
				new OracleSpatialSQLMMFunction(
						"ST_ISEMPTY",
						"ST_ISEMPTY",
						1,
						StandardFunctionReturnTypeResolvers.invariant(
								typeRegistry.resolve( StandardBasicTypes.BOOLEAN )
						),
						false
				)
		);

		map.put(
				CommonSpatialFunction.ST_BOUNDARY.getKey(),
				new OracleSpatialSQLMMFunction(
						"ST_BOUNDARY",
						"ST_BOUNDARY",
						1,
						StandardFunctionReturnTypeResolvers.useFirstNonNull(),
						true
				)
		);

		map.put(
				CommonSpatialFunction.ST_OVERLAPS.getKey(),
				new SDORelateFunction( List.of( "OVERLAPBDYDISJOINT", "OVERLAPBDYINTERSECT" ), typeRegistry )
		);

		map.put(
				CommonSpatialFunction.ST_CROSSES.getKey(),
				new OracleSpatialSQLMMFunction(
						"ST_CROSSES",
						"ST_CROSSES",
						2,
						StandardFunctionReturnTypeResolvers.invariant(
								typeRegistry.resolve( StandardBasicTypes.BOOLEAN )
						),
						false
				)
		);

		map.put(
				CommonSpatialFunction.ST_INTERSECTS.getKey(),
				new SDORelateFunction( List.of( "ANYINTERACT" ), typeRegistry )
		);

		map.put(
				CommonSpatialFunction.ST_CONTAINS.getKey(),
				new SDORelateFunction( List.of( "CONTAINS", "COVERS" ), typeRegistry )
		);

		map.put(
				CommonSpatialFunction.ST_DISJOINT.getKey(),
				new SDORelateFunction( List.of( "DISJOINT" ), typeRegistry )
		);

		map.put( CommonSpatialFunction.ST_RELATE.getKey(), new STRelateFunction( typeRegistry ) );

		map.put(
				CommonSpatialFunction.ST_TOUCHES.getKey(),
				new SDORelateFunction( List.of( "TOUCH" ), typeRegistry )
		);

		map.put(
				CommonSpatialFunction.ST_WITHIN.getKey(),
				new SDORelateFunction( List.of( "INSIDE", "COVEREDBY" ), typeRegistry )
		);

		map.put(
				CommonSpatialFunction.ST_EQUALS.getKey(),
				new SDORelateFunction( List.of( "EQUAL" ), typeRegistry )
		);

		map.put(
				CommonSpatialFunction.ST_DISTANCE.getKey(),
				new NamedSqmFunctionDescriptor(
						"SDO_GEOM.SDO_DISTANCE",
						true,
						StandardArgumentsValidators.exactly( 2 ),
						StandardFunctionReturnTypeResolvers.invariant( typeRegistry.resolve( StandardBasicTypes.DOUBLE ) )
				)
		);

		map.put(
				CommonSpatialFunction.ST_BUFFER.getKey(),
				new NamedSqmFunctionDescriptor(
						"SDO_GEOM.SDO_BUFFER",
						true,
						StandardArgumentsValidators.exactly( 2 ),
						StandardFunctionReturnTypeResolvers.useFirstNonNull()
				)
		);

		map.put(
				CommonSpatialFunction.ST_CONVEXHULL.getKey(),
				new NamedSqmFunctionDescriptor(
						"SDO_GEOM.SDO_CONVEXHULL",
						true,
						StandardArgumentsValidators.exactly( 1 ),
						StandardFunctionReturnTypeResolvers.useFirstNonNull()
				)
		);

		map.put(
				CommonSpatialFunction.ST_DIFFERENCE.getKey(),
				new NamedSqmFunctionDescriptor(
						"SDO_GEOM.SDO_DIFFERENCE",
						true,
						StandardArgumentsValidators.exactly( 2 ),
						StandardFunctionReturnTypeResolvers.useFirstNonNull()
				)
		);

		map.put(
				CommonSpatialFunction.ST_INTERSECTION.getKey(),
				new NamedSqmFunctionDescriptor(
						"SDO_GEOM.SDO_INTERSECTION",
						true,
						StandardArgumentsValidators.exactly( 2 ),
						StandardFunctionReturnTypeResolvers.useFirstNonNull()
				)
		);

		map.put(
				CommonSpatialFunction.ST_SYMDIFFERENCE.getKey(),
				new NamedSqmFunctionDescriptor(
						"SDO_GEOM.SDO_XOR",
						true,
						StandardArgumentsValidators.exactly( 2 ),
						StandardFunctionReturnTypeResolvers.useFirstNonNull()
				)
		);

		map.put(
				CommonSpatialFunction.ST_UNION.getKey(),
				new NamedSqmFunctionDescriptor(
						"SDO_GEOM.SDO_UNION",
						true,
						StandardArgumentsValidators.exactly( 2 ),
						StandardFunctionReturnTypeResolvers.useFirstNonNull()
				)
		);

	}
}
