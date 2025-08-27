/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.implicit;

import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.ResultBuilderBasicValued;
import org.hibernate.query.results.internal.ResultsHelper;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * @author Steve Ebersole
 */
public class ImplicitModelPartResultBuilderBasic
		implements ImplicitModelPartResultBuilder, ResultBuilderBasicValued {
	private final NavigablePath navigablePath;
	private final BasicValuedModelPart modelPart;

	public ImplicitModelPartResultBuilderBasic(NavigablePath navigablePath, BasicValuedModelPart modelPart) {
		this.navigablePath = navigablePath;
		this.modelPart = modelPart;
	}

	@Override
	public Class<?> getJavaType() {
		return modelPart.getExpressibleJavaType().getJavaTypeClass();
	}

	@Override
	public ResultBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public BasicResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState domainResultCreationState) {
		final TableGroup tableGroup =
				ResultsHelper.impl( domainResultCreationState ).getFromClauseAccess()
						.getTableGroup( navigablePath.getParent() );
		return (BasicResult<?>) modelPart.createDomainResult( navigablePath, tableGroup, null, domainResultCreationState );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final ImplicitModelPartResultBuilderBasic that = (ImplicitModelPartResultBuilderBasic) o;
		return navigablePath.equals( that.navigablePath )
			&& modelPart.equals( that.modelPart );
	}

	@Override
	public int hashCode() {
		int result = navigablePath.hashCode();
		result = 31 * result + modelPart.hashCode();
		return result;
	}
}
