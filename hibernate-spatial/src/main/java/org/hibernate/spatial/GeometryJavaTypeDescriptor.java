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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 7/27/11
 */
public class GeometryJavaTypeDescriptor extends AbstractTypeDescriptor<Geometry> {


	public static final JavaTypeDescriptor<Geometry> INSTANCE = new GeometryJavaTypeDescriptor(Geometry.class);

	protected GeometryJavaTypeDescriptor(Class<Geometry> type) {
		super(type);
	}

	@Override
	public String toString(Geometry value) {
		return value.toText();
	}

	@Override
	public Geometry fromString(String string) {
		WKTReader reader = new WKTReader();
		try {
			return reader.read(string);
		} catch (ParseException e) {
			throw new RuntimeException(String.format("Can't parse string %s as WKT", string));
		}
	}

	@Override
	public <X> X unwrap(Geometry value, Class<X> type, WrapperOptions options) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public <X> Geometry wrap(X value, WrapperOptions options) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

}
