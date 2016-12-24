/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.process.spi;

import org.hibernate.sql.convert.results.spi.EntityReference;

/**
 * @author Steve Ebersole
 */
public interface EntityReferenceInitializer extends Initializer, InitializerParent {
	EntityReference getEntityReference();

	Object getEntityInstance();

	void hydrateIdentifier(RowProcessingState rowProcessingState);

	void resolveEntityKey(RowProcessingState rowProcessingState);

	void hydrateEntityState(RowProcessingState rowProcessingState);
}
