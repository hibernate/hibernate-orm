package org.hibernate.spatial;

/**
 * Spatial functions that users generally expect in a database.
 * <p>
 * The javadoc contains references to these specifications.
 * <ul>
 * <li>OpenGIS Simple Features Specification for SQL, rev. 1.1  (OGC 99-049)</li>
 * </ul>
 *
 * @author Karel Maesen, Geovise BVBA
 *
 * creation-date: Oct 7, 2010
 * @deprecated To be replaced by {@link CommonSpatialFunction}
 */
@Deprecated
public enum SpatialFunction {

	/**
	 * The dimension function, cfr. OGC 99-049, s2.1.1.1
	 */
	dimension( ),

	/**
	 * The geometryType function, cfr. OGC 99-049, s2.1.1.1
	 */
	geometrytype( ),

	/**
	 * The SRID function, cfr. OGC 99-049, s2.1.1.1
	 */
	srid( ),

	/**
	 * The envelope function, cfr. OGC 99-049, s2.1.1.1
	 */
	envelope( ),

	/**
	 * The asText function, cfr. OGC 99-049, s2.1.1.1
	 */
	astext( ),

	/**
	 * The asBinary function, cfr. OGC 99-049, s2.1.1.1
	 */
	asbinary( ),

	/**
	 * The isEmpty function, cfr. OGC 99-049, s2.1.1.1
	 */
	isempty( ),

	/**
	 * The isSimple function, cfr. OGC 99-049, s2.1.1.1
	 */
	issimple( ),

	/**
	 * The boundery function, cfr. OGC 99-049, s2.1.1.1
	 */
	boundary( ),

	/**
	 * The equals function, cfr. OGC 99-049, s2.1.1.2
	 */
	equals( ),

	/**
	 * The disjoint function, cfr. OGC 99-049, s2.1.1.2
	 */
	disjoint( ),

	/**
	 * The intersects function, cfr. OGC 99-049, s2.1.1.2
	 */
	intersects( ),

	/**
	 * The touches function, cfr. OGC 99-049, s2.1.1.2
	 */
	touches( ),

	/**
	 * The crosses function, cfr. OGC 99-049, s2.1.1.2
	 */
	crosses( ),

	/**
	 * The within function, cfr. OGC 99-049, s2.1.1.2
	 */
	within( ),

	/**
	 * The contains function, cfr. OGC 99-049, s2.1.1.2
	 */
	contains( ),

	/**
	 * The overlaps function, cfr. OGC 99-049, s2.1.1.2
	 */
	overlaps( ),

	/**
	 * The relate function, cfr. OGC 99-049, s2.1.1.2
	 */
	relate( ),

	/**
	 * The distance function, cfr. OGC 99-049, s2.1.1.3
	 */
	distance( ),

	/**
	 * The buffer function, cfr. OGC 99-049, s2.1.1.3
	 */
	buffer( ),

	/**
	 * The convexHull function, cfr. OGC 99-049, s2.1.1.3
	 */
	convexhull( ),

	/**
	 * The intersection function, cfr. OGC 99-049, s2.1.1.3
	 */
	intersection( ),

	/**
	 * The union function, cfr. OGC 99-049, s2.1.1.3
	 */
	geomunion( ),

	/**
	 * The difference function, cfr. OGC 99-049, s2.1.1.3
	 */
	difference( ),

	/**
	 * The symDifference function, cfr. OGC 99-049, s2.1.1.3
	 */
	symdifference( ),

	/**
	 * the distance within function
	 * <p>
	 * The semantics are those of Postgis function ST_Dwithin (geom1, geom2, distance) : boolean. It returns true
	 * if geom1 and geom2 are within the specified distance of one another (in units of the spatial reference system).
	 */
	dwithin( ),

	/**
	 * the transform function
	 * <p>
	 * The semantics are those of the Postgis function ST_Transform(geometry, srid) : geometry. It returns new geometry
	 * with its coordinates transformed to the spatial reference system referenced by the srid parameter.
	 */
	transform( ),

	/**
	 * the extents function
	 */
	extent( ),

	/**
	 * The filter function
	 * <p>
	 * Corresponds to the Oracle Spatial's "SDO_FILTER" function, or the "&amp;&amp;" operator of PostGIS.
	 */
	filter( ),

	;


	SpatialFunction() {
	}

}
