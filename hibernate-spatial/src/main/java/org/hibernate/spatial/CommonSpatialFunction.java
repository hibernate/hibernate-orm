/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial;

import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.StandardBasicTypes;

/**
 * Functions commonly expected in databases, as defined
 * by the SQL/MM specs
 *
 * @author Karel Maesen
 */
public enum CommonSpatialFunction {

	ST_ASTEXT( FunctionKey.apply( "st_astext", "astext" ), 1, StandardBasicTypes.STRING ),
	ST_GEOMETRYTYPE( FunctionKey.apply( "st_geometrytype", "geometrytype" ), 1, StandardBasicTypes.STRING ),
	ST_DIMENSION( FunctionKey.apply( "st_dimension", "dimension" ), 1, StandardBasicTypes.INTEGER ),
	ST_SRID( FunctionKey.apply( "st_srid", "srid" ), 1, StandardBasicTypes.INTEGER ),
	ST_ENVELOPE( FunctionKey.apply( "st_envelope", "envelope" ), 1 ),
	ST_ASBINARY( FunctionKey.apply( "st_asbinary", "asbinary" ), 1, StandardBasicTypes.BINARY ),
	ST_ISEMPTY( FunctionKey.apply( "st_isempty", "isempty" ), 1, StandardBasicTypes.BOOLEAN ),
	ST_ISSIMPLE( FunctionKey.apply( "st_issimple", "issimple" ), 1, StandardBasicTypes.BOOLEAN ),
	ST_BOUNDARY( FunctionKey.apply( "st_boundary", "boundary" ), 1 ),
	ST_OVERLAPS( FunctionKey.apply( "st_overlaps", "overlaps" ), 2, StandardBasicTypes.BOOLEAN ),
	ST_INTERSECTS( FunctionKey.apply( "st_intersects", "intersects" ), 2, StandardBasicTypes.BOOLEAN ),
	ST_EQUALS( FunctionKey.apply( "st_equals", "equals" ), 2, StandardBasicTypes.BOOLEAN ),
	ST_CONTAINS( FunctionKey.apply( "st_contains", "contains" ), 2, StandardBasicTypes.BOOLEAN ),
	ST_CROSSES( FunctionKey.apply( "st_crosses", "crosses" ), 2, StandardBasicTypes.BOOLEAN ),
	ST_DISJOINT( FunctionKey.apply( "st_disjoint", "disjoint" ), 2, StandardBasicTypes.BOOLEAN ),
	ST_TOUCHES( FunctionKey.apply( "st_touches", "touches" ), 2, StandardBasicTypes.BOOLEAN ),
	ST_WITHIN( FunctionKey.apply( "st_within", "within" ), 2, StandardBasicTypes.BOOLEAN ),
	ST_RELATE( FunctionKey.apply( "st_relate", "relate" ), 2, StandardBasicTypes.STRING ),
	ST_DISTANCE( FunctionKey.apply( "st_distance", "distance" ), 2, StandardBasicTypes.DOUBLE ),
	ST_BUFFER( FunctionKey.apply( "st_buffer", "buffer" ), 2 ),
	ST_CONVEXHULL( FunctionKey.apply( "st_convexhull", "convexhull" ), 1 ),
	ST_DIFFERENCE( FunctionKey.apply( "st_difference", "difference" ), 2 ),
	ST_INTERSECTION( FunctionKey.apply( "st_intersection", "intersection" ), 2 ),
	ST_SYMDIFFERENCE( FunctionKey.apply( "st_symdifference", "symdifference" ), 2 ),
	ST_UNION( FunctionKey.apply( "st_union", "geomunion" ), 2 );


	public static enum Type {
		/**
		 * Geometry -> String or Byte[]
		 */
		GEOMETRY_OUTPUT,
		/**
		 * String or Byte Array -> Geometry
		 */
		GEOMETRY_INPUT,
		/**
		 * Geometry [, OBJECT]* -> Geometry
		 */
		CONSTRUCTION,

		/**
		 * Geometry, Geometry, [Geometry]* -> Geometry
		 */
		OVERLAY,
		/**
		 * Geometry, Geometry -> Boolean (or String for st_relate)
		 */
		ANALYSIS,
		/**
		 * Geometry -> Boolean
		 */
		VALIDATION,
		/**
		 * Geometry[, Object]* -> Scalar type
		 */
		INFORMATION,
	}

	private final FunctionKey key;
	private final BasicTypeReference<?> ReturnType;
	private final boolean spatialReturnType;
	private final int numArgs;

	CommonSpatialFunction(FunctionKey key, int numArgs, BasicTypeReference<?> returnType) {
		this.key = key;
		ReturnType = returnType;
		spatialReturnType = false;
		this.numArgs = numArgs;
	}

	CommonSpatialFunction(FunctionKey key, int numArgs) {
		this.key = key;
		ReturnType = null;
		spatialReturnType = true;
		this.numArgs = numArgs;
	}


	public FunctionKey getKey() {
		return key;
	}

	public BasicTypeReference<?> getReturnType() {
		return ReturnType;
	}

	public boolean returnsGeometry() {
		return spatialReturnType;
	}

	public int getNumArgs() {
		return numArgs;
	}

	public Type getType() {
		switch ( this ) {
			case ST_SRID:
			case ST_DIMENSION:
			case ST_GEOMETRYTYPE:
			case ST_DISTANCE:
				return Type.INFORMATION;
			case ST_ASBINARY:
			case ST_ASTEXT:
				return Type.GEOMETRY_OUTPUT;
			case ST_ENVELOPE:
			case ST_BOUNDARY:
			case ST_BUFFER:
			case ST_CONVEXHULL:
				return Type.CONSTRUCTION;
			case ST_DIFFERENCE:
			case ST_INTERSECTION:
			case ST_SYMDIFFERENCE:
			case ST_UNION:
				return Type.OVERLAY;
			case ST_CONTAINS:
			case ST_CROSSES:
			case ST_DISJOINT:
			case ST_INTERSECTS:
			case ST_EQUALS:
			case ST_TOUCHES:
			case ST_WITHIN:
			case ST_OVERLAPS:
			case ST_RELATE:
				return Type.ANALYSIS;
			case ST_ISEMPTY:
			case ST_ISSIMPLE:
				return Type.VALIDATION;
			default:
				throw new IllegalStateException( "The function " + this.getKey().getName() + " is not categorized" );
		}
	}
}
