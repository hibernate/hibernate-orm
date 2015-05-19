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
 * HQL Implementation for the geometry ype function.
 */
class GetGeometryTypeFunction extends SDOObjectMethod {

	GetGeometryTypeFunction() {
		super( "Get_GType", StandardBasicTypes.STRING );
	}

	public String render(Type firstArgumentType, final List args, final SessionFactoryImplementor factory) {
		final StringBuffer buf = new StringBuffer();
		if ( args.isEmpty() ) {
			throw new IllegalArgumentException(
					"First Argument in arglist must be object to which"
							+ " method is applied"
			);
		}

		buf.append( "CASE " ).append( args.get( 0 ) ).append( "." ).append(
				getName()
		).append( "()" );
		buf.append( " WHEN 1 THEN 'POINT'" ).append(
				" WHEN 2 THEN 'LINESTRING'"
		).append(
				" WHEN 3 THEN 'POLYGON'"
		).append(
				" WHEN 5 THEN 'MULTIPOINT'"
		).append(
				" WHEN 6 THEN 'MULTILINE'"
		).append(
				" WHEN 7 THEN 'MULTIPOLYGON'"
		).append( " END" );
		return buf.toString();
	}
}
