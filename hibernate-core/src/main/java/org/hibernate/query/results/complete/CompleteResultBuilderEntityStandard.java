/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.complete;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.ResultBuilderBasicValued;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * @author Steve Ebersole
 */
public class CompleteResultBuilderEntityStandard implements CompleteResultBuilderEntityValued {
	private final NavigablePath navigablePath;
	private final EntityMappingType entityDescriptor;
	private final LockMode lockMode;
	private final ResultBuilder identifierResultBuilder;
	private final ResultBuilderBasicValued discriminatorResultBuilder;
	private final HashMap<String, FetchBuilder> fetchBuilderMap;

	public CompleteResultBuilderEntityStandard(
			NavigablePath navigablePath,
			EntityMappingType entityDescriptor,
			LockMode lockMode,
			ResultBuilder identifierResultBuilder,
			ResultBuilderBasicValued discriminatorResultBuilder,
			HashMap<String, FetchBuilder> fetchBuilderMap) {
		this.navigablePath = navigablePath;
		this.entityDescriptor = entityDescriptor;
		this.lockMode = lockMode;
		this.identifierResultBuilder = identifierResultBuilder;
		this.discriminatorResultBuilder = discriminatorResultBuilder;
		this.fetchBuilderMap = fetchBuilderMap;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public EntityMappingType getReferencedPart() {
		return entityDescriptor;
	}

	@Override
	public EntityResult buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl impl = ResultsHelper.impl( domainResultCreationState );

		// we just want it added to the registry
		impl.getFromClauseAccess().resolveTableGroup(
				navigablePath,
				np -> entityDescriptor.createRootTableGroup(
						navigablePath,
						null,
						false,
						lockMode,
						impl.getSqlAliasBaseManager(),
						impl.getSqlAstCreationState().getSqlExpressionResolver(),
						() -> predicate -> {},
						impl.getSqlAstCreationState().getCreationContext()
				)
		);

		final DomainResult<?> identifierResult = identifierResultBuilder.buildResult(
				jdbcResultsMetadata,
				resultPosition,
				legacyFetchResolver,
				domainResultCreationState
		);

		final BasicResult<?> discriminatorResult;
		if ( discriminatorResultBuilder != null ) {
			discriminatorResult = discriminatorResultBuilder.buildResult(
					jdbcResultsMetadata,
					resultPosition,
					legacyFetchResolver,
					domainResultCreationState
			);
		}
		else {
			discriminatorResult = null;
		}

		return new EntityResultImpl(
				navigablePath,
				entityDescriptor,
				null,
				lockMode,
				identifierResult,
				discriminatorResult,
				fetchParent -> {
					final List<Fetch> fetches = new ArrayList<>( fetchBuilderMap.size() );

					fetchBuilderMap.forEach(
							(fetchableName, fetchBuilder) -> fetches.add(
									fetchBuilder.buildFetch(
											fetchParent,
											navigablePath.append( fetchableName ),
											jdbcResultsMetadata,
											legacyFetchResolver,
											domainResultCreationState
									)
							)
					);

					return fetches;
				}
		);
	}
}
