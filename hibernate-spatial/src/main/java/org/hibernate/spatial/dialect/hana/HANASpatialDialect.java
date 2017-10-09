/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.hana;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.HANAColumnStoreDialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.ConfigurationService.Converter;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.GeolatteGeometryType;
import org.hibernate.spatial.JTSGeometryType;
import org.hibernate.spatial.SpatialAggregate;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.SpatialRelation;
import org.hibernate.type.StandardBasicTypes;

public class HANASpatialDialect extends HANAColumnStoreDialect implements SpatialDialect {

	private static final long serialVersionUID = -432631517465714911L;

	private static final String DETERMINE_CRS_ID_FROM_DATABASE_PARAMETER_NAME = "hibernate.spatial.dialect.hana.determine_crs_id_from_database";

	public HANASpatialDialect() {
		registerColumnType( HANAGeometryTypeDescriptor.INSTANCE.getSqlType(), "ST_GEOMETRY" );
		registerColumnType( HANAPointTypeDescriptor.INSTANCE.getSqlType(), "ST_POINT" );

		registerFunction( SpatialFunction.asbinary.name(),
				new HANASpatialFunction( "ST_AsBinary", StandardBasicTypes.MATERIALIZED_BLOB, false ) );
		registerFunction( SpatialFunction.astext.name(),
				new HANASpatialFunction( "ST_AsText", StandardBasicTypes.MATERIALIZED_CLOB, false ) );
		registerFunction( SpatialFunction.boundary.name(), new HANASpatialFunction( "ST_Boundary", false ) );
		registerFunction( SpatialFunction.buffer.name(), new HANASpatialFunction( "ST_Buffer", false ) );
		registerFunction( SpatialFunction.contains.name(),
				new HANASpatialFunction( "ST_Contains", StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction( SpatialFunction.convexhull.name(), new HANASpatialFunction( "ST_ConvexHull", false ) );
		registerFunction( SpatialFunction.crosses.name(),
				new HANASpatialFunction( "ST_Crosses", StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction( SpatialFunction.difference.name(), new HANASpatialFunction( "ST_Difference", true ) );
		registerFunction( SpatialFunction.dimension.name(),
				new HANASpatialFunction( "ST_Dimension ", StandardBasicTypes.INTEGER, false ) );
		registerFunction( SpatialFunction.disjoint.name(),
				new HANASpatialFunction( "ST_Disjoint", StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction( SpatialFunction.distance.name(),
				new HANASpatialFunction( "ST_Distance", StandardBasicTypes.DOUBLE, true ) );
		registerFunction( SpatialFunction.dwithin.name(),
				new HANASpatialFunction( "ST_WithinDistance", StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction( SpatialFunction.envelope.name(), new HANASpatialFunction( "ST_Envelope", true ) );
		registerFunction( SpatialFunction.equals.name(),
				new HANASpatialFunction( "ST_Equals", StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction( SpatialFunction.extent.name(), new HANASpatialAggregate( "ST_EnvelopeAggr" ) );
		registerFunction( SpatialFunction.geometrytype.name(),
				new HANASpatialFunction( "ST_GeometryType", StandardBasicTypes.STRING, false ) );
		registerFunction( SpatialFunction.geomunion.name(), new HANASpatialFunction( "ST_Union", true ) );
		registerFunction( SpatialFunction.intersection.name(), new HANASpatialFunction( "ST_Intersection", true ) );
		registerFunction( SpatialFunction.intersects.name(),
				new HANASpatialFunction( "ST_Intersects", StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction( SpatialFunction.isempty.name(),
				new HANASpatialFunction( "ST_IsEmpty", StandardBasicTypes.NUMERIC_BOOLEAN, false ) );
		registerFunction( SpatialFunction.issimple.name(),
				new HANASpatialFunction( "ST_IsSimple", StandardBasicTypes.NUMERIC_BOOLEAN, false ) );
		registerFunction( SpatialFunction.overlaps.name(),
				new HANASpatialFunction( "ST_Overlaps", StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction( SpatialFunction.relate.name(),
				new HANASpatialFunction( "ST_Relate", StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction( SpatialFunction.srid.name(),
				new HANASpatialFunction( "ST_SRID", StandardBasicTypes.INTEGER, false ) );
		registerFunction( SpatialFunction.symdifference.name(), new HANASpatialFunction( "ST_SymDifference", true ) );
		registerFunction( SpatialFunction.touches.name(),
				new HANASpatialFunction( "ST_Touches", StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction( SpatialFunction.transform.name(), new HANASpatialFunction( "ST_Transform", false ) );
		registerFunction( SpatialFunction.within.name(),
				new HANASpatialFunction( "ST_Within", StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
	}

	@Override
	public String getSpatialRelateSQL(String columnName, int spatialRelation) {
		switch ( spatialRelation ) {
			case SpatialRelation.WITHIN:
				return columnName + ".ST_Within(ST_GeomFromEWKB(?)) = 1";
			case SpatialRelation.CONTAINS:
				return columnName + ".ST_Contains(ST_GeomFromEWKB(?)) = 1";
			case SpatialRelation.CROSSES:
				return columnName + ".ST_Crosses(ST_GeomFromEWKB(?)) = 1";
			case SpatialRelation.OVERLAPS:
				return columnName + ".ST_Overlaps(ST_GeomFromEWKB(?)) = 1";
			case SpatialRelation.DISJOINT:
				return columnName + ".ST_Disjoint(ST_GeomFromEWKB(?)) = 1";
			case SpatialRelation.INTERSECTS:
				return columnName + ".ST_Intersects(ST_GeomFromEWKB(?)) = 1";
			case SpatialRelation.TOUCHES:
				return columnName + ".ST_Touches(ST_GeomFromEWKB(?)) = 1";
			case SpatialRelation.EQUALS:
				return columnName + ".ST_Equals(ST_GeomFromEWKB(?)) = 1";
			case SpatialRelation.FILTER:
				return columnName + ".ST_IntersectsFilter(ST_GeomFromEWKB(?)) = 1";
			default:
				throw new IllegalArgumentException( "Spatial relation is not known by this dialect" );
		}
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		final ConfigurationService configurationService = serviceRegistry.getService( ConfigurationService.class );
		boolean determineCrsIdFromDatabase = configurationService.getSetting(
				DETERMINE_CRS_ID_FROM_DATABASE_PARAMETER_NAME,
				new Converter<Boolean>() {

					@Override
					public Boolean convert(Object value) {
						return Boolean.valueOf( value.toString() );
					}

				},
				Boolean.FALSE ).booleanValue();

		if ( determineCrsIdFromDatabase ) {
			typeContributions.contributeType( new GeolatteGeometryType( HANAGeometryTypeDescriptor.CRS_LOADING_INSTANCE ) );
			typeContributions.contributeType( new JTSGeometryType( HANAGeometryTypeDescriptor.CRS_LOADING_INSTANCE ) );
		}
		else {
			typeContributions.contributeType( new GeolatteGeometryType( HANAGeometryTypeDescriptor.INSTANCE ) );
			typeContributions.contributeType( new JTSGeometryType( HANAGeometryTypeDescriptor.INSTANCE ) );
		}

	}

	@Override
	public String getSpatialFilterExpression(String columnName) {
		return columnName + ".ST_IntersectsFilter(ST_GeomFromEWKB(?)) = 1";
	}

	@Override
	public String getSpatialAggregateSQL(String columnName, int aggregation) {
		switch ( aggregation ) {
			case SpatialAggregate.EXTENT:
				return "ST_EnvelopeAggr(" + columnName + ")";
			default:
				throw new IllegalArgumentException( "The aggregate type [" + aggregation + "] is not known by this dialect" );
		}
	}

	@Override
	public String getDWithinSQL(String columnName) {
		return columnName + ".ST_WithinDistance(ST_GeomFromEWKB(?), ?) = 1";
	}

	@Override
	public String getHavingSridSQL(String columnName) {
		return columnName + ".ST_SRID() = ?";
	}

	@Override
	public String getIsEmptySQL(String columnName, boolean isEmpty) {
		return columnName + ".ST_IsEmpty() = " + ( isEmpty ? 1 : 0 );
	}

	@Override
	public boolean supportsFiltering() {
		return true;
	}

	@Override
	public boolean supports(SpatialFunction function) {
		switch ( function ) {
			case asbinary:
				return true;
			case astext:
				return true;
			case boundary:
				return true;
			case buffer:
				return true;
			case contains:
				return true;
			case convexhull:
				return true;
			case crosses:
				return true;
			case difference:
				return true;
			case dimension:
				return true;
			case disjoint:
				return true;
			case distance:
				return true;
			case dwithin:
				return true;
			case envelope:
				return true;
			case equals:
				return true;
			case extent:
				return true;
			case geometrytype:
				return true;
			case geomunion:
				return true;
			case intersection:
				return true;
			case intersects:
				return true;
			case isempty:
				return true;
			case issimple:
				return true;
			case overlaps:
				return true;
			case relate:
				return true;
			case srid:
				return true;
			case symdifference:
				return true;
			case touches:
				return true;
			case transform:
				return true;
			case within:
				return true;
		}
		return false;
	}
}
