/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal;
import javax.persistence.criteria.Selection;

/**
 * Contract for query components capable of eirther being a parameter or containing parameters.
 *
 * @author Steve Ebersole
 */
public interface ParameterContainer {
	/**
	 * Register any parameters contained within this query component with the given registry.
	 *
	 * @param registry The parameter registry with which to register.
	 */
	public void registerParameters(ParameterRegistry registry);

	/**
	 * Helper to deal with potential parameter container nodes.
	 */
	public static class Helper {
		public static void possibleParameter(Selection selection, ParameterRegistry registry) {
			if ( ParameterContainer.class.isInstance( selection ) ) {
				( (ParameterContainer) selection ).registerParameters( registry );
			}
		}
	}
}
