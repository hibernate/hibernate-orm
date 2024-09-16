/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.implicit;

import java.util.function.BiFunction;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.ResultBuilderEntityValued;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * @author Steve Ebersole
 */
public class ImplicitModelPartResultBuilderEntity
		implements ImplicitModelPartResultBuilder, ResultBuilderEntityValued {

	private final NavigablePath navigablePath;
	private final EntityValuedModelPart modelPart;

	public ImplicitModelPartResultBuilderEntity(
			NavigablePath navigablePath,
			EntityValuedModelPart modelPart) {
		this.navigablePath = navigablePath;
		this.modelPart = modelPart;
	}

	public ImplicitModelPartResultBuilderEntity(EntityMappingType entityMappingType) {
		this( new NavigablePath( entityMappingType.getEntityName() ), entityMappingType );
	}

	@Override
	public Class<?> getJavaType() {
		return modelPart.getJavaType().getJavaTypeClass();
	}

	@Override
	public ResultBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public EntityResult buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationStateImpl = ResultsHelper.impl( domainResultCreationState );
		creationStateImpl.disallowPositionalSelections();

		final TableGroup tableGroup = creationStateImpl.getFromClauseAccess().resolveTableGroup(
				navigablePath,
				np -> {
					if ( navigablePath.getParent() != null ) {
						return creationStateImpl.getFromClauseAccess().getTableGroup( navigablePath.getParent() );
					}

					return modelPart.getEntityMappingType().createRootTableGroup(
							// since this is only used for result set mappings, the canUseInnerJoins value is irrelevant.
							true,
							navigablePath,
							null,
							null,
							null,
							creationStateImpl
					);
				}
		);

		return (EntityResult) modelPart.createDomainResult(
				navigablePath,
				tableGroup,
				null,
				domainResultCreationState
		);
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		ImplicitModelPartResultBuilderEntity that = (ImplicitModelPartResultBuilderEntity) o;

		if ( !navigablePath.equals( that.navigablePath ) ) {
			return false;
		}
		return modelPart.equals( that.modelPart );
	}

	@Override
	public int hashCode() {
		int result = navigablePath.hashCode();
		result = 31 * result + modelPart.hashCode();
		return result;
	}
}
