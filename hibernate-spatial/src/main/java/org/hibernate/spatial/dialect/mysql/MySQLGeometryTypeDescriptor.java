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

package org.hibernate.spatial.dialect.mysql;

import java.sql.Types;

import org.hibernate.spatial.GeometrySqlTypeDescriptor;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 1/17/12
 */
public class MySQLGeometryTypeDescriptor extends GeometrySqlTypeDescriptor {

	public static final MySQLGeometryTypeDescriptor INSTANCE = new MySQLGeometryTypeDescriptor();

	@Override
	public int getSqlType() {
		return Types.ARRAY;
	}

	@Override
	public String getTypeName() {
		return "GEOMETRY";
	}

	@Override
	public boolean canBeRemapped() {
		return false;
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
		return (ValueBinder<X>) new MySQLGeometryValueBinder(javaTypeDescriptor);
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
		return (ValueExtractor<X>) new MySQLGeometryValueExtractor(javaTypeDescriptor);
	}
}
