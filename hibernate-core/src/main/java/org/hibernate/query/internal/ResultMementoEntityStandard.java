/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.results.FetchBuilderBasicValued;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.named.FetchMementoBasic;
import org.hibernate.query.named.ResultMementoEntity;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.ResultBuilderEntityValued;
import org.hibernate.query.results.internal.complete.CompleteResultBuilderEntityStandard;
import org.hibernate.sql.results.graph.Fetchable;

import static org.hibernate.query.QueryLogging.QUERY_LOGGER;

/**
 * @author Steve Ebersole
 */
public class ResultMementoEntityStandard implements ResultMementoEntity, FetchMemento.Parent {
	private final String tableAlias;
	private final NavigablePath navigablePath;
	private final EntityMappingType entityDescriptor;
	private final LockMode lockMode;
	private final FetchMementoBasic discriminatorMemento;
	private final Map<String, FetchMemento> fetchMementoMap;

	public ResultMementoEntityStandard(
			String tableAlias,
			EntityMappingType entityDescriptor,
			LockMode lockMode,
			FetchMementoBasic discriminatorMemento,
			Map<String, FetchMemento> fetchMementoMap) {
		this.tableAlias = tableAlias;
		this.navigablePath = new NavigablePath( entityDescriptor.getEntityName() );
		this.entityDescriptor = entityDescriptor;
		this.lockMode = lockMode;
		this.discriminatorMemento = discriminatorMemento;
		this.fetchMementoMap = fetchMementoMap;

		QUERY_LOGGER.debugf( "Created ResultMementoEntityStandard - %s", navigablePath );
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public ResultBuilderEntityValued resolve(
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {

		final HashMap<Fetchable, FetchBuilder> fetchBuilderMap = new HashMap<>();
		fetchMementoMap.forEach(
				(attrName, fetchMemento) -> fetchBuilderMap.put(
						(Fetchable) entityDescriptor.findByPath( attrName ),
						fetchMemento.resolve(this, querySpaceConsumer, context )
				)
		);

		return new CompleteResultBuilderEntityStandard(
				tableAlias,
				navigablePath,
				entityDescriptor,
				lockMode,
				discriminatorMemento == null
						? null
						: (FetchBuilderBasicValued)
								discriminatorMemento.resolve( this, querySpaceConsumer, context ),
				fetchBuilderMap
		);
	}
}
