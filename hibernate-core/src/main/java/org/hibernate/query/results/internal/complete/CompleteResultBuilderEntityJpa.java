/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.complete;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.FetchBuilderBasicValued;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.internal.DomainResultCreationStateImpl;
import org.hibernate.query.results.internal.ResultsHelper;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * CompleteResultBuilderEntityValued implementation specific to JPA.  In JPA
 * mappings, fetches contains also entity-identifier-related fetches - so we will
 * need to look for that one here in the fetches and handle it specially.
 *
 * This differs from Hibernate's specific mapping declarations which split identifier related
 * fetches separately
 *
 * @author Steve Ebersole
 */
public class CompleteResultBuilderEntityJpa implements CompleteResultBuilderEntityValued {
	private final NavigablePath navigablePath;
	private final EntityMappingType entityDescriptor;
	private final LockMode lockMode;
	private final FetchBuilderBasicValued discriminatorFetchBuilder;
	private final HashMap<Fetchable, FetchBuilder> explicitFetchBuilderMap;

	public CompleteResultBuilderEntityJpa(
			NavigablePath navigablePath,
			EntityMappingType entityDescriptor,
			LockMode lockMode,
			FetchBuilderBasicValued discriminatorFetchBuilder,
			HashMap<Fetchable, FetchBuilder> explicitFetchBuilderMap) {
		this.navigablePath = navigablePath;
		this.entityDescriptor = entityDescriptor;
		this.lockMode = lockMode;
		this.discriminatorFetchBuilder = discriminatorFetchBuilder;
		this.explicitFetchBuilderMap = explicitFetchBuilderMap;

		if ( entityDescriptor.getDiscriminatorMapping() == null ) {
			// not discriminated
			assert discriminatorFetchBuilder == null;
		}
		else {
			// discriminated
			assert !entityDescriptor.hasSubclasses() || discriminatorFetchBuilder != null;
		}
	}

	@Override
	public Class<?> getJavaType() {
		return entityDescriptor.getJavaType().getJavaTypeClass();
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
	public ResultBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public EntityResult buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState domainResultCreationState) {
		final String implicitAlias = entityDescriptor.getSqlAliasStem() + resultPosition;
		final SqlAliasBase sqlAliasBase =
				domainResultCreationState.getSqlAliasBaseManager().createSqlAliasBase( implicitAlias );

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
							implicitAlias,
							sqlAliasBase,
							null,
							impl.getSqlAstCreationState()
					)
			);

			return new EntityResultImpl(
					navigablePath,
					entityDescriptor,
					implicitAlias,
					lockMode,
					entityResult -> discriminatorFetchBuilder == null
							? null
							: discriminatorFetchBuilder.buildFetch(
									entityResult,
									navigablePath.append( EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME ),
									jdbcResultsMetadata,
									domainResultCreationState
							),
					domainResultCreationState
			);
		}
		finally {
			impl.popExplicitFetchMementoResolver();
		}
	}

	@Override
	public void visitFetchBuilders(BiConsumer<Fetchable, FetchBuilder> consumer) {
		explicitFetchBuilderMap.forEach( consumer );
	}

	@Override
	public int hashCode() {
		int result = navigablePath.hashCode();
		result = 31 * result + entityDescriptor.hashCode();
		result = 31 * result + lockMode.hashCode();
		result = 31 * result + ( discriminatorFetchBuilder != null ? discriminatorFetchBuilder.hashCode() : 0 );
		result = 31 * result + explicitFetchBuilderMap.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final CompleteResultBuilderEntityJpa that = (CompleteResultBuilderEntityJpa) o;
		return navigablePath.equals( that.navigablePath )
			&& entityDescriptor.equals( that.entityDescriptor )
			&& lockMode == that.lockMode
			&& Objects.equals( discriminatorFetchBuilder, that.discriminatorFetchBuilder )
			&& explicitFetchBuilderMap.equals( that.explicitFetchBuilderMap );
	}
}
