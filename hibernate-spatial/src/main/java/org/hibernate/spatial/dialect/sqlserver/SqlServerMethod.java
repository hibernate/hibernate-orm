/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.sqlserver;

import java.util.List;

import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * @author Karel Maesen, Geovise BVBA
 * creation-date: 4/8/13
 */
class SqlServerMethod extends StandardSQLFunction {

	public SqlServerMethod(String name) {
		super( name );
	}

	@Override
	public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor sessionFactory) {
		final StringBuffer buf = new StringBuffer();
		if ( arguments.size() < 1 ) {
			buf.append( getName() ).append( "()" );
		}
		else {
			buf.append( arguments.get( 0 ) ).append( "." )
					.append( getName() ).append( "(" );
			for ( int i = 1; i < arguments.size(); i++ ) {
				buf.append( arguments.get( i ) );
				if ( i < arguments.size() - 1 ) {
					buf.append( "," );
				}
			}
			buf.append( ")" );
		}
		return buf.toString();
	}
}
