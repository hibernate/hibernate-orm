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
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.named.ResultMementoBasic;
import org.hibernate.query.named.ResultMementoEntity;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.ResultBuilderBasicValued;
import org.hibernate.query.results.ResultBuilderEntityValued;
import org.hibernate.query.results.complete.CompleteResultBuilderEntityJpa;
import org.hibernate.query.results.implicit.ImplicitModelPartResultBuilderBasic;

/**
 * @author Steve Ebersole
 */
public class ResultMementoEntityJpa implements ResultMementoEntity, FetchMemento.Parent {
	private final NavigablePath navigablePath;
	private final EntityMappingType entityDescriptor;
	private final LockMode lockMode;
//	private final ResultMemento identifierMemento;
	private final ResultMementoBasic discriminatorMemento;
	private final Map<String, FetchMemento> explicitFetchMementoMap;

	public ResultMementoEntityJpa(
			EntityMappingType entityDescriptor,
			LockMode lockMode,
			ResultMementoBasic discriminatorMemento,
			Map<String, FetchMemento> explicitFetchMementoMap) {
		this.navigablePath = new NavigablePath( entityDescriptor.getEntityName() );
		this.entityDescriptor = entityDescriptor;
		this.lockMode = lockMode;
		this.discriminatorMemento = discriminatorMemento;
		this.explicitFetchMementoMap = explicitFetchMementoMap;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public ResultBuilderEntityValued resolve(
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		final EntityDiscriminatorMapping discriminatorMapping = entityDescriptor.getDiscriminatorMapping();
		final ResultBuilderBasicValued discriminatorResultBuilder;
		if ( discriminatorMapping == null ) {
			assert discriminatorMemento == null;
			discriminatorResultBuilder = null;
		}
		else {
			if ( discriminatorMemento != null ) {
				discriminatorResultBuilder = discriminatorMemento.resolve( querySpaceConsumer, context );
			}
			else {
				discriminatorResultBuilder = new ImplicitModelPartResultBuilderBasic( navigablePath, discriminatorMapping );
			}
		}

		final HashMap<String, FetchBuilder> explicitFetchBuilderMap = new HashMap<>();

		explicitFetchMementoMap.forEach(
				(relativePath, fetchMemento) -> explicitFetchBuilderMap.put(
						relativePath,
						fetchMemento.resolve(this, querySpaceConsumer, context )
				)
		);

		return new CompleteResultBuilderEntityJpa(
				navigablePath,
				entityDescriptor,
				lockMode,
				discriminatorResultBuilder,
				explicitFetchBuilderMap
		);
	}
}
