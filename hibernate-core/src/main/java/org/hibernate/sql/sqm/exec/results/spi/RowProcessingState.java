/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.exec.results.spi;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.Fetch;

/**
 * State pertaining to the processing of a single row of a ResultSet
 *
 * @author Steve Ebersole
 */
public interface RowProcessingState {
	ResultSetProcessingState getResultSetProcessingState();

	void registerNonExists(EntityFetch fetch);
	void registerHydratedEntity(EntityReference entityReference, EntityKey entityKey, Object entityInstance);

	EntityReferenceProcessingState getProcessingState(EntityReference entityReference);
	EntityReferenceProcessingState getOwnerProcessingState(Fetch fetch);

	void finishRowProcessing();
}
