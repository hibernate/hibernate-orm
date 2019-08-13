/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.NamedResultSetMappingMemento;

/**
 * @author Steve Ebersole
 */
public interface NamedResultSetMappingDefinition {
	/**
	 * The name under which the result-set-mapping is to be registered
	 */
	String getRegistrationName();

	/**
	 * Create the named runtime memento instance
	 */
	NamedResultSetMappingMemento resolve(SessionFactoryImplementor factory);
}
