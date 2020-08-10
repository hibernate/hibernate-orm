/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.named.ResultMemento;
import org.hibernate.query.named.ResultMementoBasic;
import org.hibernate.query.named.ResultMementoEntity;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.ResultBuilderBasicValued;
import org.hibernate.query.results.ResultBuilderEntityValued;
import org.hibernate.query.results.complete.CompleteResultBuilderEntityStandard;

/**
 * @author Steve Ebersole
 */
public class ResultMementoEntityStandard implements ResultMementoEntity, FetchMemento.Parent {
	private final NavigablePath navigablePath;
	private final EntityMappingType entityDescriptor;
	private final LockMode lockMode;
	private final ResultMemento identifierMemento;
	private final ResultMementoBasic discriminatorMemento;
	private final Map<String, FetchMemento> fetchMementoMap;

	public ResultMementoEntityStandard(
			EntityMappingType entityDescriptor,
			LockMode lockMode,
			ResultMemento identifierMemento,
			ResultMementoBasic discriminatorMemento,
			Map<String, FetchMemento> fetchMementoMap) {
		this.navigablePath = new NavigablePath( entityDescriptor.getEntityName() );
		this.entityDescriptor = entityDescriptor;
		this.lockMode = lockMode;
		this.identifierMemento = identifierMemento;
		this.discriminatorMemento = discriminatorMemento;
		this.fetchMementoMap = fetchMementoMap;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public EntityMappingType getReferencedModelPart() {
		return entityDescriptor;
	}

	@Override
	public ManagedMappingType getMappingType() {
		return entityDescriptor;
	}

	@Override
	public ResultBuilderEntityValued resolve(
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		final ResultBuilder identifierResultBuilder = identifierMemento.resolve(
				querySpaceConsumer,
				context
		);

		final ResultBuilderBasicValued discriminatorResultBuilder = discriminatorMemento != null
				? discriminatorMemento.resolve( querySpaceConsumer, context )
				: null;

		final HashMap<String, FetchBuilder> fetchBuilderMap = new HashMap<>();

		fetchMementoMap.forEach(
				(attrName, fetchMemento) -> fetchBuilderMap.put(
						attrName,
						fetchMemento.resolve(this, querySpaceConsumer, context )
				)
		);

		return new CompleteResultBuilderEntityStandard(
				navigablePath,
				entityDescriptor,
				lockMode,
				identifierResultBuilder,
				discriminatorResultBuilder,
				fetchBuilderMap
		);
	}
}
