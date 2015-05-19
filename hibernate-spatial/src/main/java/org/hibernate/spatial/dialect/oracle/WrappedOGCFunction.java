/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.oracle;

import java.util.List;

import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.spatial.Spatial;
import org.hibernate.type.Type;

/**
 * An HQL function that is implemented using Oracle's OGC compliance
 * package.
 */
class WrappedOGCFunction extends StandardSQLFunction {
	private final boolean[] geomArrays;
	private final boolean isGeometryTyped;

	/**
	 * Creates a functions that does not have a {@code Spatial} return type
	 *
	 * @param name function name
	 * @param type return type of the function
	 * @param geomArrays indicates which argument places are occupied by
	 * sdo_geometries
	 */
	WrappedOGCFunction(final String name, final Type type, final boolean[] geomArrays) {
		super( name, type );
		if ( isSpatial( type ) ) {
			throw new IllegalArgumentException(
					"This constructor is only valid for functions returning non-spatial values."
			);
		}
		this.geomArrays = geomArrays;
		this.isGeometryTyped = false;
	}

	/**
	 * @param name function name
	 * @param geomArrays indicates which argument places are occupied by
	 * sdo_geometries
	 */
	WrappedOGCFunction(final String name, final boolean[] geomArrays) {
		super( name );
		this.geomArrays = geomArrays;
		this.isGeometryTyped = true;
	}

	public String render(Type firstArgumentType, final List args, final SessionFactoryImplementor factory) {
		final StringBuilder buf = new StringBuilder();
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
		return ( isGeometryTyped ) ? buf
				.append( ".geom" ).toString() : buf.toString();
	}

	private boolean isSpatial(Type type) {
		return Spatial.class.isAssignableFrom( type.getClass() );
	}

}
