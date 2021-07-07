/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.complete;

import java.util.HashMap;
import java.util.function.BiFunction;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.ResultBuilderBasicValued;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.results.graph.DomainResultCreationState;
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
	private final ResultBuilderBasicValued discriminatorResultBuilder;
	private final HashMap<String, FetchBuilder> explicitFetchBuilderMap;

	public CompleteResultBuilderEntityStandard(
			NavigablePath navigablePath,
			EntityMappingType entityDescriptor,
			LockMode lockMode,
			ResultBuilderBasicValued discriminatorResultBuilder,
			HashMap<String, FetchBuilder> explicitFetchBuilderMap) {
		this.navigablePath = navigablePath;
		this.entityDescriptor = entityDescriptor;
		this.lockMode = lockMode;
		this.discriminatorResultBuilder = discriminatorResultBuilder;
		this.explicitFetchBuilderMap = explicitFetchBuilderMap;
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
		impl.disallowPositionalSelections();

		impl.pushExplicitFetchMementoResolver( explicitFetchBuilderMap::get );

		try {
			// we just want it added to the registry
			impl.getFromClauseAccess().resolveTableGroup(
					navigablePath,
					np -> entityDescriptor.createRootTableGroup(
							// since this is only used for result set mappings, the canUseInnerJoins value is irrelevant.
							true,
							navigablePath,
							null,
							() -> predicate -> {},
							impl,
							impl.getCreationContext()
					)
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
					discriminatorResult,
					domainResultCreationState
			);
		}
		finally {
			impl.popExplicitFetchMementoResolver();
		}
	}
}
