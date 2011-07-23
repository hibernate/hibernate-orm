/*
 * $Id: SpatialAnalysis.java 200 2010-03-31 19:52:12Z maesenka $
 *
 * This file is part of Hibernate Spatial, an extension to the
 * hibernate ORM solution for geographic data.
 *
 * Copyright © 2007-2010 Geovise BVBA
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
 * The spatial analysis functions defined in the OGC SFS specification.
 *
 * @author Karel Maesen
 */
public interface SpatialAnalysis {

    public static int DISTANCE = 1;

    public static int BUFFER = 2;

    public static int CONVEXHULL = 3;

    public static int INTERSECTION = 4;

    public static int UNION = 5;

    public static int DIFFERENCE = 6;

    public static int SYMDIFFERENCE = 7;

}
