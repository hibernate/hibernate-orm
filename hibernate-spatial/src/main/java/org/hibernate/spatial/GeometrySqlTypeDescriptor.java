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

package org.hibernate.spatial;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * A generic <code>SqlTypeDescriptor</code>, intended to be remapped
 * by the spatial dialect.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 7/27/11
 */
public class GeometrySqlTypeDescriptor implements SqlTypeDescriptor {

	public static final GeometrySqlTypeDescriptor INSTANCE = new GeometrySqlTypeDescriptor();

	@Override
	public int getSqlType() {
		return 3000; //this value doesn't conflict with presently defined java.sql.Types values.
	}

	@Override
	public boolean canBeRemapped() {
		return true;
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the fully-qualified database specific type name for the spatial user-defined type.
	 * @return
	 */
	public String getTypeName() {
		return "GEOMETRY";
	}
}
