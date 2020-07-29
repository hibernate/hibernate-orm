/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.results.ResultBuilder;

/**
 * Models the "boot view" of a ResultSet mapping used in the mapping
 * of native and procedure queries.
 *
 * Ultimately used to generate a NamedResultSetMappingMemento that is
 * stored in the {@link org.hibernate.query.named.NamedQueryRepository}
 * for availability at runtime
 *
 * @author Steve Ebersole
 */
public interface NamedResultSetMappingDefinition {
	/**
	 * The name under which the result-set-mapping is to be registered
	 */
	String getRegistrationName();

	/**
	 * Contract for the individual result mappings
	 */
	interface ResultMapping {
		ResultBuilder resolve(SessionFactoryImplementor factory);
	}

	/**
	 * Create the named runtime memento instance
	 */
	NamedResultSetMappingMemento resolve(SessionFactoryImplementor factory);
}
