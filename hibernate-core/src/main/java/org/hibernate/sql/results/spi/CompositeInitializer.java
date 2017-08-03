/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

/**
 * Initializer implementation for initializing composite (embedded) references.
 *
 * @author Steve Ebersole
 */
public interface CompositeInitializer extends Initializer {
	Object getComponentInstance();

	void resolveInstance(RowProcessingState rowProcessingState);
}
