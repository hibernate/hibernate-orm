/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.results.spi;

/**
 * Common interface for initializers of entity, collection and composite state
 *
 * @see InitializerEntity
 * @see InitializerCollection
 * @see InitializerComposite
 *
 * @author Steve Ebersole
 */
public interface Initializer {
	void finishUpRow(RowProcessingState rowProcessingState);
}
