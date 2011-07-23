/*
 * This file is part of Hibernate Spatial, an extension to the
 * hibernate ORM solution for geographic data.
 *
 * Copyright © 2007-2011 Geovise BVBA
 * Copyright © 2007 K.U. Leuven LRD, Spatial Applications Division, Belgium
 *
 * This work was partially supported by the European Commission,
 * under the 6th Framework Programme, contract IST-2-004688-STP.
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
 * For more information, visit: http://www.hibernatespatial.org/
 */

package org.hibernate.spatial;

/**
 * Spatial functions that users generally expect in a database.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Oct 7, 2010
 */
public enum SpatialFunction {

    dimension("SFS 1.1"),
    geometrytype("SFS 1.1"),
    srid("SFS 1.1"),
    envelope("SFS 1.1"),
    astext("SFS 1.1"),
    asbinary("SFS 1.1"),
    isempty("SFS 1.1"),
    issimple("SFS 1.1"),
    boundary("SFS 1.1"),
    equals("SFS 1.1"),
    disjoint("SFS 1.1"),
    intersects("SFS 1.1"),
    touches("SFS 1.1"),
    crosses("SFS 1.1"),
    within("SFS 1.1"),
    contains("SFS 1.1"),
    overlaps("SFS 1.1"),
    relate("SFS 1.1"),
    distance("SFS 1.1"),
    buffer("SFS 1.1"),
    convexhull("SFS 1.1"),
    intersection("SFS 1.1"),
    geomunion("SFS 1.1"), //is actually UNION but this conflicts with SQL UNION construct
    difference("SFS 1.1"),
    symdifference("SFS 1.1"),
    //the distance within function - dwithin(geom, geom, distance) : boolean)
    dwithin("common"),
    //the transform function - transform(geom, epsg-code): geometry
    transform("common");

    private final String description;

    SpatialFunction(String specification) {
        this.description = specification;
    }

}
