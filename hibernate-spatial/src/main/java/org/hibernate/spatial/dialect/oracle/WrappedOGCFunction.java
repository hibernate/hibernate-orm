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

import java.util.List;

import com.vividsolutions.jts.geom.Geometry;

import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * An HQL function that is implemented using Oracle's OGC compliance
 * package.
 */
class WrappedOGCFunction extends StandardSQLFunction {
	private final boolean[] geomArrays;

	/**
	 * @param name function name
	 * @param type return type of the function
	 * @param geomArrays indicates which argument places are occupied by
	 * sdo_geometries
	 */
	WrappedOGCFunction(final String name, final Type type,
					   final boolean[] geomArrays) {
		super( name, type );
		this.geomArrays = geomArrays;
	}

	public String render(Type firstArgumentType, final List args,
						 final SessionFactoryImplementor factory) {

		StringBuffer buf = new StringBuffer();
		buf.append( "MDSYS." ).append( getName() ).append( "(" );
		for ( int i = 0; i < args.size(); i++ ) {
			if ( i > 0 ) {
				buf.append( "," );
			}
			if ( geomArrays[i] ) {
				buf.append( "MDSYS.ST_GEOMETRY.FROM_SDO_GEOM(" ).append(
						args.get( i )
				).append( ")" );
			}
			else {
				buf.append( args.get( i ) );
			}

		}
		buf.append( ")" );
		return ( getType().getReturnedClass() == Geometry.class ) ? buf
				.append( ".geom" ).toString() : buf.toString();
	}

}