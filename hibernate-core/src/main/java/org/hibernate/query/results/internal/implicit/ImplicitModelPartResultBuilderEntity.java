/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.implicit;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.query.results.spi.ResultBuilder;
import org.hibernate.query.results.spi.ResultBuilderEntityValued;
import org.hibernate.query.results.internal.DomainResultCreationStateImpl;
import org.hibernate.query.results.internal.ResultsHelper;
import org.hibernate.spi.NavigablePath;
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
	public EntityResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState domainResultCreationState) {
		final var creationStateImpl = ResultsHelper.impl( domainResultCreationState );
		creationStateImpl.disallowPositionalSelections();
		return (EntityResult<?>) modelPart.createDomainResult(
				navigablePath,
				tableGroup( creationStateImpl ),
				null,
				domainResultCreationState
		);
	}

	private TableGroup tableGroup(DomainResultCreationStateImpl creationStateImpl) {
		return creationStateImpl.getFromClauseAccess().resolveTableGroup(
				navigablePath,
				path -> {
					final var parentPath = navigablePath.getParent();
					return parentPath != null
							? creationStateImpl.getFromClauseAccess().getTableGroup( parentPath )
							: modelPart.getEntityMappingType().createRootTableGroup(
									// since this is only used for result set mappings,
									// the canUseInnerJoins value is irrelevant.
									true,
									navigablePath,
									null,
									null,
									null,
									creationStateImpl
							);
				}
		);
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !( object instanceof ImplicitModelPartResultBuilderEntity that ) ) {
			return false;
		}
		else {
			return navigablePath.equals( that.navigablePath )
				&& modelPart.equals( that.modelPart );
		}
	}

	@Override
	public int hashCode() {
		int result = navigablePath.hashCode();
		result = 31 * result + modelPart.hashCode();
		return result;
	}
}
