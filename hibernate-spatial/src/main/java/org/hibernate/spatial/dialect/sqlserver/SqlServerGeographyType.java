package org.hibernate.spatial.dialect.sqlserver;

import org.hibernate.SPI;


import org.hibernate.spatial.GeometryLiteralFormatter;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.codec.db.sqlserver.GeographyDecoders;
import org.geolatte.geom.codec.db.sqlserver.GeographyEncoders;

/**
 * Type descriptor for the SQL Server 2008 Geography type.
 *
 * @author Karel Maesen, Geovise BVBA
 * creation-date: 8/23/11
 */
@SPI
public class SqlServerGeographyType extends AbstractSqlServerGISType {

	/**
	 * An instance of the descrtiptor
	 */
	public static final SqlServerGeographyType INSTANCE = new SqlServerGeographyType();

	@Override
	protected byte[] encode(Geometry<?> geometry) {
		return GeographyEncoders.encode( geometry );
	}

	@Override
	protected Geometry<?> decode(byte[] bytes) {
		return GeographyDecoders.decode( bytes );
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.GEOGRAPHY;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new GeometryLiteralFormatter<>( javaType, Wkt.Dialect.SFA_1_2_1, "geography::STGeomFromText" );
	}

}
