/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.dynamic;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.results.internal.ResultsHelper;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseConstant;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * An entity-valued DynamicResultBuilder for cases when the user has not supplied
 * specific column to attribute mappings. Hibernate uses the column names mapped
 * by the entity mapping itself to read the entity values.
 *
 * @author Steve Ebersole
 */
public class DynamicResultBuilderEntityCalculated implements DynamicResultBuilderEntity, NativeQuery.RootReturn {
	private final NavigablePath navigablePath;
	private final EntityMappingType entityMapping;

	private final String tableAlias;
	private final LockMode explicitLockMode;

	public DynamicResultBuilderEntityCalculated(
			EntityMappingType entityMapping,
			String tableAlias,
			LockMode explicitLockMode) {
		this.entityMapping = entityMapping;
		this.navigablePath = new NavigablePath( entityMapping.getEntityName() );
		this.tableAlias = tableAlias;
		this.explicitLockMode = explicitLockMode;
	}

	@Override
	public Class<?> getJavaType() {
		return entityMapping.getJavaType().getJavaTypeClass();
	}

	@Override
	@Nonnull
	public EntityMappingType getEntityMapping() {
		return entityMapping;
	}

	@Override
	@Nonnull
	public String getTableAlias() {
		return tableAlias;
	}

	@Override
	@Nonnull
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	@Nullable
	public LockMode getLockMode() {
		return explicitLockMode;
	}

	@Override
	@Nonnull
	public NativeQuery.RootReturn setLockMode(@Nullable LockMode lockMode) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Nonnull
	public NativeQuery.RootReturn addIdColumnAliases(@Nonnull String... aliases) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Nullable
	public String getDiscriminatorAlias() {
		return null;
	}

	@Override
	@Nonnull
	public NativeQuery.RootReturn setDiscriminatorAlias(@Nullable String columnAlias) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Nonnull
	public NativeQuery.RootReturn addProperty(@Nonnull String propertyName, @Nonnull String columnAlias) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Nonnull
	public NativeQuery.ReturnProperty addProperty(@Nonnull String propertyName) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Nonnull
	public DynamicResultBuilderEntityCalculated cacheKeyInstance() {
		return this;
	}

	@Override
	@Nonnull
	public EntityResult<?> buildResult(
			@Nonnull JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			@Nonnull DomainResultCreationState domainResultCreationState) {
		final var creationStateImpl = ResultsHelper.impl( domainResultCreationState );

		final var tableGroup = entityMapping.createRootTableGroup(
				true,
				navigablePath,
				tableAlias,
				new SqlAliasBaseConstant( tableAlias ),
				null,
				creationStateImpl
		);

		creationStateImpl.getFromClauseAccess().registerTableGroup( navigablePath, tableGroup );
		if ( explicitLockMode != null ) {
			domainResultCreationState.getSqlAstCreationState().registerLockMode( tableAlias, explicitLockMode );
		}

		return (EntityResult<?>) entityMapping.createDomainResult(
				navigablePath,
				tableGroup,
				tableAlias,
				domainResultCreationState
		);
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !( object instanceof DynamicResultBuilderEntityCalculated that ) ) {
			return false;
		}
		else {
			return navigablePath.equals( that.navigablePath )
				&& entityMapping.equals( that.entityMapping )
				&& tableAlias.equals( that.tableAlias )
				&& explicitLockMode == that.explicitLockMode;
		}
	}

	@Override
	public int hashCode() {
		int result = navigablePath.hashCode();
		result = 31 * result + entityMapping.hashCode();
		result = 31 * result + tableAlias.hashCode();
		result = 31 * result + ( explicitLockMode != null ? explicitLockMode.hashCode() : 0 );
		return result;
	}
}
