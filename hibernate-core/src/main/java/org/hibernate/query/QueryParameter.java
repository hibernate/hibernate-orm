/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import org.hibernate.Incubating;
import org.hibernate.type.Type;

/**
 * NOTE: Consider this contract (and its sub-contracts) as incubating as we transition to 6.0 and SQM
 *
 * @author Steve Ebersole
 */
@Incubating
public interface QueryParameter<T> extends javax.persistence.Parameter<T> {
	/**
	 * Get the Hibernate Type associated with this parameter.
	 *
	 * @return The Hibernate Type.
	 */
	Type getHibernateType();

	int[] getSourceLocations();

	// todo : add a method indicating whether this parameter is valid for use in "parameter list binding"
	//		actually this already implemented in 6.0 code and I'm not going to mess with
	//		this in earlier versions
}
