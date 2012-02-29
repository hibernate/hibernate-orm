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

import java.sql.SQLException;
import java.sql.Struct;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Jul 1, 2010
 */
class SDOPoint {
	public double x = 0.0;

	public double y = 0.0;

	public double z = Double.NaN;

	public SDOPoint(Struct struct) {
		try {
			Object[] data = struct.getAttributes();
			this.x = ((Number) data[0]).doubleValue();
			this.y = ((Number) data[1]).doubleValue();
			if (data[2] != null) {
				this.z = ((Number) data[1]).doubleValue();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public String toString() {
		StringBuilder stb = new StringBuilder();
		stb.append("(").append(x).append(",").append(y).append(",").append(
				z).append(")");
		return stb.toString();
	}

}