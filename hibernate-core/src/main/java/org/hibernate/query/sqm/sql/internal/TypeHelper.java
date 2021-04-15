/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.ModelPart;

/**
 * @author Steve Ebersole
 */
public class TypeHelper {
	public static JdbcMappingContainer highestPrecedence(JdbcMappingContainer type1, JdbcMappingContainer type2) {
		if ( type1 == null ) {
			return type2;
		}

		if ( type2 == null ) {
			return type1;
		}

		if ( type1 instanceof ModelPart ) {
			return type1;
		}

		if ( type2 instanceof ModelPart ) {
			return type2;
		}

		// todo (6.0) : we probably want a precedence based on generic resolutions such as those based on Serializable

		// todo (6.0) : anything else to consider?

		return type1;
	}
}
