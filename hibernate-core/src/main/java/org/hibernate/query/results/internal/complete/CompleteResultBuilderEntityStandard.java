/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.complete;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.FetchBuilderBasicValued;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.internal.DomainResultCreationStateImpl;
import org.hibernate.query.results.internal.ResultsHelper;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAliasBaseConstant;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * @author Steve Ebersole
 */
public class CompleteResultBuilderEntityStandard implements CompleteResultBuilderEntityValued, NativeQuery.RootReturn {
	private final String tableAlias;
	private final NavigablePath navigablePath;
	private final EntityMappingType entityDescriptor;
	private final LockMode lockMode;
	private final FetchBuilderBasicValued discriminatorFetchBuilder;
	private final HashMap<Fetchable, FetchBuilder> explicitFetchBuilderMap;

	public CompleteResultBuilderEntityStandard(
			String tableAlias,
			NavigablePath navigablePath,
			EntityMappingType entityDescriptor,
			LockMode lockMode,
			FetchBuilderBasicValued discriminatorFetchBuilder,
			HashMap<Fetchable, FetchBuilder> explicitFetchBuilderMap) {
		this.tableAlias = tableAlias;
		this.navigablePath = navigablePath;
		this.entityDescriptor = entityDescriptor;
		this.lockMode = lockMode;
		this.discriminatorFetchBuilder = discriminatorFetchBuilder;
		this.explicitFetchBuilderMap = explicitFetchBuilderMap;
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
	public String getTableAlias() {
		return tableAlias;
	}

	@Override
	public String getDiscriminatorAlias() {
		return null;
	}

	@Override
	public EntityMappingType getEntityMapping() {
		return entityDescriptor;
	}

	@Override
	public LockMode getLockMode() {
		return lockMode;
	}

	@Override
	public NativeQuery.RootReturn setLockMode(LockMode lockMode) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeQuery.RootReturn addIdColumnAliases(String... aliases) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeQuery.RootReturn setDiscriminatorAlias(String columnAlias) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeQuery.RootReturn addProperty(String propertyName, String columnAlias) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NativeQuery.ReturnProperty addProperty(String propertyName) {
		throw new UnsupportedOperationException();
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
							tableAlias,
							new SqlAliasBaseConstant( tableAlias ),
							null,
							impl
					)
			);

			return new EntityResultImpl(
					navigablePath,
					entityDescriptor,
					tableAlias,
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

		final CompleteResultBuilderEntityStandard that = (CompleteResultBuilderEntityStandard) o;
		return navigablePath.equals( that.navigablePath )
			&& entityDescriptor.equals( that.entityDescriptor )
			&& lockMode == that.lockMode
			&& Objects.equals( discriminatorFetchBuilder, that.discriminatorFetchBuilder )
			&& explicitFetchBuilderMap.equals( that.explicitFetchBuilderMap );
	}
}
