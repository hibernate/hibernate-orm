/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
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

package org.hibernate.spatial.dialect.sqlserver.convertors;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.vividsolutions.jts.geom.Coordinate;
import org.geolatte.geom.DimensionalFlag;
import org.geolatte.geom.PointCollection;
import org.geolatte.geom.PointSequence;
import org.geolatte.geom.PointSequenceBuilder;
import org.geolatte.geom.PointSequenceBuilders;
import org.geolatte.geom.crs.CrsId;

import org.hibernate.spatial.jts.mgeom.MCoordinate;

/**
 * A <code>SqlServerGeometry</code> represents the native SQL Server database object.
 * <p/>
 * <p>Instances are created by deserializing the byte array returned in the JDBC result set.
 * They present the structure of the SQL Server Geometry object as specified in <a href="http://download.microsoft.com/download/7/9/3/79326E29-1E2E-45EE-AA73-74043587B17D/%5BMS-SSCLRT%5D.pdf">Microsoft SQL Server CLR Types Serialization Formats</a> .
 *
 * @author Karel Maesen, Geovise BVBA.
 */
public class SqlServerGeometry {

	public static final byte SUPPORTED_VERSION = 1;

	private static final byte hasZValuesMask = 1;
	private static final byte hasMValuesMask = 2;
	private static final byte isValidMask = 4;
	private static final byte isSinglePointMask = 8;
	private static final byte isSingleLineSegment = 16;

	private ByteBuffer buffer;
	private Integer srid;
	private byte version;
	private byte serializationPropertiesByte;
	private int numberOfPoints;
	private double[] points;
	private double[] mValues;
	private double[] zValues;
	private int numberOfFigures;
	private Figure[] figures = null;
	private int numberOfShapes;
	private Shape[] shapes = null;


	private SqlServerGeometry(byte[] bytes) {
		buffer = ByteBuffer.wrap( bytes );
		buffer.order( ByteOrder.LITTLE_ENDIAN );
	}

	SqlServerGeometry() {
	}


	public static byte[] serialize(SqlServerGeometry sqlServerGeom) {
		int capacity = sqlServerGeom.calculateCapacity();
		ByteBuffer buffer = ByteBuffer.allocate( capacity );
		buffer.order( ByteOrder.LITTLE_ENDIAN );
		buffer.putInt( sqlServerGeom.srid );
		buffer.put( SUPPORTED_VERSION );
		buffer.put( sqlServerGeom.serializationPropertiesByte );
		if ( !sqlServerGeom.isSinglePoint() && !sqlServerGeom.isSingleLineSegment() ) {
			buffer.putInt( sqlServerGeom.numberOfPoints );
		}
		for ( int i = 0; i < sqlServerGeom.getNumPoints(); i++ ) {
			buffer.putDouble( sqlServerGeom.points[2 * i] );
			buffer.putDouble( sqlServerGeom.points[2 * i + 1] );
		}
		if ( sqlServerGeom.hasZValues() ) {
			for ( int i = 0; i < sqlServerGeom.zValues.length; i++ ) {
				buffer.putDouble( sqlServerGeom.zValues[i] );
			}
		}
		if ( sqlServerGeom.hasMValues() ) {
			for ( int i = 0; i < sqlServerGeom.mValues.length; i++ ) {
				buffer.putDouble( sqlServerGeom.mValues[i] );
			}
		}
		if ( sqlServerGeom.isSingleLineSegment() || sqlServerGeom.isSinglePoint() ) {
			return buffer.array();
		}

		//in all other cases, we continue to serialize shapes and figures
		buffer.putInt( sqlServerGeom.getNumFigures() );
		for ( int i = 0; i < sqlServerGeom.getNumFigures(); i++ ) {
			sqlServerGeom.getFigure( i ).store( buffer );
		}

		buffer.putInt( sqlServerGeom.getNumShapes() );
		for ( int i = 0; i < sqlServerGeom.getNumShapes(); i++ ) {
			sqlServerGeom.getShape( i ).store( buffer );
		}

		return buffer.array();
	}

	public static SqlServerGeometry deserialize(byte[] bytes) {
		SqlServerGeometry result = new SqlServerGeometry( bytes );
		result.parse();
		return result;
	}

	void copyCoordinate(int index, double[] coords, DimensionalFlag df) {
		coords[0] = points[2 * index];
		coords[1] = points[2 * index + 1];
		if ( hasZValues() ) {
			assert ( df.is3D() );
			coords[df.Z] = zValues[index];
		}
		if ( hasMValues() ) {
			assert ( df.isMeasured() );
			coords[df.M] = mValues[index];
		}
	}

	boolean isParentShapeOf(int parent, int child) {
		return getShape( child ).parentOffset == parent;
	}

	boolean isEmptyShape(int shapeIndex) {
		return getShape( shapeIndex ).figureOffset == -1;
	}

	IndexRange getFiguresForShape(int shapeIndex) {
		int startIdx = getShape( shapeIndex ).figureOffset;
		if ( startIdx == -1 ) {
			return new IndexRange( -1, -1 ); //empty figures
		}
		int endIdx = -1;
		int nextShapeIdx = shapeIndex + 1;
		if ( nextShapeIdx == getNumShapes() ) {
			endIdx = getNumFigures();
		}
		else {
			endIdx = getShape( nextShapeIdx ).figureOffset;
		}
		return new IndexRange( startIdx, endIdx );
	}

	/**
	 * Returns the range of indices in the point array for the specified figure.
	 *
	 * @param figureIndex index to shape in shape array
	 *
	 * @return index range for
	 */
	IndexRange getPointsForFigure(int figureIndex) {
		int start = getFigure( figureIndex ).pointOffset;
		int end = -1;
		int nextFigure = figureIndex + 1;
		if ( nextFigure == getNumFigures() ) {
			end = getNumPoints();
		}
		else {
			end = getFigure( nextFigure ).pointOffset;
		}
		return new IndexRange( start, end );
	}

	boolean isFigureInteriorRing(int figureIdx) {
		return getFigure( figureIdx ).isInteriorRing();
	}

	OpenGisType getOpenGisTypeOfShape(int shpIdx) {
		return getShape( shpIdx ).openGisType;
	}

	PointSequence coordinateRange(IndexRange range) {
		DimensionalFlag df = DimensionalFlag.valueOf( hasZValues(), hasMValues() );
		PointSequenceBuilder psBuilder = PointSequenceBuilders.fixedSized(
				range.end - range.start,
				df,
				CrsId.valueOf( getSrid() )
		);
		double[] coordinates = new double[df.getCoordinateDimension()];
		for ( int idx = range.start, i = 0; idx < range.end; idx++, i++ ) {
			copyCoordinate( idx, coordinates, df );
			psBuilder.add( coordinates );
		}
		return psBuilder.toPointSequence();
	}

	private Coordinate[] createCoordinateArray(int size) {
		if ( hasMValues() ) {
			return new MCoordinate[size];
		}
		else {
			return new Coordinate[size];
		}
	}


	private Figure getFigure(int index) {
		return figures[index];
	}

	private Shape getShape(int index) {
		return shapes[index];
	}

	void setCoordinate(int index, PointCollection coordinate) {
		points[2 * index] = coordinate.getX( index );
		points[2 * index + 1] = coordinate.getY( index );
		if ( hasZValues() ) {
			zValues[index] = coordinate.getZ( index );
		}
		if ( hasMValues() ) {
			mValues[index] = coordinate.getM( index );
		}
	}

	boolean isEmpty() {
		return this.numberOfPoints == 0;
	}

	OpenGisType openGisType() {
		if ( isValid() && isSinglePoint() ) {
			return OpenGisType.POINT;
		}
		if ( isValid() && isSingleLineSegment() ) {
			return OpenGisType.LINESTRING;
		}
		return firstShapeOpenGisType();
	}

	void setHasZValues() {
		serializationPropertiesByte |= hasZValuesMask;
	}

	void allocateZValueArray() {
		if ( this.hasZValues() ) {
			this.zValues = new double[this.numberOfPoints];
		}
	}

	void allocateMValueArray() {
		if ( this.hasMValues() ) {
			this.mValues = new double[this.numberOfPoints];
		}
	}

	void setHasMValues() {
		serializationPropertiesByte |= hasMValuesMask;
	}

	void setIsValid() {
		serializationPropertiesByte |= isValidMask;
	}

	void setIsSinglePoint() {
		setNumberOfPoints( 1 );
		serializationPropertiesByte |= isSinglePointMask;
	}

	void setIsSingleLineSegment() {
		serializationPropertiesByte |= isSingleLineSegment;
	}

	int getNumPoints() {
		return this.numberOfPoints;
	}

	void setNumberOfPoints(int num) {
		this.numberOfPoints = num;
		this.points = new double[2 * this.numberOfPoints];
	}

	private void parse() {
		srid = buffer.getInt();
		version = buffer.get();
		if ( !isCompatible() ) {
			throw new IllegalStateException( "Version mismatch. Expected version " + SUPPORTED_VERSION + ", but received version " + version );
		}
		serializationPropertiesByte = buffer.get();
		determineNumberOfPoints();
		readPoints();
		if ( hasZValues() ) {
			readZValues();
		}
		if ( hasMValues() ) {
			readMValues();
		}

		if ( isSingleLineSegment() ||
				isSinglePoint() ) {
			//generate figure and shape.
			// These are assumed, not explicitly encoded in the
			// serialized data. See specs.
			setNumberOfFigures( 1 );
			setFigure( 0, new Figure( FigureAttribute.Stroke, 0 ) );
			setNumberOfShapes( 1 );
			OpenGisType gisType = isSinglePoint() ? OpenGisType.POINT : OpenGisType.LINESTRING;
			setShape( 0, new Shape( -1, 0, gisType ) );
			return;
		}
		//in all other cases, figures and shapes are
		//explicitly encoded.
		readFigures();
		readShapes();
	}

	private void readShapes() {
		setNumberOfShapes( buffer.getInt() );
		for ( int sIdx = 0; sIdx < numberOfShapes; sIdx++ ) {
			int parentOffset = buffer.getInt();
			int figureOffset = buffer.getInt();
			byte ogtByte = buffer.get();
			OpenGisType type = OpenGisType.valueOf( ogtByte );
			Shape shape = new Shape( parentOffset, figureOffset, type );
			setShape( sIdx, shape );
		}
	}

	private void readFigures() {
		setNumberOfFigures( buffer.getInt() );
		for ( int fIdx = 0; fIdx < numberOfFigures; fIdx++ ) {
			byte faByte = buffer.get();
			int pointOffset = buffer.getInt();
			FigureAttribute fa = FigureAttribute.valueOf( faByte );
			Figure figure = new Figure( fa, pointOffset );
			setFigure( fIdx, figure );
		}
	}

	private OpenGisType firstShapeOpenGisType() {
		if ( shapes == null || shapes.length == 0 ) {
			return OpenGisType.INVALID_TYPE;
		}
		return shapes[0].openGisType;
	}

	private int calculateCapacity() {
		int numPoints = getNumPoints();
		int prefixSize = 6;

		if ( isSinglePoint() ||
				isSingleLineSegment() ) {
			int capacity = prefixSize + 16 * numPoints;
			if ( hasZValues() ) {
				capacity += 8 * numPoints;
			}
			if ( hasMValues() ) {
				capacity += 8 * numPoints;
			}
			return capacity;
		}

		int pointSize = getPointByteSize();
		int size = prefixSize + 3 * 4; // prefix + 3 ints for points, shapes and figures
		size += getNumPoints() * pointSize;
		size += getNumFigures() * Figure.getByteSize();
		size += getNumShapes() * Shape.getByteSize();
		return size;
	}

	int getNumShapes() {
		return this.numberOfShapes;
	}

	private int getPointByteSize() {
		int size = 16; //for X/Y values
		if ( hasMValues() ) {
			size += 8;
		}
		if ( hasZValues() ) {
			size += 8;
		}
		return size;

	}

	private void readPoints() {
		points = new double[2 * numberOfPoints];
		for ( int i = 0; i < numberOfPoints; i++ ) {
			points[2 * i] = buffer.getDouble();
			points[2 * i + 1] = buffer.getDouble();
		}
	}

	private void readZValues() {
		zValues = new double[numberOfPoints];
		for ( int i = 0; i < numberOfPoints; i++ ) {
			zValues[i] = buffer.getDouble();
		}
	}


	private void readMValues() {
		mValues = new double[numberOfPoints];
		for ( int i = 0; i < numberOfPoints; i++ ) {
			mValues[i] = buffer.getDouble();
		}
	}

	private void determineNumberOfPoints() {
		if ( isSinglePoint() ) {
			numberOfPoints = 1;
			return;
		}
		if ( isSingleLineSegment() ) {
			numberOfPoints = 2;
			return;
		}
		numberOfPoints = buffer.getInt();
	}

	boolean isCompatible() {
		return version == SUPPORTED_VERSION;
	}

	void setSrid(Integer srid) {
		this.srid = ( srid == null ) ? -1 : srid;
	}

	Integer getSrid() {
		return srid != -1 ? srid : null;
	}

	boolean hasZValues() {
		return ( serializationPropertiesByte & hasZValuesMask ) != 0;
	}

	boolean hasMValues() {
		return ( serializationPropertiesByte & hasMValuesMask ) != 0;
	}

	boolean isValid() {
		return ( serializationPropertiesByte & isValidMask ) != 0;
	}

	boolean isSinglePoint() {
		return ( serializationPropertiesByte & isSinglePointMask ) != 0;
	}

	boolean isSingleLineSegment() {
		return ( serializationPropertiesByte & isSingleLineSegment ) != 0;
	}

	void setNumberOfFigures(int num) {
		numberOfFigures = num;
		figures = new Figure[numberOfFigures];
	}

	void setFigure(int i, Figure figure) {
		figures[i] = figure;
	}

	void setNumberOfShapes(int num) {
		numberOfShapes = num;
		shapes = new Shape[numberOfShapes];
	}

	void setShape(int i, Shape shape) {
		shapes[i] = shape;
	}

	int getNumFigures() {
		return this.numberOfFigures;
	}


}
