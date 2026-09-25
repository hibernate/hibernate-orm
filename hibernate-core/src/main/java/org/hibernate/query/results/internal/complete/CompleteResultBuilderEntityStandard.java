package org.hibernate.query.results.internal.complete;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.results.spi.FetchBuilder;
import org.hibernate.query.results.spi.FetchBuilderBasicValued;
import org.hibernate.query.results.spi.ResultBuilder;
import org.hibernate.query.results.internal.ResultsHelper;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseConstant;
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
	@Nonnull
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public EntityMappingType getReferencedPart() {
		return entityDescriptor;
	}

	@Override
	@Nonnull
	public String getTableAlias() {
		return tableAlias;
	}

	@Override
	@Nullable
	public String getDiscriminatorAlias() {
		return null;
	}

	@Override
	@Nonnull
	public EntityMappingType getEntityMapping() {
		return entityDescriptor;
	}

	@Override
	@Nullable
	public LockMode getLockMode() {
		return lockMode;
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
	public ResultBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public EntityResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState domainResultCreationState) {
		final var impl = ResultsHelper.impl( domainResultCreationState );
		impl.disallowPositionalSelections();
		impl.pushExplicitFetchMementoResolver( explicitFetchBuilderMap::get );
		try {
			// we just want it added to the registry
			impl.getFromClauseAccess().resolveTableGroup(
					navigablePath,
					path -> entityDescriptor.createRootTableGroup(
							// since this is only used for result set mappings, the canUseInnerJoins value is irrelevant.
							true,
							navigablePath,
							tableAlias,
							new SqlAliasBaseConstant( tableAlias ),
							null,
							impl
					)
			);

			return new EntityResultImpl<>(
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
		else if ( !( o instanceof CompleteResultBuilderEntityStandard that ) ) {
			return false;
		}
		else {
			return navigablePath.equals( that.navigablePath )
				&& entityDescriptor.equals( that.entityDescriptor )
				&& lockMode == that.lockMode
				&& Objects.equals( discriminatorFetchBuilder, that.discriminatorFetchBuilder )
				&& explicitFetchBuilderMap.equals( that.explicitFetchBuilderMap );
		}
	}
}
