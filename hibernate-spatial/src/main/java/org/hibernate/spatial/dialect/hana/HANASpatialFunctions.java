/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.hana;

public enum HANASpatialFunctions {
	alphashape("ST_AlphaShape"),
	
	area("ST_Area"),

	asewkb("ST_AsEWKB"),

	asewkt("ST_AsEWKT"),

	asgeojson("ST_AsGeoJSON"),

	assvg("ST_AsSVG"),

	assvgaggr("ST_AsSVGAggr"),

	aswkb("ST_AsWKB"),

	aswkt("ST_AsWKT"),
	
	centroid("ST_Centroid"),

	convexhullaggr("ST_ConvexHullAggr"),

	coorddim("ST_CoordDim"),

	coveredby("ST_CoveredBy"),

	covers("ST_Covers"),
	
	endpoint("ST_EndPoint"),

	envelopeaggr("ST_EnvelopeAggr"),
	
	exteriorring("ST_ExteriorRing"),
	
	geomfromewkb("ST_GeomFromEWKB"),
	
	geomfromewkt("ST_GeomFromEWKT"),
	
	geomfromtext("ST_GeomFromText"),
	
	geomfromwkb("ST_GeomFromWKB"),
	
	geomfromwkt("ST_GeomFromWKT"),
	
	geometryn("ST_GeometryN"),
	
	interiorringn("ST_InteriorRingN"),
	
	intersectionaggr("ST_IntersectionAggr"),
	
	intersectsrect("ST_IntersectsRect"),
	
	is3d("ST_Is3D"),
	
	isclosed("ST_IsClosed"),
	
	ismeasured("ST_IsMeasured"),
	
	isring("ST_IsRing"),
	
	isvalid("ST_IsValid"),
	
	length("ST_Length"),
	
	m("ST_M"),
	
	mmax("ST_MMax"),
	
	mmin("ST_MMin"),
	
	numgeometries("ST_NumGeometries"),
	
	numinteriorring("ST_NumInteriorRing"),
	
	numinteriorrings("ST_NumInteriorRings"),
	
	numpoints("ST_NumPoints"),
	
	orderingequals("ST_OrderingEquals"),
	
	perimeter("ST_Perimeter"),
	
	pointonsurface("ST_PointOnSurface"),
	
	pointn("ST_PointN"),
	
	snaptogrid("ST_SnapToGrid"),
	
	startpoint("ST_StartPoint"),
	
	unionaggr("ST_UnionAggr"),
	
	x("ST_X"),
	
	xmax("ST_XMax"),
	
	xmin("ST_XMin"),
	
	y("ST_Y"),
	
	ymax("ST_YMax"),
	
	ymin("ST_YMin"),
	
	z("ST_Z"),
	
	zmax("ST_ZMax"),
	
	zmin("ST_ZMin");

	private final String functionName;

	private HANASpatialFunctions(String functionName) {
		this.functionName = functionName;
	}

	public String getFunctionName() {
		return functionName;
	}
}
