/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.exec.results.internal;

import java.util.List;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.sql.sqm.exec.results.spi.EntityReferenceProcessingState;
import org.hibernate.sql.sqm.exec.results.spi.ResultSetProcessingState;
import org.hibernate.sql.sqm.exec.results.spi.RowProcessingState;
import org.hibernate.sql.sqm.exec.spi.QueryOptions;
import org.hibernate.sql.sqm.convert.spi.Return;

/**
 * @author Steve Ebersole
 */
public class RowProcessingStateStandardImpl implements RowProcessingState {
	private final ResultSetProcessingStateStandardImpl resultSetProcessingState;

	public RowProcessingStateStandardImpl(
			ResultSetProcessingStateStandardImpl resultSetProcessingState,
			List<Return> returns,
			QueryOptions queryOptions) {
		this.resultSetProcessingState = resultSetProcessingState;
	}

	@Override
	public ResultSetProcessingState getResultSetProcessingState() {
		return resultSetProcessingState;
	}

	@Override
	public void registerNonExists(EntityFetch fetch) {
	}

	@Override
	public void registerHydratedEntity(EntityReference entityReference, EntityKey entityKey, Object entityInstance) {
	}

	@Override
	public EntityReferenceProcessingState getProcessingState(EntityReference entityReference) {
		return null;
	}

	@Override
	public EntityReferenceProcessingState getOwnerProcessingState(Fetch fetch) {
		return null;
	}

	@Override
	public void finishRowProcessing() {

	}
}
