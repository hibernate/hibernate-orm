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

package org.hibernate.spatial.dialect.oracle;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Struct;

//TODO -- remove this interface..

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Jul 3, 2010
 */
interface SQLTypeFactory {

	/**
	 * Creates a {@code Struct} representing the specified geometry, using the specified Connection.
	 *
	 * @param geom The {@code SDOGeometry} object
	 * @param conn The Oracle {@code Connection} used to create the {@code Struct}
	 * @return The {@code Struct} representation of the specified SDO Geometry
	 * @throws SQLException If a Struct object cannot be created.
	 */
	public abstract Struct createStruct(SDOGeometry geom, Connection conn) throws SQLException;

	public abstract Array createElemInfoArray(ElemInfo elemInfo, Connection conn) throws SQLException;

	public abstract Array createOrdinatesArray(Ordinates ordinates, Connection conn) throws SQLException;

}
