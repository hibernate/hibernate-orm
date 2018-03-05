/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.hana;

import java.util.List;

import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

public class HANASpatialFunction extends StandardSQLFunction {

	private static final String AS_EWKB_SUFFIX = ".ST_AsEWKB()";

	private final boolean firstArgumentIsGeometryType;

	public HANASpatialFunction(String name, boolean firstArgumentIsGeometryType) {
		super( name );
		this.firstArgumentIsGeometryType = firstArgumentIsGeometryType;
	}

	public HANASpatialFunction(String name, Type registeredType, boolean firstArgumentIsGeometryType) {
		super( name, registeredType );
		this.firstArgumentIsGeometryType = firstArgumentIsGeometryType;
	}

	@Override
	public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor sessionFactory) {
		if ( arguments.size() == 0 ) {
			return getName() + "()";
		}
		else {
			final StringBuilder buf = new StringBuilder();
			// If the first argument is an expression, e.g. a nested function, strip the .ST_AsEWKB() suffix
			buf.append( stripEWKBSuffix( arguments.get( 0 ) ) );

			// Add function call
			buf.append( "." ).append( getName() ).append( '(' );

			// Add function arguments
			for ( int i = 1; i < arguments.size(); i++ ) {
				final Object argument = arguments.get( i );
				// Check if first argument needs to be parsed from EWKB. This is the case if the first argument is a
				// parameter that is set as EWKB or if it's a nested function call.
				final boolean parseFromWKB = ( this.firstArgumentIsGeometryType && i == 1 && "?".equals( argument ) );
				if ( parseFromWKB ) {
					buf.append( "ST_GeomFromEWKB(" );
				}
				buf.append( stripEWKBSuffix( argument ) );
				if ( parseFromWKB ) {
					buf.append( ")" );
				}
				if ( i < arguments.size() - 1 ) {
					buf.append( ", " );
				}
			}
			buf.append( ')' );
			// If it doesn't specify an explicit type, assume it's a geometry
			if ( this.getType() == null ) {
				buf.append( AS_EWKB_SUFFIX );
			}
			return buf.toString();
		}
	}

	private Object stripEWKBSuffix(Object argument) {
		if ( ( argument instanceof String ) && ( (String) argument ).endsWith( AS_EWKB_SUFFIX ) ) {
			String argumentString = (String) argument;
			return argumentString.substring( 0, argumentString.length() - AS_EWKB_SUFFIX.length() );
		}

		return argument;
	}
}
