/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.hana;

import java.sql.Types;
import java.util.List;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.HANAColumnStoreDialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.ConfigurationService.Converter;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.GeolatteGeometryJavaTypeDescriptor;
import org.hibernate.spatial.GeolatteGeometryType;
import org.hibernate.spatial.JTSGeometryJavaTypeDescriptor;
import org.hibernate.spatial.JTSGeometryType;
import org.hibernate.spatial.SpatialAggregate;
import org.hibernate.spatial.SpatialDialect;
import org.hibernate.spatial.SpatialFunction;
import org.hibernate.spatial.SpatialRelation;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

public class HANASpatialDialect extends HANAColumnStoreDialect implements SpatialDialect {

	private static final long serialVersionUID = -432631517465714911L;

	private static final String DETERMINE_CRS_ID_FROM_DATABASE_PARAMETER_NAME = "hibernate.spatial.dialect.hana.determine_crs_id_from_database";

	public HANASpatialDialect() {
		registerColumnType( HANAGeometryTypeDescriptor.INSTANCE.getSqlType(), "ST_GEOMETRY" );
		registerColumnType( HANAPointTypeDescriptor.INSTANCE.getSqlType(), "ST_POINT" );

		registerHibernateType( Types.OTHER, new GeolatteGeometryType( HANAGeometryTypeDescriptor.INSTANCE ).getName() );

		/*
		 * Hibernate Spatial functions
		 */
		registerFunction(
				SpatialFunction.asbinary.name(),
				new HANASpatialFunction( "ST_AsBinary", StandardBasicTypes.MATERIALIZED_BLOB, false ) );
		registerFunction(
				SpatialFunction.astext.name(),
				new HANASpatialFunction( "ST_AsText", StandardBasicTypes.MATERIALIZED_CLOB, false ) );
		registerFunction( SpatialFunction.boundary.name(), new HANASpatialFunction( "ST_Boundary", false ) );
		registerFunction( SpatialFunction.buffer.name(), new HANASpatialFunction( "ST_Buffer", false ) );
		registerFunction(
				SpatialFunction.contains.name(),
				new HANASpatialFunction( "ST_Contains", StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction( SpatialFunction.convexhull.name(), new HANASpatialFunction( "ST_ConvexHull", false ) );
		registerFunction(
				SpatialFunction.crosses.name(),
				new HANASpatialFunction( "ST_Crosses", StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction( SpatialFunction.difference.name(), new HANASpatialFunction( "ST_Difference", true ) );
		registerFunction(
				SpatialFunction.dimension.name(),
				new HANASpatialFunction( "ST_Dimension", StandardBasicTypes.INTEGER, false ) );
		registerFunction(
				SpatialFunction.disjoint.name(),
				new HANASpatialFunction( "ST_Disjoint", StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction(
				SpatialFunction.distance.name(),
				new HANASpatialFunction( "ST_Distance", StandardBasicTypes.DOUBLE, true ) );
		registerFunction(
				SpatialFunction.dwithin.name(),
				new HANASpatialFunction( "ST_WithinDistance", StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction( SpatialFunction.envelope.name(), new HANASpatialFunction( "ST_Envelope", true ) );
		registerFunction(
				SpatialFunction.equals.name(),
				new HANASpatialFunction( "ST_Equals", StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction( SpatialFunction.extent.name(), new HANASpatialAggregate( "ST_EnvelopeAggr" ) );
		registerFunction(
				SpatialFunction.geometrytype.name(),
				new HANASpatialFunction( "ST_GeometryType", StandardBasicTypes.STRING, false ) );
		registerFunction( SpatialFunction.geomunion.name(), new HANASpatialFunction( "ST_Union", true ) );
		registerFunction( SpatialFunction.intersection.name(), new HANASpatialFunction( "ST_Intersection", true ) );
		registerFunction(
				SpatialFunction.intersects.name(),
				new HANASpatialFunction( "ST_Intersects", StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction(
				SpatialFunction.isempty.name(),
				new HANASpatialFunction( "ST_IsEmpty", StandardBasicTypes.NUMERIC_BOOLEAN, false ) );
		registerFunction(
				SpatialFunction.issimple.name(),
				new HANASpatialFunction( "ST_IsSimple", StandardBasicTypes.NUMERIC_BOOLEAN, false ) );
		registerFunction(
				SpatialFunction.overlaps.name(),
				new HANASpatialFunction( "ST_Overlaps", StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction(
				SpatialFunction.relate.name(),
				new HANASpatialFunction( "ST_Relate", StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction(
				SpatialFunction.srid.name(),
				new HANASpatialFunction( "ST_SRID", StandardBasicTypes.INTEGER, false ) );
		registerFunction( SpatialFunction.symdifference.name(), new HANASpatialFunction( "ST_SymDifference", true ) );
		registerFunction(
				SpatialFunction.touches.name(),
				new HANASpatialFunction( "ST_Touches", StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction( SpatialFunction.transform.name(), new HANASpatialFunction( "ST_Transform", false ) );
		registerFunction(
				SpatialFunction.within.name(),
				new HANASpatialFunction( "ST_Within", StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction(
				SpatialFunction.filter.name(),
				new FilterFunction() );

		/*
		 * Additional HANA functions
		 */
		registerFunction( HANASpatialFunctions.alphashape.name(),
				new HANASpatialFunction( HANASpatialFunctions.alphashape.getFunctionName(), false ) );
		registerFunction( HANASpatialFunctions.area.name(),
				new HANASpatialFunction( HANASpatialFunctions.area.getFunctionName(), StandardBasicTypes.DOUBLE, false ) );
		registerFunction( HANASpatialFunctions.asewkb.name(),
				new HANASpatialFunction( HANASpatialFunctions.asewkb.getFunctionName(), StandardBasicTypes.MATERIALIZED_BLOB, false ) );
		registerFunction( HANASpatialFunctions.asewkt.name(),
				new HANASpatialFunction( HANASpatialFunctions.asewkt.getFunctionName(), StandardBasicTypes.MATERIALIZED_CLOB, false ) );
		registerFunction( HANASpatialFunctions.asgeojson.name(),
				new HANASpatialFunction( HANASpatialFunctions.asgeojson.getFunctionName(), StandardBasicTypes.MATERIALIZED_CLOB, false ) );
		registerFunction( HANASpatialFunctions.assvg.name(),
				new HANASpatialFunction( HANASpatialFunctions.assvg.getFunctionName(), StandardBasicTypes.MATERIALIZED_CLOB, false ) );
		registerFunction( HANASpatialFunctions.assvgaggr.name(),
				new HANASpatialFunction( HANASpatialFunctions.assvgaggr.getFunctionName(), StandardBasicTypes.MATERIALIZED_CLOB, false, true ) );
		registerFunction( HANASpatialFunctions.aswkb.name(),
				new HANASpatialFunction( HANASpatialFunctions.aswkb.getFunctionName(), StandardBasicTypes.MATERIALIZED_BLOB, false ) );
		registerFunction( HANASpatialFunctions.aswkt.name(),
				new HANASpatialFunction( HANASpatialFunctions.aswkt.getFunctionName(), StandardBasicTypes.MATERIALIZED_CLOB, false ) );
		registerFunction( HANASpatialFunctions.centroid.name(),
				new HANASpatialFunction( HANASpatialFunctions.centroid.getFunctionName(), false ) );
		registerFunction( HANASpatialFunctions.convexhullaggr.name(),
				new HANASpatialFunction( HANASpatialFunctions.convexhullaggr.getFunctionName(), true, true ) );
		registerFunction(
				HANASpatialFunctions.coorddim.name(),
				new HANASpatialFunction( HANASpatialFunctions.coorddim.getFunctionName(), StandardBasicTypes.INTEGER, false ) );
		registerFunction(
				HANASpatialFunctions.coveredby.name(),
				new HANASpatialFunction( HANASpatialFunctions.coveredby.getFunctionName(), StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction(
				HANASpatialFunctions.covers.name(),
				new HANASpatialFunction( HANASpatialFunctions.covers.getFunctionName(), StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction(
				HANASpatialFunctions.endpoint.name(),
				new HANASpatialFunction( HANASpatialFunctions.endpoint.getFunctionName(), false ) );
		registerFunction( HANASpatialFunctions.envelopeaggr.name(),
				new HANASpatialFunction( HANASpatialFunctions.envelopeaggr.getFunctionName(), true, true ) );
		registerFunction(
				HANASpatialFunctions.exteriorring.name(),
				new HANASpatialFunction( HANASpatialFunctions.exteriorring.getFunctionName(), false ) );
		registerFunction( HANASpatialFunctions.geomfromewkb.name(),
				new HANASpatialFunction( HANASpatialFunctions.geomfromewkb.getFunctionName(), false, true ) );
		registerFunction( HANASpatialFunctions.geomfromewkt.name(),
				new HANASpatialFunction( HANASpatialFunctions.geomfromewkt.getFunctionName(), false, true ) );
		registerFunction( HANASpatialFunctions.geomfromtext.name(),
				new HANASpatialFunction( HANASpatialFunctions.geomfromtext.getFunctionName(), false, true ) );
		registerFunction( HANASpatialFunctions.geomfromwkb.name(),
				new HANASpatialFunction( HANASpatialFunctions.geomfromwkb.getFunctionName(), false, true ) );
		registerFunction( HANASpatialFunctions.geomfromwkt.name(),
				new HANASpatialFunction( HANASpatialFunctions.geomfromwkt.getFunctionName(), false, true ) );
		registerFunction(
				HANASpatialFunctions.geometryn.name(),
				new HANASpatialFunction( HANASpatialFunctions.geometryn.getFunctionName(), false ) );
		registerFunction(
				HANASpatialFunctions.interiorringn.name(),
				new HANASpatialFunction( HANASpatialFunctions.interiorringn.getFunctionName(), false ) );
		registerFunction( HANASpatialFunctions.intersectionaggr.name(),
				new HANASpatialFunction( HANASpatialFunctions.intersectionaggr.getFunctionName(), true, true ) );
		registerFunction(
				HANASpatialFunctions.intersectsrect.name(),
				new HANASpatialFunction( HANASpatialFunctions.intersectsrect.getFunctionName(), StandardBasicTypes.NUMERIC_BOOLEAN,
						new boolean[]{ true, true } ) );
		registerFunction(
				HANASpatialFunctions.is3d.name(),
				new HANASpatialFunction( HANASpatialFunctions.is3d.getFunctionName(), StandardBasicTypes.NUMERIC_BOOLEAN, false ) );
		registerFunction(
				HANASpatialFunctions.isclosed.name(),
				new HANASpatialFunction( HANASpatialFunctions.isclosed.getFunctionName(), StandardBasicTypes.NUMERIC_BOOLEAN, false ) );
		registerFunction(
				HANASpatialFunctions.ismeasured.name(),
				new HANASpatialFunction( HANASpatialFunctions.ismeasured.getFunctionName(), StandardBasicTypes.NUMERIC_BOOLEAN, false ) );
		registerFunction(
				HANASpatialFunctions.isring.name(),
				new HANASpatialFunction( HANASpatialFunctions.isring.getFunctionName(), StandardBasicTypes.NUMERIC_BOOLEAN, false ) );
		registerFunction(
				HANASpatialFunctions.isvalid.name(),
				new HANASpatialFunction( HANASpatialFunctions.isvalid.getFunctionName(), StandardBasicTypes.NUMERIC_BOOLEAN, false ) );
		registerFunction(
				HANASpatialFunctions.length.name(),
				new HANASpatialFunction( HANASpatialFunctions.length.getFunctionName(), StandardBasicTypes.DOUBLE, false ) );
		registerFunction(
				HANASpatialFunctions.m.name(),
				new HANASpatialFunction( HANASpatialFunctions.m.getFunctionName(), StandardBasicTypes.DOUBLE, false ) );
		registerFunction(
				HANASpatialFunctions.mmax.name(),
				new HANASpatialFunction( HANASpatialFunctions.mmax.getFunctionName(), StandardBasicTypes.DOUBLE, false ) );
		registerFunction(
				HANASpatialFunctions.mmin.name(),
				new HANASpatialFunction( HANASpatialFunctions.mmin.getFunctionName(), StandardBasicTypes.DOUBLE, false ) );
		registerFunction(
				HANASpatialFunctions.numgeometries.name(),
				new HANASpatialFunction( HANASpatialFunctions.numgeometries.getFunctionName(), StandardBasicTypes.INTEGER, false ) );
		registerFunction(
				HANASpatialFunctions.numinteriorring.name(),
				new HANASpatialFunction( HANASpatialFunctions.numinteriorring.getFunctionName(), StandardBasicTypes.INTEGER, false ) );
		registerFunction(
				HANASpatialFunctions.numinteriorrings.name(),
				new HANASpatialFunction( HANASpatialFunctions.numinteriorrings.getFunctionName(), StandardBasicTypes.INTEGER, false ) );
		registerFunction(
				HANASpatialFunctions.numpoints.name(),
				new HANASpatialFunction( HANASpatialFunctions.numpoints.getFunctionName(), StandardBasicTypes.INTEGER, false ) );
		registerFunction(
				HANASpatialFunctions.orderingequals.name(),
				new HANASpatialFunction( HANASpatialFunctions.orderingequals.getFunctionName(), StandardBasicTypes.NUMERIC_BOOLEAN, true ) );
		registerFunction(
				HANASpatialFunctions.perimeter.name(),
				new HANASpatialFunction( HANASpatialFunctions.perimeter.getFunctionName(), StandardBasicTypes.DOUBLE, false ) );
		registerFunction(
				HANASpatialFunctions.pointonsurface.name(),
				new HANASpatialFunction( HANASpatialFunctions.pointonsurface.getFunctionName(), false ) );
		registerFunction(
				HANASpatialFunctions.pointn.name(),
				new HANASpatialFunction( HANASpatialFunctions.pointn.getFunctionName(), false ) );
		registerFunction(
				HANASpatialFunctions.snaptogrid.name(),
				new HANASpatialFunction( HANASpatialFunctions.snaptogrid.getFunctionName(), false ) );
		registerFunction(
				HANASpatialFunctions.startpoint.name(),
				new HANASpatialFunction( HANASpatialFunctions.startpoint.getFunctionName(), false ) );
		registerFunction( HANASpatialFunctions.unionaggr.name(),
				new HANASpatialFunction( HANASpatialFunctions.unionaggr.getFunctionName(), true, true ) );
		registerFunction(
				HANASpatialFunctions.x.name(),
				new HANASpatialFunction( HANASpatialFunctions.x.getFunctionName(), StandardBasicTypes.DOUBLE, false ) );
		registerFunction(
				HANASpatialFunctions.xmax.name(),
				new HANASpatialFunction( HANASpatialFunctions.xmax.getFunctionName(), StandardBasicTypes.DOUBLE, false ) );
		registerFunction(
				HANASpatialFunctions.xmin.name(),
				new HANASpatialFunction( HANASpatialFunctions.xmin.getFunctionName(), StandardBasicTypes.DOUBLE, false ) );
		registerFunction(
				HANASpatialFunctions.y.name(),
				new HANASpatialFunction( HANASpatialFunctions.y.getFunctionName(), StandardBasicTypes.DOUBLE, false ) );
		registerFunction(
				HANASpatialFunctions.ymax.name(),
				new HANASpatialFunction( HANASpatialFunctions.ymax.getFunctionName(), StandardBasicTypes.DOUBLE, false ) );
		registerFunction(
				HANASpatialFunctions.ymin.name(),
				new HANASpatialFunction( HANASpatialFunctions.ymin.getFunctionName(), StandardBasicTypes.DOUBLE, false ) );
		registerFunction(
				HANASpatialFunctions.z.name(),
				new HANASpatialFunction( HANASpatialFunctions.z.getFunctionName(), StandardBasicTypes.DOUBLE, false ) );
		registerFunction(
				HANASpatialFunctions.zmax.name(),
				new HANASpatialFunction( HANASpatialFunctions.zmax.getFunctionName(), StandardBasicTypes.DOUBLE, false ) );
		registerFunction(
				HANASpatialFunctions.zmin.name(),
				new HANASpatialFunction( HANASpatialFunctions.zmin.getFunctionName(), StandardBasicTypes.DOUBLE, false ) );
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

		typeContributions.contributeJavaTypeDescriptor( GeolatteGeometryJavaTypeDescriptor.INSTANCE );
		typeContributions.contributeJavaTypeDescriptor( JTSGeometryJavaTypeDescriptor.INSTANCE );
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

	private static class FilterFunction extends HANASpatialFunction {

		public FilterFunction() {
			super( "ST_IntersectsFilter", StandardBasicTypes.NUMERIC_BOOLEAN, true );
		}

		@Override
		public String render(
				Type firstArgumentType, List arguments, SessionFactoryImplementor sessionFactory) {
			return super.render( firstArgumentType, arguments, sessionFactory ) + " = 1";
		}
	}
}
