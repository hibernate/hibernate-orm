/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA, Geodan IT b.v.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.hibernate.spatial.dialect.h2geodb;

import org.hibernate.HibernateException;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.spatial.*;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * Extends the H2Dialect by also including information on spatial functions.
 *
 * @author Jan Boonen, Geodan IT b.v.
 */
public class GeoDBDialect extends H2Dialect implements SpatialDialect {


	/**
	 * Constructor. Registers OGC simple feature functions (see
	 * http://portal.opengeospatial.org/files/?artifact_id=829 for details).
	 * <p/>
	 * Note for the registerfunction method: it registers non-standard database
	 * functions: first argument is the internal (OGC standard) function name,
	 * second the name as it occurs in the spatial dialect
	 */
	public GeoDBDialect() {
		super();

		// Register Geometry column type
		registerColumnType(java.sql.Types.ARRAY, "BLOB");

		// Register functions that operate on spatial types
		registerFunction("dimension", new StandardSQLFunction("ST_Dimension",
				StandardBasicTypes.INTEGER));
		registerFunction("geometrytype", new StandardSQLFunction(
				"GeometryType", StandardBasicTypes.STRING));
		registerFunction("srid", new StandardSQLFunction("ST_SRID",
				StandardBasicTypes.INTEGER));
		registerFunction("envelope", new StandardSQLFunction("ST_Envelope",
				GeometryType.INSTANCE));
		registerFunction("astext", new StandardSQLFunction("ST_AsText",
				StandardBasicTypes.STRING));
		registerFunction("asbinary", new StandardSQLFunction("ST_AsEWKB",
				StandardBasicTypes.BINARY));
		registerFunction("isempty", new StandardSQLFunction("ST_IsEmpty",
				StandardBasicTypes.BOOLEAN));
		registerFunction("issimple", new StandardSQLFunction("ST_IsSimple",
				StandardBasicTypes.BOOLEAN));
		registerFunction("boundary", new StandardSQLFunction("ST_Boundary",
				GeometryType.INSTANCE));

		// Register functions for spatial relation constructs
		registerFunction("overlaps", new StandardSQLFunction("ST_Overlaps",
				StandardBasicTypes.BOOLEAN));
		registerFunction("intersects", new StandardSQLFunction("ST_Intersects",
				StandardBasicTypes.BOOLEAN));
		registerFunction("equals", new StandardSQLFunction("ST_Equals",
				StandardBasicTypes.BOOLEAN));
		registerFunction("contains", new StandardSQLFunction("ST_Contains",
				StandardBasicTypes.BOOLEAN));
		registerFunction("crosses", new StandardSQLFunction("ST_Crosses",
				StandardBasicTypes.BOOLEAN));
		registerFunction("disjoint", new StandardSQLFunction("ST_Disjoint",
				StandardBasicTypes.BOOLEAN));
		registerFunction("touches", new StandardSQLFunction("ST_Touches",
				StandardBasicTypes.BOOLEAN));
		registerFunction("within", new StandardSQLFunction("ST_Within",
				StandardBasicTypes.BOOLEAN));
		registerFunction("relate", new StandardSQLFunction("ST_Relate",
				StandardBasicTypes.BOOLEAN));
		// register the spatial analysis functions
		registerFunction("distance", new StandardSQLFunction("ST_Distance",
				StandardBasicTypes.DOUBLE));
		registerFunction("buffer", new StandardSQLFunction("ST_Buffer",
				GeometryType.INSTANCE));
		registerFunction("convexhull", new StandardSQLFunction("ST_ConvexHull",
				GeometryType.INSTANCE));
		registerFunction("difference", new StandardSQLFunction("ST_Difference",
				GeometryType.INSTANCE));
		registerFunction("intersection", new StandardSQLFunction("ST_Intersection",
				GeometryType.INSTANCE));
		registerFunction("symdifference", new StandardSQLFunction("ST_SymDifference",
				GeometryType.INSTANCE));
		registerFunction("geomunion", new StandardSQLFunction("ST_Union",
				GeometryType.INSTANCE));


		registerFunction("dwithin", new StandardSQLFunction("ST_DWithin",
				StandardBasicTypes.BOOLEAN));

	}

	//TODO the getTypeName() override is necessary in the absence of HHH-6074

	/**
	 * Get the name of the database type associated with the given
	 * {@link java.sql.Types} typecode with the given storage specification
	 * parameters. In the case of typecode == 3000, it returns this dialect's spatial type which is
	 * <code>GEOMETRY</code>.
	 *
	 * @param code	  The {@link java.sql.Types} typecode
	 * @param length	The datatype length
	 * @param precision The datatype precision
	 * @param scale	 The datatype scale
	 * @return
	 * @throws org.hibernate.HibernateException
	 *
	 */
	@Override
	public String getTypeName(int code, long length, int precision, int scale) throws HibernateException {
		if (code == 3000) return "GEOMETRY";
		return super.getTypeName(code, length, precision, scale);
	}

	@Override
	public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		if (sqlTypeDescriptor instanceof GeometrySqlTypeDescriptor) {
			return GeoDBGeometryTypeDescriptor.INSTANCE;
		}
		return super.remapSqlTypeDescriptor(sqlTypeDescriptor);
	}

	/* (non-Javadoc)
		  * @see org.hibernatespatial.SpatialDialect#getSpatialAggregateSQL(java.lang.String, int)
		  */

	public String getSpatialAggregateSQL(String columnName, int aggregation) {
		switch (aggregation) {
// NOT YET AVAILABLE IN GEODB
//		case SpatialAggregate.EXTENT:
//			StringBuilder stbuf = new StringBuilder();
//			stbuf.append("extent(").append(columnName).append(")");
//			return stbuf.toString();
			default:
				throw new IllegalArgumentException("Aggregations of type "
						+ aggregation + " are not supported by this dialect");
		}
	}

	public String getDWithinSQL(String columnName) {
		return "ST_DWithin(" + columnName + ",?,?)";
	}

	public String getHavingSridSQL(String columnName) {
		return "( ST_srid(" + columnName + ") = ?)";
	}

	public String getIsEmptySQL(String columnName, boolean isEmpty) {
		String emptyExpr = " ST_IsEmpty(" + columnName + ") ";
		return isEmpty ? emptyExpr : "( NOT " + emptyExpr + ")";
	}

	/* (non-Javadoc)
		  * @see org.hibernatespatial.SpatialDialect#getSpatialFilterExpression(java.lang.String)
		  */
	public String getSpatialFilterExpression(String columnName) {
		return "(" + columnName + " && ? ) ";
	}

	/* (non-Javadoc)
		  * @see org.hibernatespatial.SpatialDialect#getSpatialRelateSQL(java.lang.String, int, boolean)
		  */

	public String getSpatialRelateSQL(String columnName, int spatialRelation) {
		switch (spatialRelation) {
			case SpatialRelation.WITHIN:
				return " ST_Within(" + columnName + ", ?)";
			case SpatialRelation.CONTAINS:
				return " ST_Contains(" + columnName + ", ?)";
			case SpatialRelation.CROSSES:
				return " ST_Crosses(" + columnName + ", ?)";
			case SpatialRelation.OVERLAPS:
				return " ST_Overlaps(" + columnName + ", ?)";
			case SpatialRelation.DISJOINT:
				return " ST_Disjoint(" + columnName + ", ?)";
			case SpatialRelation.INTERSECTS:
				return " ST_Intersects(" + columnName + ", ?)";
			case SpatialRelation.TOUCHES:
				return " ST_Touches(" + columnName + ", ?)";
			case SpatialRelation.EQUALS:
				return " ST_Equals(" + columnName + ", ?)";
			default:
				throw new IllegalArgumentException(
						"Spatial relation is not known by this dialect");
		}
	}

	/* (non-Javadoc)
		  * @see org.hibernatespatial.SpatialDialect#getDbGeometryTypeName()
		  */

	public String getDbGeometryTypeName() {
		return "GEOM";
	}

	/* (non-Javadoc)
		  * @see org.hibernatespatial.SpatialDialect#isTwoPhaseFiltering()
		  */

	public boolean isTwoPhaseFiltering() {
		return false;
	}

	public boolean supportsFiltering() {
		return false;
	}

	public boolean supports(SpatialFunction function) {
		if (function == SpatialFunction.difference) return false;
		return (getFunctions().get(function.toString()) != null);
	}

}
