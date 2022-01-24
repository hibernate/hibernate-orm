/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractNonJoinedEntityFetch implements EntityFetch {
	private final NavigablePath navigablePath;
	private final EntityValuedFetchable fetchedModelPart;
	private final FetchParent fetchParent;

	public AbstractNonJoinedEntityFetch(
			NavigablePath navigablePath,
			EntityValuedFetchable fetchedModelPart,
			FetchParent fetchParent) {
		this.navigablePath = navigablePath;
		this.fetchedModelPart = fetchedModelPart;
		this.fetchParent = fetchParent;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public EntityValuedFetchable getFetchedMapping() {
		return fetchedModelPart;
	}

	@Override
	public EntityValuedFetchable getEntityValuedModelPart() {
		return fetchedModelPart;
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public List<Fetch> getFetches() {
		return Collections.emptyList();
	}

	@Override
	public Fetch findFetch(Fetchable fetchable) {
		return null;
	}

	@Override
	public EntityMappingType getReferencedMappingType() {
		return fetchedModelPart.getEntityMappingType();
	}
}
