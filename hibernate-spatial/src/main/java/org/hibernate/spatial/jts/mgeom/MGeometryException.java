/**
 * $Id$
 *
 * This file is part of Hibernate Spatial, an extension to the 
 * hibernate ORM solution for geographic data. 
 *
 * Copyright © 2007 Geovise BVBA
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
package org.hibernate.spatial.jts.mgeom;

/**
 * @author Karel Maesen
 */
public class MGeometryException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public final static int OPERATION_REQUIRES_MONOTONE = 1;

	public final static int UNIONM_ON_DISJOINT_MLINESTRINGS = 2;

	public final static int GENERAL_MGEOMETRY_EXCEPTION = 0;

	// type of exception
	private final int type;

	public MGeometryException(String s) {
		super( s );
		type = 0;
	}

	public MGeometryException(int type) {
		super();
		this.type = type;
	}

	public MGeometryException(int type, String msg) {
		super( msg );
		this.type = type;
	}

	public int getType() {
		return type;
	}

}
