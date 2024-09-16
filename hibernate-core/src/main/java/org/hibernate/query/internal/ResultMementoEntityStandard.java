/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.QueryLogging;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.named.FetchMementoBasic;
import org.hibernate.query.named.ResultMementoEntity;
import org.hibernate.query.results.BasicValuedFetchBuilder;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.ResultBuilderEntityValued;
import org.hibernate.query.results.complete.CompleteResultBuilderEntityStandard;

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

		QueryLogging.QUERY_LOGGER.debugf(
				"Created ResultMementoEntityStandard - %s",
				navigablePath
		);
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public ResultBuilderEntityValued resolve(
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {

		final BasicValuedFetchBuilder discriminatorResultBuilder = discriminatorMemento != null
				? (BasicValuedFetchBuilder) discriminatorMemento.resolve( this, querySpaceConsumer, context )
				: null;

		final HashMap<String, FetchBuilder> fetchBuilderMap = new HashMap<>();

		fetchMementoMap.forEach(
				(attrName, fetchMemento) -> fetchBuilderMap.put(
						attrName,
						fetchMemento.resolve(this, querySpaceConsumer, context )
				)
		);

		return new CompleteResultBuilderEntityStandard(
				tableAlias,
				navigablePath,
				entityDescriptor,
				lockMode,
				discriminatorResultBuilder,
				fetchBuilderMap
		);
	}
}
