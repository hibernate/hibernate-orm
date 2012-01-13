/*
 * $Id: EWKTReader.java 166 2010-03-11 22:17:49Z maesenka $
 *
 * This file is an adapted version of the JTS WKTReader. It has
 * been extended by Martin Steinwender to deal with Measured coordinates.
 * 
 * Original copyright notice:
 *
 * The JTS Topology Suite is a collection of Java classes that
 * implement the fundamental operations required to validate a given
 * geo-spatial data set to a known topological specification.
 *
 * Copyright (C) 2001 Vivid Solutions
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, contact:
 *
 *     Vivid Solutions
 *     Suite #1A
 *     2328 Government Street
 *     Victoria BC  V8T 5G5
 *     Canada
 *
 *     (250)385-6040
 *     www.vividsolutions.com
 */


package org.hibernate.spatial.integration;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.util.Assert;

import org.hibernate.spatial.mgeom.MCoordinate;
import org.hibernate.spatial.mgeom.MGeometryFactory;
import org.hibernate.spatial.mgeom.MLineString;

/**
 * Converts a geometry in EWKT to a JTS-Geometry.
 * <p/>
 * <code>EWKTReader</code> supports
 * extracting <code>Geometry</code> objects from either {@link java.io.Reader}s or
 * {@link String}s. This allows it to function as a parser to read <code>Geometry</code>
 * objects from text blocks embedded in other data formats (e.g. XML). <P>
 * <p/>
 * A <code>WKTReader</code> is parameterized by a <code>GeometryFactory</code>,
 * to allow it to create <code>Geometry</code> objects of the appropriate
 * implementation. In particular, the <code>GeometryFactory</code>
 * determines the <code>PrecisionModel</code> and <code>SRID</code> that is
 * used. <P>
 * <p/>
 * The <code>WKTReader</code> converts all input numbers to the precise
 * internal representation.
 * <p/>
 * <h3>Notes:</h3>
 * <ul>
 * <li>The reader supports non-standard "LINEARRING" tags.
 * <li>The reader uses Double.parseDouble to perform the conversion of ASCII
 * numbers to floating point.  This means it supports the Java
 * syntax for floating point literals (including scientific notation).
 * </ul>
 * <p/>
 * <h3>Syntax</h3>
 * The following syntax specification describes the version of Well-Known Text
 * supported by JTS.
 * (The specification uses a syntax language similar to that used in
 * the C and Java language specifications.)
 * <p/>
 * <p/>
 * <blockquote><pre>
 * <i>WKTGeometry:</i> one of<i>
 * <p/>
 *       WKTPoint  WKTLineString  WKTLinearRing  WKTPolygon
 *       WKTMultiPoint  WKTMultiLineString  WKTMultiPolygon
 *       WKTGeometryCollection</i>
 * <p/>
 * <i>WKTPoint:</i> <b>POINT ( </b><i>Coordinate</i> <b>)</b>
 * <p/>
 * <i>WKTLineString:</i> <b>LINESTRING</b> <i>CoordinateSequence</i>
 * <p/>
 * <i>WKTLinearRing:</i> <b>LINEARRING</b> <i>CoordinateSequence</i>
 * <p/>
 * <i>WKTPolygon:</i> <b>POLYGON</b> <i>CoordinateSequenceList</i>
 * <p/>
 * <i>WKTMultiPoint:</i> <b>MULTIPOINT</b> <i>CoordinateSequence</i>
 * <p/>
 * <i>WKTMultiLineString:</i> <b>MULTILINESTRING</b> <i>CoordinateSequenceList</i>
 * <p/>
 * <i>WKTMultiPolygon:</i>
 *         <b>MULTIPOLYGON (</b> <i>CoordinateSequenceList {</i> , <i>CoordinateSequenceList }</i> <b>)</b>
 * <p/>
 * <i>WKTGeometryCollection: </i>
 *         <b>GEOMETRYCOLLECTION (</b> <i>WKTGeometry {</i> , <i>WKTGeometry }</i> <b>)</b>
 * <p/>
 * <i>CoordinateSequenceList:</i>
 *         <b>(</b> <i>CoordinateSequence {</i> <b>,</b> <i>CoordinateSequence }</i> <b>)</b>
 * <p/>
 * <i>CoordinateSequence:</i>
 *         <b>(</b> <i>Coordinate {</i> , <i>Coordinate }</i> <b>)</b>
 * <p/>
 * <i>Coordinate:
 *         Number Number Number<sub>opt</sub></i>
 * <p/>
 * <i>Number:</i> A Java-style floating-point number
 * <p/>
 * </pre></blockquote>
 *
 * @see WKTWriter
 */
public class EWKTReader {
	private static final String EMPTY = "EMPTY";
	private static final String COMMA = ",";
	private static final String L_PAREN = "(";
	private static final String R_PAREN = ")";
	private static final String EQUALS = "=";
	private static final String SEMICOLON = ";";

	private GeometryFactory geometryFactory;
	private PrecisionModel precisionModel;
	private StreamTokenizer tokenizer;

	private int dimension = -1;
	private Boolean hasM = null;

	/**
	 * Creates a reader that creates objects using the default {@link GeometryFactory}.
	 */
	public EWKTReader() {
		this( new MGeometryFactory() );
	}

	/**
	 * Creates a reader that creates objects using the given
	 * {@link GeometryFactory}.
	 *
	 * @param geometryFactory the factory used to create <code>Geometry</code>s.
	 */
	public EWKTReader(GeometryFactory geometryFactory) {
		this.geometryFactory = geometryFactory;
		precisionModel = geometryFactory.getPrecisionModel();
	}

	/**
	 * Reads a Well-Known Text representation of a {@link com.vividsolutions.jts.geom.Geometry}
	 * from a {@link String}.
	 *
	 * @param wellKnownText one or more <Geometry Tagged Text>strings (see the OpenGIS
	 * Simple Features Specification) separated by whitespace
	 *
	 * @return a <code>Geometry</code> specified by <code>wellKnownText</code>
	 *
	 * @throws com.vividsolutions.jts.io.ParseException if a parsing problem occurs
	 */
	public Geometry read(String wellKnownText) throws ParseException {
		StringReader reader = new StringReader( wellKnownText );
		try {
			return read( reader );
		}
		finally {
			reader.close();
		}
	}

	/**
	 * Reads a Well-Known Text representation of a {@link Geometry}
	 * from a {@link java.io.Reader}.
	 *
	 * @param reader a Reader which will return a <Geometry Tagged Text>
	 * string (see the OpenGIS Simple Features Specification)
	 *
	 * @return a <code>Geometry</code> read from <code>reader</code>
	 *
	 * @throws ParseException if a parsing problem occurs
	 */
	public Geometry read(Reader reader) throws ParseException {

		try {

			synchronized ( this ) {
				if ( this.tokenizer != null ) {
					throw new RuntimeException( "EWKT-Reader is already in use." );
				}
				tokenizer = new StreamTokenizer( reader );
			}

			// set tokenizer to NOT parse numbers
			tokenizer.resetSyntax();
			tokenizer.wordChars( 'a', 'z' );
			tokenizer.wordChars( 'A', 'Z' );
			tokenizer.wordChars( 128 + 32, 255 );
			tokenizer.wordChars( '0', '9' );
			tokenizer.wordChars( '-', '-' );
			tokenizer.wordChars( '+', '+' );
			tokenizer.wordChars( '.', '.' );
			tokenizer.whitespaceChars( 0, ' ' );
			tokenizer.commentChar( '#' );

			this.hasM = null;
			this.dimension = -1;

			return readGeometryTaggedText();

		}
		catch ( IOException e ) {
			throw new ParseException( e.toString() );
		}
		finally {
			this.tokenizer = null;
		}
	}

	/**
	 * Returns the next array of <code>Coordinate</code>s in the stream.
	 *
	 * @param tokenizer tokenizer over a stream of text in Well-known Text
	 * format. The next element returned by the stream should be L_PAREN (the
	 * beginning of "(x1 y1, x2 y2, ..., xn yn)") or EMPTY.
	 *
	 * @return the next array of <code>Coordinate</code>s in the
	 *         stream, or an empty array if EMPTY is the next element returned by
	 *         the stream.
	 *
	 * @throws IOException if an I/O error occurs
	 * @throws ParseException if an unexpected token was encountered
	 */
	private MCoordinate[] getCoordinates()
			throws IOException, ParseException {
		String nextToken = getNextEmptyOrOpener();
		if ( nextToken.equals( EMPTY ) ) {
			return new MCoordinate[] { };
		}
		ArrayList coordinates = new ArrayList();
		coordinates.add( getPreciseCoordinate() );
		nextToken = getNextCloserOrComma();
		while ( nextToken.equals( COMMA ) ) {
			coordinates.add( getPreciseCoordinate() );
			nextToken = getNextCloserOrComma();
		}
		return (MCoordinate[]) coordinates.toArray( new MCoordinate[coordinates.size()] );
	}

	/**
	 * gets the next Coordinate and checks dimension
	 *
	 * @return
	 *
	 * @throws IOException
	 * @throws ParseException
	 */
	private Coordinate getPreciseCoordinate()
			throws IOException, ParseException {
		MCoordinate coord = new MCoordinate();
		coord.x = getNextNumber();
		coord.y = getNextNumber();

		Double thirdOrdinateValue = null;
		Double fourthOrdinateValue = null;

		if ( this.dimension == 3 ) {
			thirdOrdinateValue = getNextNumber();
		}
		else if ( this.dimension == 4 ) {
			thirdOrdinateValue = getNextNumber();
			fourthOrdinateValue = getNextNumber();
		}
		else if ( this.dimension < 0 ) {
			if ( isNumberNext() ) {
				thirdOrdinateValue = getNextNumber();
			}
			if ( isNumberNext() ) {
				fourthOrdinateValue = getNextNumber();
			}

			if ( fourthOrdinateValue != null ) {
				this.dimension = 4;
				setHasM( true );
			}
			else if ( thirdOrdinateValue != null ) {
				this.dimension = 3;
				setHasM( Boolean.TRUE.equals( this.hasM ) );
			}
			else {
				this.dimension = 2;
				setHasM( false );
			}
		}

		switch ( this.dimension ) {
			case 2:
				break;
			case 3:
				if ( this.hasM ) {
					coord.m = thirdOrdinateValue;
				}
				else {
					coord.z = thirdOrdinateValue;
				}
				break;
			case 4:
				if ( this.hasM ) {
					coord.z = thirdOrdinateValue;
					coord.m = fourthOrdinateValue;
				}
				else {
					throw new ParseException( "Unsupported geometry dimension." );
				}
				break;
			default:
				throw new ParseException( "Unsupported geometry dimension." );
		}

		precisionModel.makePrecise( coord );
		return coord;
	}


	private boolean isNumberNext() throws IOException {
		int type = tokenizer.nextToken();
		tokenizer.pushBack();
		return type == StreamTokenizer.TT_WORD;
	}

	/**
	 * Parses the next number in the stream.
	 * Numbers with exponents are handled.
	 *
	 * @param tokenizer tokenizer over a stream of text in Well-known Text
	 * format. The next token must be a number.
	 *
	 * @return the next number in the stream
	 *
	 * @throws ParseException if the next token is not a valid number
	 * @throws IOException if an I/O error occurs
	 */
	private double getNextNumber() throws IOException,
			ParseException {
		int type = tokenizer.nextToken();
		switch ( type ) {
			case StreamTokenizer.TT_WORD: {
				try {
					return Double.parseDouble( tokenizer.sval );
				}
				catch ( NumberFormatException ex ) {
					throw new ParseException( "Invalid number: " + tokenizer.sval );
				}
			}
		}
		parseError( "number" );
		return 0.0;
	}

	/**
	 * Returns the next EMPTY or L_PAREN in the stream as uppercase text.
	 *
	 * @param tokenizer tokenizer over a stream of text in Well-known Text
	 * format. The next token must be EMPTY or L_PAREN.
	 *
	 * @return the next EMPTY or L_PAREN in the stream as uppercase
	 *         text.
	 *
	 * @throws ParseException if the next token is not EMPTY or L_PAREN
	 * @throws IOException if an I/O error occurs
	 */
	private String getNextEmptyOrOpener() throws IOException, ParseException {
		String nextWord = getNextWord();
		if ( nextWord.equals( EMPTY ) || nextWord.equals( L_PAREN ) ) {
			return nextWord;
		}
		parseError( EMPTY + " or " + L_PAREN );
		return null;
	}

	/**
	 * Returns the next R_PAREN or COMMA in the stream.
	 *
	 * @param tokenizer tokenizer over a stream of text in Well-known Text
	 * format. The next token must be R_PAREN or COMMA.
	 *
	 * @return the next R_PAREN or COMMA in the stream
	 *
	 * @throws ParseException if the next token is not R_PAREN or COMMA
	 * @throws IOException if an I/O error occurs
	 */
	private String getNextCloserOrComma() throws IOException, ParseException {
		String nextWord = getNextWord();
		if ( nextWord.equals( COMMA ) || nextWord.equals( R_PAREN ) ) {
			return nextWord;
		}
		parseError( COMMA + " or " + R_PAREN );
		return null;
	}

	/**
	 * Returns the next R_PAREN in the stream.
	 *
	 * @param tokenizer tokenizer over a stream of text in Well-known Text
	 * format. The next token must be R_PAREN.
	 *
	 * @return the next R_PAREN in the stream
	 *
	 * @throws ParseException if the next token is not R_PAREN
	 * @throws IOException if an I/O error occurs
	 */
	private String getNextCloser() throws IOException, ParseException {
		String nextWord = getNextWord();
		if ( nextWord.equals( R_PAREN ) ) {
			return nextWord;
		}
		parseError( R_PAREN );
		return null;
	}

	/**
	 * Returns the next R_PAREN in the stream.
	 *
	 * @param tokenizer tokenizer over a stream of text in Well-known Text
	 * format. The next token must be R_PAREN.
	 *
	 * @return the next R_PAREN in the stream
	 *
	 * @throws ParseException if the next token is not R_PAREN
	 * @throws IOException if an I/O error occurs
	 */
	private int getSRID() throws IOException, ParseException {
		if ( !getNextWord().equals( EQUALS ) ) {
			parseError( EQUALS );
			return 0;
		}
		int srid = Integer.parseInt( getNextWord() );
		if ( !getNextWord().equals( SEMICOLON ) ) {
			parseError( SEMICOLON );
			return 0;
		}
		return srid;
	}

	/**
	 * Returns the next word in the stream.
	 *
	 * @param tokenizer tokenizer over a stream of text in Well-known Text
	 * format. The next token must be a word.
	 *
	 * @return the next word in the stream as uppercase text
	 *
	 * @throws ParseException if the next token is not a word
	 * @throws IOException if an I/O error occurs
	 */
	private String getNextWord() throws IOException, ParseException {
		int type = tokenizer.nextToken();
		switch ( type ) {
			case StreamTokenizer.TT_WORD:

				String word = tokenizer.sval;
				if ( word.equalsIgnoreCase( EMPTY ) ) {
					return EMPTY;
				}
				return word;

			case '(':
				return L_PAREN;
			case ')':
				return R_PAREN;
			case ',':
				return COMMA;
			case '=':
				return EQUALS;
			case ';':
				return SEMICOLON;
		}
		parseError( "word" );
		return null;
	}

	/**
	 * Throws a formatted ParseException for the current token.
	 *
	 * @param expected a description of what was expected
	 *
	 * @throws ParseException
	 * @throws com.vividsolutions.jts.util.AssertionFailedException if an invalid token is encountered
	 */
	private void parseError(String expected)
			throws ParseException {
		// throws Asserts for tokens that should never be seen
		if ( tokenizer.ttype == StreamTokenizer.TT_NUMBER ) {
			Assert.shouldNeverReachHere( "Unexpected NUMBER token" );
		}
		if ( tokenizer.ttype == StreamTokenizer.TT_EOL ) {
			Assert.shouldNeverReachHere( "Unexpected EOL token" );
		}

		String tokenStr = tokenString();
		throw new ParseException( "Expected " + expected + " but found " + tokenStr );
	}

	/**
	 * Gets a description of the current token
	 *
	 * @return a description of the current token
	 */
	private String tokenString() {
		switch ( tokenizer.ttype ) {
			case StreamTokenizer.TT_NUMBER:
				return "<NUMBER>";
			case StreamTokenizer.TT_EOL:
				return "End-of-Line";
			case StreamTokenizer.TT_EOF:
				return "End-of-Stream";
			case StreamTokenizer.TT_WORD:
				return "'" + tokenizer.sval + "'";
		}
		return "'" + (char) tokenizer.ttype + "'";
	}

	/**
	 * Creates a <code>Geometry</code> using the next token in the stream.
	 *
	 * @param tokenizer tokenizer over a stream of text in Well-known Text
	 * format. The next tokens must form a &lt;Geometry Tagged Text&gt;.
	 *
	 * @return a <code>Geometry</code> specified by the next token
	 *         in the stream
	 *
	 * @throws ParseException if the coordinates used to create a <code>Polygon</code>
	 * shell and holes do not form closed linestrings, or if an unexpected
	 * token was encountered
	 * @throws IOException if an I/O error occurs
	 */
	private Geometry readGeometryTaggedText() throws IOException, ParseException {

		String type = null;
		Geometry geom;

		int srid = geometryFactory.getSRID();

		try {
			String firstWord = getNextWord();
			if ( "SRID".equals( firstWord ) ) {
				srid = getSRID();
				type = getNextWord();
			}
			else {
				type = firstWord;
			}
		}
		catch ( IOException e ) {
			return null;
		}
		catch ( ParseException e ) {
			return null;
		}

		if ( type.equals( "POINT" ) ) {
			geom = readPointText();
		}
		else if ( type.equals( "POINTM" ) ) {
			setHasM( true );
			geom = readPointText();
		}
		else if ( type.equalsIgnoreCase( "LINESTRING" ) ) {
			geom = readLineStringText();
		}
		else if ( type.equalsIgnoreCase( "LINESTRINGM" ) ) {
			setHasM( true );
			geom = readLineStringText();
		}
		else if ( type.equalsIgnoreCase( "LINEARRING" ) ) {
			geom = readLinearRingText();
		}
		else if ( type.equalsIgnoreCase( "LINEARRINGM" ) ) {
			setHasM( true );
			geom = readLinearRingText();
		}
		else if ( type.equalsIgnoreCase( "POLYGON" ) ) {
			geom = readPolygonText();
		}
		else if ( type.equalsIgnoreCase( "POLYGONM" ) ) {
			//setHasM(true);
			//geom = readPolygonText();
			throw new RuntimeException( "PolygonM is not supported." );
		}
		else if ( type.equalsIgnoreCase( "MULTIPOINT" ) ) {
			geom = readMultiPointText();
		}
		else if ( type.equalsIgnoreCase( "MULTIPOINTM" ) ) {
			setHasM( true );
			geom = readMultiPointText();
		}
		else if ( type.equalsIgnoreCase( "MULTILINESTRING" ) ) {
			geom = readMultiLineStringText();
		}
		else if ( type.equalsIgnoreCase( "MULTILINESTRINGM" ) ) {
			setHasM( true );
			geom = readMultiLineStringText();
		}
		else if ( type.equalsIgnoreCase( "MULTIPOLYGON" ) ) {
			geom = readMultiPolygonText();
		}
		else if ( type.equalsIgnoreCase( "MULTIPOLYGONM" ) ) {
			//setHasM(true);
			//geom = readMultiPolygonText();
			throw new RuntimeException( "MultiPolygonM is not supported." );
		}
		else if ( type.equalsIgnoreCase( "GEOMETRYCOLLECTION" ) ) {
			geom = readGeometryCollectionText();
		}
		else if ( type.equalsIgnoreCase( "GEOMETRYCOLLECTIONM" ) ) {
			setHasM( true );
			geom = readGeometryCollectionText();
		}
		else {
			throw new ParseException( "Unknown geometry type: " + type );
		}
		geom.setSRID( srid );

		return geom;
	}

	/**
	 * m-values sicherstellen
	 *
	 * @throws ParseException
	 */
	private void setHasM(boolean hasM) throws ParseException {
		if ( this.hasM == null ) {
			this.hasM = hasM;
		}
		else if ( this.hasM != hasM ) {
			throw new ParseException( "Inkonsistent use of m-values." );
		}
	}

	/**
	 * Creates a <code>Point</code> using the next token in the stream.
	 *
	 * @param tokenizer tokenizer over a stream of text in Well-known Text
	 * format. The next tokens must form a &lt;Point Text&gt;.
	 *
	 * @return a <code>Point</code> specified by the next token in
	 *         the stream
	 *
	 * @throws IOException if an I/O error occurs
	 * @throws ParseException if an unexpected token was encountered
	 */
	private Point readPointText() throws IOException, ParseException {

		String nextToken = getNextEmptyOrOpener();
		if ( nextToken.equals( EMPTY ) ) {
			return geometryFactory.createPoint( (Coordinate) null );
		}
		Point point = geometryFactory.createPoint( getPreciseCoordinate() );
		getNextCloser();
		return point;
	}

	/**
	 * Creates a <code>LineString</code> using the next token in the stream.
	 *
	 * @param tokenizer tokenizer over a stream of text in Well-known Text
	 * format. The next tokens must form a &lt;LineString Text&gt;.
	 *
	 * @return a <code>LineString</code> specified by the next
	 *         token in the stream
	 *
	 * @throws IOException if an I/O error occurs
	 * @throws ParseException if an unexpected token was encountered
	 */
	private LineString readLineStringText() throws IOException, ParseException {

		MCoordinate[] coords = getCoordinates();
		if ( this.hasM != null && this.hasM ) {
			return ( (MGeometryFactory) geometryFactory ).createMLineString( coords );
		}
		else {
			return geometryFactory.createLineString( coords );
		}

	}

	/**
	 * Creates a <code>LinearRing</code> using the next token in the stream.
	 *
	 * @param tokenizer tokenizer over a stream of text in Well-known Text
	 * format. The next tokens must form a &lt;LineString Text&gt;.
	 *
	 * @return a <code>LinearRing</code> specified by the next
	 *         token in the stream
	 *
	 * @throws IOException if an I/O error occurs
	 * @throws ParseException if the coordinates used to create the <code>LinearRing</code>
	 * do not form a closed linestring, or if an unexpected token was
	 * encountered
	 */
	private LinearRing readLinearRingText()
			throws IOException, ParseException {
		MCoordinate[] coords = getCoordinates();
		if ( this.hasM ) {
			throw new RuntimeException( "LinearRingM not supported." );
		}
		else {
			return geometryFactory.createLinearRing( coords );
		}
	}

	/**
	 * Creates a <code>MultiPoint</code> using the next token in the stream.
	 *
	 * @param tokenizer tokenizer over a stream of text in Well-known Text
	 * format. The next tokens must form a &lt;MultiPoint Text&gt;.
	 *
	 * @return a <code>MultiPoint</code> specified by the next
	 *         token in the stream
	 *
	 * @throws IOException if an I/O error occurs
	 * @throws ParseException if an unexpected token was encountered
	 */
	private MultiPoint readMultiPointText() throws IOException, ParseException {
		MCoordinate[] coords = getCoordinates();
		Point[] pts = toPoints( coords );
		return geometryFactory.createMultiPoint( pts );
	}

	/**
	 * Creates an array of <code>Point</code>s having the given <code>Coordinate</code>
	 * s.
	 *
	 * @param coordinates the <code>Coordinate</code>s with which to create the
	 * <code>Point</code>s
	 *
	 * @return <code>Point</code>s created using this <code>WKTReader</code>
	 *         s <code>GeometryFactory</code>
	 */
	private Point[] toPoints(Coordinate[] coordinates) {
		ArrayList points = new ArrayList();
		for ( int i = 0; i < coordinates.length; i++ ) {
			points.add( geometryFactory.createPoint( coordinates[i] ) );
		}
		return (Point[]) points.toArray( new Point[] { } );
	}

	/**
	 * Creates a <code>Polygon</code> using the next token in the stream.
	 *
	 * @param hasM
	 * @param tokenizer tokenizer over a stream of text in Well-known Text
	 * format. The next tokens must form a &lt;Polygon Text&gt;.
	 *
	 * @return a <code>Polygon</code> specified by the next token
	 *         in the stream
	 *
	 * @throws ParseException if the coordinates used to create the <code>Polygon</code>
	 * shell and holes do not form closed linestrings, or if an unexpected
	 * token was encountered.
	 * @throws IOException if an I/O error occurs
	 */
	private Polygon readPolygonText() throws IOException, ParseException {

		// PolygonM is not supported
		setHasM( false );

		String nextToken = getNextEmptyOrOpener();
		if ( nextToken.equals( EMPTY ) ) {
			return geometryFactory.createPolygon(
					geometryFactory.createLinearRing(
							new Coordinate[] { }
					), new LinearRing[] { }
			);
		}
		ArrayList holes = new ArrayList();
		LinearRing shell = readLinearRingText();
		nextToken = getNextCloserOrComma();
		while ( nextToken.equals( COMMA ) ) {
			LinearRing hole = readLinearRingText();
			holes.add( hole );
			nextToken = getNextCloserOrComma();
		}
		LinearRing[] array = new LinearRing[holes.size()];
		return geometryFactory.createPolygon( shell, (LinearRing[]) holes.toArray( array ) );
	}

	/**
	 * Creates a <code>MultiLineString</code> using the next token in the stream.
	 *
	 * @param hasM
	 * @param tokenizer tokenizer over a stream of text in Well-known Text
	 * format. The next tokens must form a &lt;MultiLineString Text&gt;.
	 *
	 * @return a <code>MultiLineString</code> specified by the
	 *         next token in the stream
	 *
	 * @throws IOException if an I/O error occurs
	 * @throws ParseException if an unexpected token was encountered
	 */
	private com.vividsolutions.jts.geom.MultiLineString readMultiLineStringText() throws IOException, ParseException {

		ArrayList lineStrings = new ArrayList();

		String nextToken = getNextEmptyOrOpener();
		if ( nextToken.equals( EMPTY ) ) {
			// No Coordinates for LineString
		}
		else {
			LineString lineString = readLineStringText();
			lineStrings.add( lineString );
			nextToken = getNextCloserOrComma();
			while ( nextToken.equals( COMMA ) ) {
				lineString = readLineStringText();
				lineStrings.add( lineString );
				nextToken = getNextCloserOrComma();
			}
		}

		if ( this.hasM != null && this.hasM == true ) {
			MLineString[] mlines = (MLineString[]) lineStrings.toArray( new MLineString[lineStrings.size()] );
			return ( (MGeometryFactory) geometryFactory ).createMultiMLineString( mlines );
		}
		else {
			setHasM( false );
			LineString[] lines = (LineString[]) lineStrings.toArray( new LineString[lineStrings.size()] );
			return geometryFactory.createMultiLineString( lines );
		}
	}

	/**
	 * Creates a <code>MultiPolygon</code> using the next token in the stream.
	 *
	 * @param hasM
	 * @param tokenizer tokenizer over a stream of text in Well-known Text
	 * format. The next tokens must form a &lt;MultiPolygon Text&gt;.
	 *
	 * @return a <code>MultiPolygon</code> specified by the next
	 *         token in the stream, or if if the coordinates used to create the
	 *         <code>Polygon</code> shells and holes do not form closed linestrings.
	 *
	 * @throws IOException if an I/O error occurs
	 * @throws ParseException if an unexpected token was encountered
	 */
	private MultiPolygon readMultiPolygonText()
			throws IOException, ParseException {

		// MultiPolygonM is not supported
		setHasM( false );

		String nextToken = getNextEmptyOrOpener();
		if ( nextToken.equals( EMPTY ) ) {
			return geometryFactory.createMultiPolygon( new Polygon[] { } );
		}
		ArrayList polygons = new ArrayList();
		Polygon polygon = readPolygonText();
		polygons.add( polygon );
		nextToken = getNextCloserOrComma();
		while ( nextToken.equals( COMMA ) ) {
			polygon = readPolygonText();
			polygons.add( polygon );
			nextToken = getNextCloserOrComma();
		}
		Polygon[] array = new Polygon[polygons.size()];
		return geometryFactory.createMultiPolygon( (Polygon[]) polygons.toArray( array ) );
	}

	/**
	 * Creates a <code>GeometryCollection</code> using the next token in the
	 * stream.
	 *
	 * @param tokenizer tokenizer over a stream of text in Well-known Text
	 * format. The next tokens must form a &lt;GeometryCollection Text&gt;.
	 *
	 * @return a <code>GeometryCollection</code> specified by the
	 *         next token in the stream
	 *
	 * @throws ParseException if the coordinates used to create a <code>Polygon</code>
	 * shell and holes do not form closed linestrings, or if an unexpected
	 * token was encountered
	 * @throws IOException if an I/O error occurs
	 */
	private GeometryCollection readGeometryCollectionText()
			throws IOException, ParseException {

		String nextToken = getNextEmptyOrOpener();
		if ( nextToken.equals( EMPTY ) ) {
			return geometryFactory.createGeometryCollection( new Geometry[] { } );
		}
		ArrayList geometries = new ArrayList();
		Geometry geometry = readGeometryTaggedText();
		geometries.add( geometry );
		nextToken = getNextCloserOrComma();
		while ( nextToken.equals( COMMA ) ) {
			geometry = readGeometryTaggedText();
			geometries.add( geometry );
			nextToken = getNextCloserOrComma();
		}
		Geometry[] array = new Geometry[geometries.size()];
		return geometryFactory.createGeometryCollection( (Geometry[]) geometries.toArray( array ) );
	}

}