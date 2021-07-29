/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial;

import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;

/**
 * TODO -- documentation
 */
public enum CommonSpatialFunction {

	ST_ASTEXT( FunctionKey.apply( "st_astext", "astext" ), StandardBasicTypes.STRING ),
	ST_GEOMETRYTYPE( FunctionKey.apply( "st_geometrytype", "geometrytype" ), StandardBasicTypes.STRING ),
	ST_DIMENSION( FunctionKey.apply( "st_dimension", "dimension" ), StandardBasicTypes.INTEGER ),
	ST_SRID( FunctionKey.apply("st_srid", "srid"), StandardBasicTypes.INTEGER),
	ST_ENVELOPE( FunctionKey.apply("st_envelope", "envelope"))
	;


	private final FunctionKey key;
	private final BasicType<?> ReturnType;
	private final boolean spatialReturnType;

	CommonSpatialFunction(FunctionKey key, BasicType<?> returnType) {
		this.key = key;
		ReturnType = returnType;
		spatialReturnType = false;
	}

	CommonSpatialFunction(FunctionKey key) {
		this.key = key;
		ReturnType = null;
		spatialReturnType = true;
	}


	public FunctionKey getKey() {
		return key;
	}

	public BasicType<?> getReturnType() {
		return ReturnType;
	}

	public boolean returnsGeometry() {
		return spatialReturnType;
	}
}
