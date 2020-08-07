/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.ResultBuilder;

/**
 * @author Steve Ebersole
 */
public class EntityResultMappingMemento implements ResultMappingMemento, FetchMappingMemento.Parent {
	private final NavigablePath navigablePath;
	private final EntityMappingType entityDescriptor;
	private final String discriminatorColumnAlias;
	private final Map<String,FetchMappingMemento> fetchMementoMap;

	public EntityResultMappingMemento(
			EntityMappingType entityDescriptor,
			String discriminatorColumnAlias,
			Map<String, FetchMappingMemento> fetchMementoMap) {
		this.navigablePath = new NavigablePath( entityDescriptor.getEntityName() );
		this.entityDescriptor = entityDescriptor;
		this.discriminatorColumnAlias = discriminatorColumnAlias;
		this.fetchMementoMap = fetchMementoMap;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public ManagedMappingType getMappingType() {
		return entityDescriptor;
	}

	@Override
	public ResultBuilder resolve(
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		throw new NotYetImplementedFor6Exception( getClass() );
//		final Map<String, FetchBuilder> fetchBuilderMap = new HashMap<>();
//		fetchMementoMap.forEach(
//				(fetchableName, memento) -> fetchBuilderMap.put(
//						fetchableName,
//						memento.resolve( this, querySpaceConsumer, context )
//				)
//		);
//		return new DynamicResultBuilderEntityStandard( entityDescriptor, null, discriminatorColumnAlias, fetchBuilderMap );
	}
}
