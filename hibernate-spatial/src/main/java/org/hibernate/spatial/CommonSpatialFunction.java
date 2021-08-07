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
	;


	private final FunctionKey key;
	private final BasicType<?> ReturnType;
	private final boolean spatialReturnType;
	private final int numArgs;

	CommonSpatialFunction(FunctionKey key, int numArgs, BasicType<?> returnType) {
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

	public BasicType<?> getReturnType() {
		return ReturnType;
	}

	public boolean returnsGeometry() {
		return spatialReturnType;
	}

	public int getNumArgs() {
		return numArgs;
	}
}
