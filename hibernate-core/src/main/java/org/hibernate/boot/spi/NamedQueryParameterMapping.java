/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Describes a parameter defined in the mapping of a {@link NamedQueryMapping}
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface NamedQueryParameterMapping {
	/**
	 * Resolve the mapping definition into its run-time memento form
	 */
	ParameterMemento resolve(SessionFactoryImplementor factory);
}
