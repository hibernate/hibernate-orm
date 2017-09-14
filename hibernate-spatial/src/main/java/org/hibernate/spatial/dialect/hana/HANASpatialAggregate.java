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

public class HANASpatialAggregate extends StandardSQLFunction {

	public HANASpatialAggregate(String name) {
		super( name );
	}

	public HANASpatialAggregate(String name, Type registeredType) {
		super( name, registeredType );
	}

	@Override
	public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor sessionFactory) {
		if ( arguments.size() == 0 ) {
			return getName() + "()";
		}
		else {
			final StringBuilder buf = new StringBuilder();
			buf.append( getName() ).append( '(' );
			for ( int i = 0; i < arguments.size(); i++ ) {
				buf.append( arguments.get( i ) );
				if ( i < arguments.size() - 1 ) {
					buf.append( ", " );
				}
			}
			return buf.append( ')' ).toString();
		}
	}
}
