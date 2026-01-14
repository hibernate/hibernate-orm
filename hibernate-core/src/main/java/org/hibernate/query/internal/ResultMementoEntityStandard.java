/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.persistence.sql.ResultSetMapping;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.results.spi.FetchBuilderBasicValued;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.named.FetchMementoBasic;
import org.hibernate.query.named.ResultMementoEntity;
import org.hibernate.query.results.spi.FetchBuilder;
import org.hibernate.query.results.spi.ResultBuilderEntityValued;
import org.hibernate.query.results.internal.complete.CompleteResultBuilderEntityStandard;
import org.hibernate.sql.results.graph.Fetchable;

import static org.hibernate.query.QueryLogging.QUERY_LOGGER;

/// ResultMementoEntity implementation from Hibernate's historical result-set mapping support.
///
/// @see org.hibernate.query.NativeQuery#addEntity
/// @see org.hibernate.query.NativeQuery#addRoot
/// @see org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryReturnType
///
/// @author Steve Ebersole
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
	public Class<?> getResultJavaType() {
		return entityDescriptor.getJavaType().getJavaTypeClass();
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

	@Override
	public <R> boolean canBeTreatedAsResultSetMapping(Class<R> resultType, SessionFactory sessionFactory) {
		return false;
	}

	@Override
	public <R> ResultSetMapping<R> toJpaMapping(SessionFactory sessionFactory) {
		throw new UnsupportedOperationException( "Unsupported" );
	}
}
