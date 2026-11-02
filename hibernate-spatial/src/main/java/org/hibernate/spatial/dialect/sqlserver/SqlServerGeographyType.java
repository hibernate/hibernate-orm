package org.hibernate.spatial.dialect.sqlserver;

import org.hibernate.SPI;


import org.hibernate.spatial.GeometryLiteralFormatter;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.Position;
import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.crs.CoordinateReferenceSystem;
import org.geolatte.geom.crs.trans.CoordinateOperation;
import org.geolatte.geom.crs.trans.TransformOperation;
import org.geolatte.geom.crs.trans.TransformVisitor;

/**
 * Type descriptor for the SQL Server 2008 Geography type.
 * <p>
 * The serialized form of a {@code geography} value holds the latitude of each point before its longitude, while a
 * {@link Geometry} holds its x, the longitude, before its y, the latitude; the descriptor swaps the two axes of every
 * position on each value it binds or extracts. The Well-Known Text of a literal is longitude first, as SQL Server's
 * {@code STGeomFromText} takes it.
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
	public int getDefaultSqlTypeCode() {
		return SqlTypes.GEOGRAPHY;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new GeometryLiteralFormatter<>( javaType, Wkt.Dialect.SFA_1_2_1, "geography::STGeomFromText" );
	}

	@Override
	protected Geometry<?> toNative(Geometry<?> geometry) {
		return swapAxes( geometry );
	}

	@Override
	protected Geometry<?> fromNative(Geometry<?> geometry) {
		return swapAxes( geometry );
	}

	private static <P extends Position> Geometry<P> swapAxes(Geometry<P> geometry) {
		if ( geometry == null || geometry.isEmpty() ) {
			return geometry;
		}
		final TransformVisitor<P, P> visitor =
				new TransformVisitor<>( new AxisSwap<>( geometry.getCoordinateReferenceSystem() ) );
		geometry.accept( visitor );
		return visitor.getTransformed();
	}

	/**
	 * Swaps the first two axes of each position, within its coordinate reference system; its own reverse.
	 */
	private record AxisSwap<P extends Position>(CoordinateReferenceSystem<P> crs)
			implements TransformOperation<P, P> {

		@Override
		public CoordinateReferenceSystem<P> getSource() {
			return crs;
		}

		@Override
		public CoordinateReferenceSystem<P> getTarget() {
			return crs;
		}

		@Override
		public CoordinateOperation getOperation() {
			return new FirstTwoAxesSwap( crs.getCoordinateDimension() );
		}

		@Override
		public TransformOperation<P, P> reversed() {
			return this;
		}
	}

	/**
	 * Swaps the first two ordinates of a coordinate, leaving any other as it is; its own reverse.
	 */
	private record FirstTwoAxesSwap(int dimension) implements CoordinateOperation {

		@Override
		public boolean isReversible() {
			return true;
		}

		@Override
		public int inCoordinateDimension() {
			return dimension;
		}

		@Override
		public int outCoordinateDimension() {
			return dimension;
		}

		@Override
		public void forward(double[] in, double[] out) {
			final double first = in[0];
			final double second = in[1];
			System.arraycopy( in, 2, out, 2, dimension - 2 );
			out[0] = second;
			out[1] = first;
		}

		@Override
		public void reverse(double[] in, double[] out) {
			forward( in, out );
		}
	}
}
