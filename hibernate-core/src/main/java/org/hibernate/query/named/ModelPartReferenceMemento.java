/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.named;

import org.hibernate.spi.NavigablePath;

/**
 * A ResultMappingMementoNode that is a reference to some part of the user's
 * domain model
 *
 * @author Steve Ebersole
 */
public interface ModelPartReferenceMemento extends ResultMappingMementoNode {
	/**
	 * Path to the memento, relative to the result roots
	 */
	NavigablePath getNavigablePath();

}
