/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.oracle;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Implements OGC function dimension for HQL.
 */
class GetDimensionFunction extends SDOObjectMethod {

	GetDimensionFunction() {
		super( "Get_Dims", StandardBasicTypes.INTEGER );
	}

	public String render(Type firstArgumentType, final List args, final SessionFactoryImplementor factory) {
		final StringBuffer buf = new StringBuffer();
		if ( args.isEmpty() ) {
			throw new IllegalArgumentException(
					"First Argument in arglist must be object to "
							+ "which method is applied"
			);
		}

		buf.append( args.get( 0 ) ).append( "." ).append(
				getName()
		).append( "()" );
		return buf.toString();
	}
}
