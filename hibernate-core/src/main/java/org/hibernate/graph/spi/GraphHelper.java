/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graph.spi;

import org.hibernate.graph.Graph;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

/**
 * Helper containing utilities useful for graph handling
 *
 * @author Steve Ebersole
 */
public class GraphHelper {

	public static boolean appliesTo(Graph<?> graph, ManagedDomainType<?> managedType) {
		final ManagedDomainType<?> graphedType = graph.getGraphedType();
		ManagedDomainType<?> superType = managedType;
		while ( superType != null ) {
			if ( graphedType.equals( superType ) ) {
				return true;
			}
			superType = superType.getSuperType();
		}
		return false;
	}

}
