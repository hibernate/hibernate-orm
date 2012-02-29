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

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import java.util.List;

/**
 * HQL Implementation for the geometry ype function.
 */
class GetGeometryTypeFunction extends SDOObjectMethod {

	GetGeometryTypeFunction() {
		super("Get_GType", StandardBasicTypes.STRING);
	}

	public String render(Type firstArgumentType, final List args,
						 final SessionFactoryImplementor factory) {
		StringBuffer buf = new StringBuffer();
		if (args.isEmpty()) {
			throw new IllegalArgumentException(
					"First Argument in arglist must be object to which"
							+ " method is applied");
		}

		buf.append("CASE ").append(args.get(0)).append(".").append(
				getName()).append("()");
		buf.append(" WHEN 1 THEN 'POINT'").append(
				" WHEN 2 THEN 'LINESTRING'").append(
				" WHEN 3 THEN 'POLYGON'").append(
				" WHEN 5 THEN 'MULTIPOINT'").append(
				" WHEN 6 THEN 'MULTILINE'").append(
				" WHEN 7 THEN 'MULTIPOLYGON'").append(" END");
		return buf.toString();
	}
}