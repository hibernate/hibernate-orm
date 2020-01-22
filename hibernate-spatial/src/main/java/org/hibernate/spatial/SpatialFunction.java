/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial;

/**
 * Spatial functions that users generally expect in a database.
 * <p>
 * <p>The javadoc contains references to these specifications.</p>
 * <ul>
 * <li>OpenGIS Simple Features Specification for SQL, rev. 1.1  (OGC 99-049)</li>
 * </ul>
 *
 * @author Karel Maesen, Geovise BVBA
 * creation-date: Oct 7, 2010
 */
public enum SpatialFunction {

	/**
	 * The dimension function, cfr. OGC 99-049, s2.1.1.1
	 */
	dimension( "SFS 1.1" ),

	/**
	 * The geometryType function, cfr. OGC 99-049, s2.1.1.1
	 */
	geometrytype( "SFS 1.1" ),

	/**
	 * The SRID function, cfr. OGC 99-049, s2.1.1.1
	 */
	srid( "SFS 1.1" ),

	/**
	 * The envelope function, cfr. OGC 99-049, s2.1.1.1
	 */
	envelope( "SFS 1.1" ),

	/**
	 * The asText function, cfr. OGC 99-049, s2.1.1.1
	 */
	astext( "SFS 1.1" ),

	/**
	 * The asBinary function, cfr. OGC 99-049, s2.1.1.1
	 */
	asbinary( "SFS 1.1" ),

	/**
	 * The isEmpty function, cfr. OGC 99-049, s2.1.1.1
	 */
	isempty( "SFS 1.1" ),

	/**
	 * The isSimple function, cfr. OGC 99-049, s2.1.1.1
	 */
	issimple( "SFS 1.1" ),

	/**
	 * The boundery function, cfr. OGC 99-049, s2.1.1.1
	 */
	boundary( "SFS 1.1" ),

	/**
	 * The equals function, cfr. OGC 99-049, s2.1.1.2
	 */
	equals( "SFS 1.1" ),

	/**
	 * The disjoint function, cfr. OGC 99-049, s2.1.1.2
	 */
	disjoint( "SFS 1.1" ),

	/**
	 * The intersects function, cfr. OGC 99-049, s2.1.1.2
	 */
	intersects( "SFS 1.1" ),

	/**
	 * The touches function, cfr. OGC 99-049, s2.1.1.2
	 */
	touches( "SFS 1.1" ),

	/**
	 * The crosses function, cfr. OGC 99-049, s2.1.1.2
	 */
	crosses( "SFS 1.1" ),

	/**
	 * The within function, cfr. OGC 99-049, s2.1.1.2
	 */
	within( "SFS 1.1" ),

	/**
	 * The contains function, cfr. OGC 99-049, s2.1.1.2
	 */
	contains( "SFS 1.1" ),

	/**
	 * The overlaps function, cfr. OGC 99-049, s2.1.1.2
	 */
	overlaps( "SFS 1.1" ),

	/**
	 * The relate function, cfr. OGC 99-049, s2.1.1.2
	 */
	relate( "SFS 1.1" ),

	/**
	 * The distance function, cfr. OGC 99-049, s2.1.1.3
	 */
	distance( "SFS 1.1" ),

	/**
	 * The buffer function, cfr. OGC 99-049, s2.1.1.3
	 */
	buffer( "SFS 1.1" ),

	/**
	 * The convexHull function, cfr. OGC 99-049, s2.1.1.3
	 */
	convexhull( "SFS 1.1" ),

	/**
	 * The intersection function, cfr. OGC 99-049, s2.1.1.3
	 */
	intersection( "SFS 1.1" ),

	/**
	 * The union function, cfr. OGC 99-049, s2.1.1.3
	 */
	geomunion( "SFS 1.1" ),

	/**
	 * The difference function, cfr. OGC 99-049, s2.1.1.3
	 */
	difference( "SFS 1.1" ),

	/**
	 * The symDifference function, cfr. OGC 99-049, s2.1.1.3
	 */
	symdifference( "SFS 1.1" ),

	/**
	 * the distance within function
	 * <p>
	 * <p>The semantics are those of Postgis function ST_Dwithin (geom1, geom2, distance) : boolean. It returns true
	 * if geom1 and geom2 are within the specified distance of one another (in units of the spatial reference system).</p>
	 */
	dwithin( "common" ),

	/**
	 * the transform function
	 * <p>
	 * <p>The semantics are those of the Postgis function ST_Transform(geometry, srid) : geometry. It returns new geometry
	 * with its coordinates transformed to the spatial reference system referenced by the srid parameter.
	 */
	transform( "common" ),

	/**
	 * the extents function
	 */
	extent( "common" ),

	/**
	 * The filter function
	 * <p>
	 * <p>Corresponds to the Oracle Spatial's "SDO_FILTER" function, or the "&&" operator of PostGIS.
	 */
	filter( "filter" ),

	;

	private final String description;


	SpatialFunction(String specification) {
		this.description = specification;
	}

}
