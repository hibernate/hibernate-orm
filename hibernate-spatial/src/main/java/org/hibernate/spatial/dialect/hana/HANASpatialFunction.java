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
			buf.append( arguments.get( 0 ) ).append( "." ).append( getName() ).append( '(' );
			for ( int i = 1; i < arguments.size(); i++ ) {
				final Object argument = arguments.get( i );
				final boolean parseFromWKB = this.firstArgumentIsGeometryType && i == 1 && "?".equals( argument );
				if ( parseFromWKB ) {
					buf.append( "ST_GeomFromEWKB(" );
				}
				buf.append( argument );
				if ( parseFromWKB ) {
					buf.append( ")" );
				}
				if ( i < arguments.size() - 1 ) {
					buf.append( ", " );
				}
			}
			buf.append( ')' );
			return buf.toString();
		}
	}
}
